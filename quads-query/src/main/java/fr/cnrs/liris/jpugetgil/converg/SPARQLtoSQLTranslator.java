package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.connection.JdbcConnection;
import fr.cnrs.liris.jpugetgil.converg.entailment.*;
import fr.cnrs.liris.jpugetgil.converg.path.OpZeroLengthPath;
import fr.cnrs.liris.jpugetgil.converg.path.PathRewriter;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.operator.*;
import fr.cnrs.liris.jpugetgil.converg.utils.PgUtils;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Summary;
import io.prometheus.metrics.model.snapshots.Unit;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.ARQNotImplemented;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorBase;
import org.apache.jena.sparql.algebra.OpWalker;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.postgresql.jdbc.PgResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to translate a SPARQL query into a SQL query
 */
public class SPARQLtoSQLTranslator extends SPARQLLanguageTranslator {

    private static final Logger log = LoggerFactory.getLogger(SPARQLtoSQLTranslator.class);

    private final JdbcConnection jdbcConnection;

    private final Summary queryTranslationDuration = MetricsSingleton.getInstance().queryTranslationDuration;

    private final Summary queryExecutionDuration = MetricsSingleton.getInstance().queryExecutionDuration;

    private final Counter queryCounter = MetricsSingleton.getInstance().queryCounter;

    /**
     * Constructor of the SPARQLtoSQLTranslator
     *
     * @param condensedMode    if true, the SQL query build will use the condensed mode
     * @param entailmentRegime the entailment regime to apply during query translation
     */
    public SPARQLtoSQLTranslator(boolean condensedMode, EntailmentRegime entailmentRegime) {
        super(condensedMode, entailmentRegime);
        this.jdbcConnection = JdbcConnection.getInstance();
    }

