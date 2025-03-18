package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Expression;
import fr.cnrs.liris.jpugetgil.converg.sparql.transformer.FilterConfiguration;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import org.apache.jena.sparql.algebra.op.OpFilter;

public class FilterSQLOperator extends SQLOperator  {
    private final OpFilter opFilter;
    private final Expression expression;
    private SQLQuery sqlQuery;

    public FilterSQLOperator(OpFilter opFilter, SQLQuery sqlQuery) {
        this.opFilter = opFilter;
        this.sqlQuery = sqlQuery;
        this.expression = Expression.fromJenaExpr(opFilter.getExprs().get(0));
    }

    /**
     * @return the SQL query of the filter operator
     */
    @Override
    public SQLQuery buildSQLQuery() {
        var filterCfg = new FilterConfiguration();
        expression.updateFilterConfiguration(filterCfg, true);

        String select = buildSelect();
        String from = buildFrom();
        String where = buildWhere();

        return new SQLQuery(
                select + from + where,
                new SQLContext(
                        sqlQuery.getContext().sparqlVarOccurrences(), sqlQuery.getContext().condensedMode(), null, null
                ));
    }

    /**
     * @return the select of the filter operator
     */
    @Override
    protected String buildSelect() {
        return "SELECT * \n";
    }

    /**
     * @return the from of the filter operator
     */
    @Override
    protected String buildFrom() {
        return "FROM (" + sqlQuery.getSql() + ") filtered_table\n";
    }

    /**
     * @return the where of the filter operator
     */
    @Override
    protected String buildWhere() {
        return "WHERE " + expression.toSQLString();
    }
}
