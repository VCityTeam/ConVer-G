package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.connection.JdbcConnection;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.operator.*;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Summary;
import io.prometheus.metrics.model.snapshots.Unit;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.ARQNotImplemented;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.DatasetImpl;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * This class is used to translate a SPARQL query into a SQL query
 */
public class SPARQLtoSQLTranslator extends SPARQLLanguageTranslator {

    private static final Logger log = LoggerFactory.getLogger(SPARQLtoSQLTranslator.class);

    private final JdbcConnection jdbcConnection;

    private final Summary queryTranslationDuration = MetricsSingleton.getInstance().queryTranslationDuration;

    private final Summary queryExecutionDuration = MetricsSingleton.getInstance().queryExecutionDuration;

    private final Counter selectQueryCounter = MetricsSingleton.getInstance().selectQueryCounter;

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
        Op op = Algebra.compile(query);

        // Transform the op to a quad form
        Op quadOp = Algebra.toQuadForm(op);

        Long startTranslation = System.nanoTime();
        SQLQuery builtQuery = buildSPARQLContext(quadOp);
        SQLQuery finalizedQuery = new FinalizeSQLOperator(builtQuery)
                .buildSQLQuery();

        Long endTranslation = System.nanoTime();

        queryTranslationDuration
                .labelValues(String.valueOf(selectQueryCounter.get()))
                .observe(Unit.nanosToSeconds(endTranslation - startTranslation));
        log.info("[Measure] (Query translation duration): {} ns for query: {};", endTranslation - startTranslation, query);
        log.info("Query result: {};", finalizedQuery.getSql());

        try {
            Long startExec = System.nanoTime();
            java.sql.ResultSet rs = jdbcConnection.executeSQL(finalizedQuery.getSql());
            Long endExec = System.nanoTime();
            queryExecutionDuration
                    .labelValues(String.valueOf(selectQueryCounter.get()))
                    .observe(Unit.nanosToSeconds(endExec - startExec));
            log.info("[Measure] (Query execution duration): {} ns for query: {};", endExec - startExec, query);

            BindingIterator bindingIterator = new BindingIterator(rs);

            Set<Var> vars = bindingIterator
                    .getVars();

            return ResultSetStream.create(new ArrayList<>(vars), bindingIterator);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    Dataset translateAndExecConstruct(Query query) {
        // TODO: Implement the Construct query
        Op op = Algebra.compile(query);


        return null;
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
}
