package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.sparql.algebra.op.OpGraph;

import java.util.List;
import java.util.stream.Collectors;

public class StSGraphOperator extends StSOperator {
    private final OpGraph op;
    private final StSOperator subOperator;

    public StSGraphOperator(SQLContext enclosingContext, OpGraph op) {
        super(enclosingContext.setGraph(op.getNode())); // we change the context graph
        this.op = op;
        subOperator = fromSPARQL(this.context, op);
    }

    @Override
    public SQLQuery buildSQLQuery() {
        return subOperator.buildSQLQuery();
        // removed useless query that enclosed the subquery as this operator only changed the context graph
        // this works because we have implemented StSNullOperator
    }

    @Override
    public List<SQLVariable> getSQLVariables() {
        return subOperator.getSQLVariables();
    }
}
