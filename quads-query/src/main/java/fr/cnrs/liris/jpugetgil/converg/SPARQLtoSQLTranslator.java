package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.connection.JdbcConnection;
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
     * @param condensedMode if true, the SQL query build will use the condensed mode
     */
    public SPARQLtoSQLTranslator(boolean condensedMode) {
        super(condensedMode);
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
                for (Quad quad : quadList) {
                    Node graphNode = getTypedNode(quad.getGraph(), rs);
                    Node subjectNode = getTypedNode(quad.getSubject(), rs);
                    Node predicateNode = getTypedNode(quad.getPredicate(), rs);
                    Node objectNode = getTypedNode(quad.getObject(), rs);

                    dsg.add(new Quad(graphNode, subjectNode, predicateNode, objectNode));
                }
            }

            return DatasetFactory.wrap(dsg);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public SQLQuery buildSPARQLContext(Op op) {
        return buildSPARQLContext(op, new SQLContext(new HashMap<>(), condensedMode, null, null));
    }

    private SQLQuery buildSPARQLContext(Op op, SQLContext context) {
        return switch (op) {
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
            case OpQuadPattern opQuadPattern -> new QuadPatternSQLOperator(opQuadPattern, context)
                    .buildSQLQuery();
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
            case OpAssign opAssign -> throw new ARQNotImplemented("TODO: OpAssign not implemented");
            case OpLateral opLateral -> throw new ARQNotImplemented("TODO: OpLateral not implemented");
            case OpTopN opTopN -> throw new ARQNotImplemented("TODO: OpTopN not implemented");
            case OpPath opPath -> throw new ARQNotImplemented("TODO: OpPath not implemented");
            case OpLabel opLabel -> throw new ARQNotImplemented("TODO: OpLabel not implemented");
            default -> throw new ARQNotImplemented("TODO: operator " + op.getClass().getName() + "not implemented");
        };
    }

    private SQLQuery getSqlQuery(Query query) {
        Op op = Algebra.compile(query);

        // Transform the op to a quad form
        Op quadOp = Algebra.toQuadForm(op);

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

    private static Node getTypedNode(Node node, java.sql.ResultSet rs) throws SQLException {
        if (node.isVariable()) {
            String v = node.getName();
            String value = rs.getString("name$" + v);

            if (PgUtils.hasColumn(rs, "type$" + v)) {
                String valueType = rs.getString("type$" + v);

                node = valueType == null ?
                        NodeFactory.createURI(value) : NodeFactory.createLiteral(value, NodeFactory.getType(valueType));
            } else {
                node = NodeFactory.createURI(rs.getString("name$" + node.getName()));
            }
        }

        return node;
    }
}
