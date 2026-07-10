package fr.cnrs.liris.jpugetgil.converg.entailment;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewrites a Jena Op algebra tree to include RDFS/OWL entailment expansions.
 * <p>
 * For each {@link OpQuadPattern} that triggers an entailment rule, the rewriter:
 * <ol>
 *   <li>Extracts the triggering quad from the BGP</li>
 *   <li>Applies each matching rule to produce an inferred Op subtree</li>
 *   <li>Combines via {@code OpUnion(original, inferred)} so both explicit and
 *       inferred results are returned</li>
 *   <li>Rejoins with remaining BGP quads via {@code OpJoin}</li>
 * </ol>
 * Every branch of the union is projected onto the triggering quad's visible
 * variables: rule expansions bind extra {@code __ent_*} variables that the
 * explicit branch does not, and the SQL UNION requires both branches to expose
 * the same columns.
 * <p>
 * Rules are applied in a single pass (no fixpoint iteration) to prevent infinite expansion.
 */
public class EntailmentRewriter {

    private static final Logger log = LoggerFactory.getLogger(EntailmentRewriter.class);

    private final List<EntailmentRule> rules;
    private final FreshVariableAllocator allocator;

    public EntailmentRewriter(List<EntailmentRule> rules) {
        this.rules = rules;
        this.allocator = new FreshVariableAllocator();
    }

    /**
     * Rewrite the Op tree to include entailment expansions.
     *
     * @param op the original Op tree (in quad form)
     * @return the rewritten Op tree with entailment expansions
     */
    public Op rewrite(Op op) {
        return rewriteOp(op);
    }

    private Op rewriteOp(Op op) {
        return switch (op) {
            case OpQuadPattern opQuadPattern -> rewriteQuadPattern(opQuadPattern);
            case OpJoin opJoin -> OpJoin.create(
                    rewriteOp(opJoin.getLeft()),
                    rewriteOp(opJoin.getRight())
            );
            case OpUnion opUnion -> OpUnion.create(
                    rewriteOp(opUnion.getLeft()),
                    rewriteOp(opUnion.getRight())
            );
            case OpLeftJoin opLeftJoin -> OpLeftJoin.create(
                    rewriteOp(opLeftJoin.getLeft()),
                    rewriteOp(opLeftJoin.getRight()),
                    opLeftJoin.getExprs()
            );
            case OpFilter opFilter -> OpFilter.filterDirect(
                    opFilter.getExprs(),
                    rewriteOp(opFilter.getSubOp())
            );
            case OpProject opProject -> new OpProject(
                    rewriteOp(opProject.getSubOp()),
                    opProject.getVars()
            );
            case OpDistinct opDistinct -> new OpDistinct(rewriteOp(opDistinct.getSubOp()));
            case OpOrder opOrder -> new OpOrder(
                    rewriteOp(opOrder.getSubOp()),
                    opOrder.getConditions()
            );
            case OpSlice opSlice -> new OpSlice(
                    rewriteOp(opSlice.getSubOp()),
                    opSlice.getStart(),
                    opSlice.getLength()
            );
            case OpGroup opGroup -> new OpGroup(
                    rewriteOp(opGroup.getSubOp()),
                    opGroup.getGroupVars(),
                    opGroup.getAggregators()
            );
            case OpExtend opExtend -> OpExtend.create(
                    rewriteOp(opExtend.getSubOp()),
                    opExtend.getVarExprList()
            );
            case OpMinus opMinus -> OpMinus.create(
                    rewriteOp(opMinus.getLeft()),
                    rewriteOp(opMinus.getRight())
            );
            // Pass through ops that don't contain triple patterns
            default -> op;
        };
    }

