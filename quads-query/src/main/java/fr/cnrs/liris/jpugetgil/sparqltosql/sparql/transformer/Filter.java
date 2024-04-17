package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.transformer;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.Expression;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import org.apache.jena.sparql.algebra.op.OpFilter;

public class Filter {
    private final OpFilter opFilter;
    private final Expression expression;

    public Filter(OpFilter opFilter) {
        this.opFilter = opFilter;
        this.expression = Expression.fromJenaExpr(opFilter.getExprs().get(0));
    }

    public SQLQuery buildSQLQuery(SQLContext context) {
        var filterCfg = new ExpressionVariableClassification();
        expression.updateFilterConfiguration(filterCfg, true);
        // TODO: build from
        // TODO: map variables to values or ids from subquery
        // TODO: generate SQL expression
        throw new IllegalStateException("Implementation not finished"); // TODO: remove once implemented
    }

    public OpFilter getOp() {
        return opFilter;
    }
}
