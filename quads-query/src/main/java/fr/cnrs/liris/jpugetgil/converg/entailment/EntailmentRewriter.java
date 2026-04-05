package fr.cnrs.liris.jpugetgil.converg.entailment;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Quad;
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
     *       OpQuadPattern(graph, [q1]),            -- explicit q1
     *       ruleExpansion(q1)                      -- inferred q1
     *     )
     *   )
     * </pre>
     */
    private Op rewriteQuadPattern(OpQuadPattern opQuadPattern) {
        Node graphNode = opQuadPattern.getGraphNode();
        List<Quad> quads = opQuadPattern.getPattern().getList();

        // Default graph (metadata) is not subject to entailment
        if (graphNode.isURI() && graphNode == Quad.defaultGraphNodeGenerated) {
            return opQuadPattern;
        }

        List<Quad> nonEntailedQuads = new ArrayList<>();
        List<Op> entailedOps = new ArrayList<>();

        for (Quad quad : quads) {
            List<Op> expansions = expandQuad(quad, graphNode);
            if (expansions.isEmpty()) {
                nonEntailedQuads.add(quad);
            } else {
                // Union the explicit with all rule expansions
                Op combined = createSingleQuadOp(graphNode, quad);
                for (Op expansion : expansions) {
                    combined = OpUnion.create(combined, expansion);
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