    /**
     * Translate the SPARQL query into a SQL query
     *
     * @return the SQL query
     */
    public ResultSet translateAndExecSelect(Query query) {
        SQLQuery finalizedQuery = getSqlQuery(query);

        try {
            BindingIterator bindingIterator = new BindingIterator(getSQLResultSet(query, finalizedQuery));
            Set<Var> vars = bindingIterator
                    .getVars();

            return ResultSetStream.create(new ArrayList<>(vars), bindingIterator);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Dataset translateAndExecConstruct(Query query) {
        SQLQuery finalizedQuery = getSqlQuery(query);

        try {
            java.sql.ResultSet rs = getSQLResultSet(query, finalizedQuery);
            DatasetGraph dsg = DatasetFactory.createGeneral().asDatasetGraph();

            List<Quad> quadList = query.getConstructTemplate().getQuads();

            while (rs.next()) {
                instantiateTemplate(quadList, rs).forEach(dsg::add);
            }

            return DatasetFactory.wrap(dsg);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public SQLQuery buildSPARQLContext(Op op) {
        return buildSPARQLContext(op, new SQLContext(new HashMap<>(), condensedMode, entailmentRegime, null, null));
    }

    private SQLQuery buildSPARQLContext(Op op, SQLContext context) {
        return switch (op) {
            case OpTransitiveClosure opTC -> new TransitiveClosureSQLOperator(opTC, context)
                    .buildSQLQuery();
            case OpJoin opJoin -> new JoinSQLOperator(
                    buildSPARQLContext(opJoin.getLeft(), context),
                    buildSPARQLContext(opJoin.getRight(), context)
            ).buildSQLQuery();
            case OpUnion opUnion -> new UnionSQLOperator(
                    buildSPARQLContext(opUnion.getLeft(), context),
                    buildSPARQLContext(opUnion.getRight(), context)
            ).buildSQLQuery();
            case OpGroup opGroup -> new GroupSQLOperator(
                    opGroup,
                    buildSPARQLContext(opGroup.getSubOp(), context)
            ).buildSQLQuery();
            case OpMinus opMinus -> new MinusSQLOperator(
                    buildSPARQLContext(opMinus.getLeft(), context),
                    buildSPARQLContext(opMinus.getRight(), context)
            ).buildSQLQuery();
            case OpProject opProject -> new ProjectSQLOperator(
                    opProject,
                    buildSPARQLContext(opProject.getSubOp(), context)
            ).buildSQLQuery();
            case OpExtend opExtend -> new ExtendSQLOperator(
                    opExtend,
                    buildSPARQLContext(opExtend.getSubOp(), context)
            ).buildSQLQuery();
            case OpDistinct opDistinct -> new DistinctSQLOperator(
                    opDistinct,
                    buildSPARQLContext(opDistinct.getSubOp(), context)
            ).buildSQLQuery();
            case OpQuadPattern opQuadPattern ->
                    SchemaDriftDetector.isSchemaDriftGraph(opQuadPattern.getGraphNode())
                            ? new SchemaDriftSQLOperator(opQuadPattern, context).buildSQLQuery()
                            : new QuadPatternSQLOperator(opQuadPattern, context).buildSQLQuery();
            case OpOrder opOrder -> new OrderSQLOperator(
                    opOrder,
                    buildSPARQLContext(opOrder.getSubOp(), context)
            ).buildSQLQuery();
            case OpSlice opSlice -> new SliceSQLOperator(
                    opSlice,
                    buildSPARQLContext(opSlice.getSubOp(), context)
            ).buildSQLQuery();
            case OpLeftJoin opLeftJoin -> new LeftJoinSQLOperator(
                    opLeftJoin,
                    buildSPARQLContext(opLeftJoin.getLeft(), context),
                    buildSPARQLContext(opLeftJoin.getRight(), context)
            ).buildSQLQuery();
            case OpFilter opFilter -> new FilterSQLOperator(
                    opFilter,
                    buildSPARQLContext(opFilter.getSubOp())
            ).buildSQLQuery();
            case OpTable ignored -> new SQLQuery(
                    null,
                    context
            );
            case OpNull ignored -> new SQLQuery(
                    null,
                    context
            );
            case OpDatasetNames opDatasetNames -> new DatasetNamesSQLOperator(
                    opDatasetNames,
                    context
            ).buildSQLQuery();
            // Property paths survive quadization wrapped in an OpGraph scope
            case OpGraph opGraph ->
                    buildSPARQLContext(PathRewriter.rewrite(opGraph.getNode(), opGraph.getSubOp()), context);
            case OpPath opPath ->
                    buildSPARQLContext(PathRewriter.rewrite(Quad.defaultGraphNodeGenerated, opPath), context);
            case OpZeroLengthPath opZLP -> new ZeroLengthPathSQLOperator(opZLP, context)
                    .buildSQLQuery();
            // Mixed BGP + path groups compile to a sequence of operators
            case OpSequence opSequence -> opSequence.getElements().stream()
                    .map(element -> buildSPARQLContext(element, context))
                    .reduce((left, right) -> new JoinSQLOperator(left, right).buildSQLQuery())
                    .orElse(new SQLQuery(null, context));
            // LET(?v := expr) has the same translation as BIND for a fresh variable
            case OpAssign opAssign ->
                    buildSPARQLContext(OpExtend.create(opAssign.getSubOp(), opAssign.getVarExprList()), context);
            // A lateral join without slice/group on the right-hand side is a plain join
            case OpLateral opLateral -> {
                if (containsSolutionModifier(opLateral.getRight())) {
                    throw new ARQNotImplemented(
                            "LATERAL with LIMIT/OFFSET or GROUP BY on the right-hand side is not implemented");
                }
                yield new JoinSQLOperator(
                        buildSPARQLContext(opLateral.getLeft(), context),
                        buildSPARQLContext(opLateral.getRight(), context)
                ).buildSQLQuery();
            }
            // Produced by the optimizer for ORDER BY + LIMIT
            case OpTopN opTopN -> buildSPARQLContext(
                    new OpSlice(new OpOrder(opTopN.getSubOp(), opTopN.getConditions()),
                            Long.MIN_VALUE, opTopN.getLimit()),
                    context);
            case OpLabel opLabel -> buildSPARQLContext(opLabel.getSubOp(), context);
            default -> throw new ARQNotImplemented("TODO: operator " + op.getClass().getName() + "not implemented");
        };
    }

    /**
     * A lateral right-hand side containing a slice or a grouping cannot be
     * translated as a join: those operators would apply globally instead of once
     * per left binding.
     */
    private static boolean containsSolutionModifier(Op op) {
        boolean[] found = new boolean[1];
        OpWalker.walk(op, new OpVisitorBase() {
            @Override
            public void visit(OpSlice opSlice) {
                found[0] = true;
            }

            @Override
            public void visit(OpTopN opTopN) {
                found[0] = true;
            }

            @Override
            public void visit(OpGroup opGroup) {
                found[0] = true;
            }
        });
        return found[0];
    }

    private SQLQuery getSqlQuery(Query query) {
        Op op = Algebra.compile(query);

        // Transform the op to a quad form
        Op quadOp = Algebra.toQuadForm(op);

        // Apply entailment rewriting if a regime is active
        if (entailmentRegime != EntailmentRegime.NONE) {
            List<EntailmentRule> rules = switch (entailmentRegime) {
                case RDFS -> RDFSRules.allRules();
                case OWL_LITE -> RDFSRules.allRules(); // OWL_LITE extends RDFS rules
                default -> List.of();
            };
            if (!rules.isEmpty()) {
                EntailmentRewriter rewriter = new EntailmentRewriter(rules);
                quadOp = rewriter.rewrite(quadOp);
                log.info("Entailment rewriting applied with regime: {}", entailmentRegime);
            }
        }

        Long startTranslation = System.nanoTime();
        SQLQuery builtQuery = buildSPARQLContext(quadOp);
        SQLQuery finalizedQuery = new FinalizeSQLOperator(builtQuery)
                .buildSQLQuery();

        Long endTranslation = System.nanoTime();

        queryTranslationDuration
                .labelValues(String.valueOf(queryCounter.get()))
                .observe(Unit.nanosToSeconds(endTranslation - startTranslation));
        log.info("[Measure] (Query translation duration): {} ns for query: {};", endTranslation - startTranslation, query);
        log.info("Query result: {};", finalizedQuery.getSql());
        return finalizedQuery;
    }

    private PgResultSet getSQLResultSet(Query query, SQLQuery finalizedQuery) throws SQLException {
        Long startExec = System.nanoTime();
        PgResultSet rs = (PgResultSet) jdbcConnection.executeSQL(finalizedQuery.getSql());
        Long endExec = System.nanoTime();
        queryExecutionDuration
                .labelValues(String.valueOf(queryCounter.get()))
                .observe(Unit.nanosToSeconds(endExec - startExec));
        log.info("[Measure] (Query execution duration): {} ns for query: {};", endExec - startExec, query);
        return rs;
    }

    /**
     * Instantiate the CONSTRUCT template against the current solution (row) of the
     * SQL result set. Template quads with an unbound variable or an illegal node
     * position are skipped, and blank nodes are scoped to the solution.
     */
    static List<Quad> instantiateTemplate(List<Quad> templateQuads, java.sql.ResultSet rs) throws SQLException {
        Map<Node, Node> solutionBNodes = new HashMap<>();
        List<Quad> quads = new ArrayList<>();

        for (Quad quad : templateQuads) {
            Node graphNode = getTypedNode(quad.getGraph(), rs, solutionBNodes);
            Node subjectNode = getTypedNode(quad.getSubject(), rs, solutionBNodes);
            Node predicateNode = getTypedNode(quad.getPredicate(), rs, solutionBNodes);
            Node objectNode = getTypedNode(quad.getObject(), rs, solutionBNodes);

            if (isLegalQuad(graphNode, subjectNode, predicateNode, objectNode)) {
                quads.add(new Quad(graphNode, subjectNode, predicateNode, objectNode));
            }
        }

        return quads;
    }

    private static boolean isLegalQuad(Node graph, Node subject, Node predicate, Node object) {
        return graph != null && subject != null && predicate != null && object != null
                && !graph.isLiteral() && !subject.isLiteral() && predicate.isURI();
    }

    private static Node getTypedNode(Node node, java.sql.ResultSet rs, Map<Node, Node> solutionBNodes) throws SQLException {
        if (node.isBlank()) {
            return solutionBNodes.computeIfAbsent(node, ignored -> NodeFactory.createBlankNode());
        }
        if (!node.isVariable()) {
            return node;
        }

        String v = node.getName();
        if (!PgUtils.hasColumn(rs, "name$" + v)) {
            return null;
        }

        String value = rs.getString("name$" + v);
        if (value == null) {
            return null;
        }

        String valueType;
        if (PgUtils.hasColumn(rs, "type$" + v)) {
            valueType = rs.getString("type$" + v);
        } else {
            valueType = PgUtils.getAssociatedRDFType(rs.getMetaData().getColumnType(rs.findColumn("name$" + v)));
        }

        return valueType == null ?
                NodeFactory.createURI(value) : NodeFactory.createLiteralDT(value, NodeFactory.getType(valueType));
    }
}