    /**
     * Rewrite an OpQuadPattern by applying entailment rules to individual quads.
     * <p>
     * For a BGP with quads [q0, q1, q2] where q1 triggers entailment:
     * <pre>
     *   OpJoin(
     *     OpQuadPattern(graph, [q0, q2]),         -- remaining quads
     *     OpUnion(
     *       OpProject(OpQuadPattern(graph, [q1]), vars(q1)),  -- explicit q1
     *       OpProject(ruleExpansion(q1), vars(q1))            -- inferred q1
     *     )
     *   )
     * </pre>
     */
    private Op rewriteQuadPattern(OpQuadPattern opQuadPattern) {
        Node graphNode = opQuadPattern.getGraphNode();
        List<Quad> quads = opQuadPattern.getPattern().getList();

        // Default graph (metadata) is not subject to entailment,
        // nor is the schema-drift virtual graph
        if (graphNode.isURI()
                && (graphNode == Quad.defaultGraphNodeGenerated
                || SchemaDriftDetector.isSchemaDriftGraph(graphNode))) {
            return opQuadPattern;
        }

        List<Quad> nonEntailedQuads = new ArrayList<>();
        List<Op> entailedOps = new ArrayList<>();

        for (Quad quad : quads) {
            List<Op> expansions = expandQuad(quad, graphNode);
            if (expansions.isEmpty()) {
                nonEntailedQuads.add(quad);
            } else {
                // Union the explicit with all rule expansions, each branch projected
                // onto the quad's visible variables so the SQL UNION columns align
                // (expansions bind extra __ent_* variables the explicit branch lacks)
                List<Var> branchVars = visibleVars(graphNode, quad);
                Op combined = projectBranch(createSingleQuadOp(graphNode, quad), branchVars);
                for (Op expansion : expansions) {
                    combined = OpUnion.create(combined, projectBranch(expansion, branchVars));
                }
                entailedOps.add(combined);
            }
        }

        // If no rules triggered, return the original
        if (entailedOps.isEmpty()) {
            return opQuadPattern;
        }

        log.info("Entailment rewriter expanded {} quad(s) in BGP", entailedOps.size());

        // Build result: join non-entailed BGP with entailed expansions
        Op result = null;

        if (!nonEntailedQuads.isEmpty()) {
            result = createMultiQuadOp(graphNode, nonEntailedQuads);
        }

        for (Op entailedOp : entailedOps) {
            result = (result == null) ? entailedOp : OpJoin.create(result, entailedOp);
        }

        return result;
    }

    /**
     * Apply all matching rules to a single quad, returning the list of expansions.
     */
    private List<Op> expandQuad(Quad quad, Node graphNode) {
        List<Op> expansions = new ArrayList<>();
        for (EntailmentRule rule : rules) {
            if (rule.appliesTo(quad)) {
                log.debug("Rule {} applies to quad {}", rule.name(), quad);
                Op expansion = rule.expand(quad, graphNode, allocator);
                expansions.add(expansion);
            }
        }
        return expansions;
    }

    /**
     * The variables of the triggering quad visible outside the entailment union:
     * the graph (if variable) and the quad's own variables, in a fixed order shared
     * by every branch.
     */
    private static List<Var> visibleVars(Node graphNode, Quad quad) {
        List<Var> vars = new ArrayList<>();
        for (Node node : List.of(graphNode, quad.getSubject(), quad.getPredicate(), quad.getObject())) {
            if (node.isVariable() && !vars.contains(Var.alloc(node))) {
                vars.add(Var.alloc(node));
            }
        }
        return vars;
    }

    /**
     * Project a union branch onto the shared visible variables. A quad with no
     * variables at all has nothing to align, so the branch is left as-is.
     */
    private static Op projectBranch(Op branch, List<Var> branchVars) {
        return branchVars.isEmpty() ? branch : new OpProject(branch, branchVars);
    }

    /**
     * Create an OpQuadPattern for a single quad.
     */
    private static Op createSingleQuadOp(Node graphNode, Quad quad) {
        BasicPattern bp = new BasicPattern();
        bp.add(quad.asTriple());
        return new OpQuadPattern(graphNode, bp);
    }

    /**
     * Create an OpQuadPattern for multiple quads sharing the same graph.
     */
    private static Op createMultiQuadOp(Node graphNode, List<Quad> quads) {
        BasicPattern bp = new BasicPattern();
        for (Quad quad : quads) {
            bp.add(quad.asTriple());
        }
        return new OpQuadPattern(graphNode, bp);
    }
}
