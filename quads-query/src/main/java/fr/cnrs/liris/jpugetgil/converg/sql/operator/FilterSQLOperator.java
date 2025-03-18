package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Expression;
import fr.cnrs.liris.jpugetgil.converg.sparql.transformer.FilterConfiguration;
import fr.cnrs.liris.jpugetgil.converg.sql.*;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.core.Var;

import java.util.List;
import java.util.Map;

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

        this.sqlQuery = flattenIdentifyQuery();

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

    private SQLQuery flattenIdentifyQuery() {
        SQLQuery newQuery = this.sqlQuery;

        boolean requiresValue = opFilter.getExprs().getList().stream()
                .map(Expression::fromJenaExpr)
                .anyMatch(Expression::requiresValue);

        if (requiresValue) {
            for (Map.Entry<Node, List<SPARQLOccurrence>> entry : this.sqlQuery.getContext().sparqlVarOccurrences().entrySet()) {
                List<SPARQLOccurrence> sparqlOccurrences = entry.getValue();
                SPARQLOccurrence maxSPARQLOccurrence = SQLUtils.getMaxSPARQLOccurrence(sparqlOccurrences);

                if (
                        opFilter.getExprs().getVarsMentioned()
                                .stream()
                                .anyMatch(var -> var.getVarName().equals(maxSPARQLOccurrence.getSqlVariable().getSqlVarName()))
                ) {
                    if (maxSPARQLOccurrence.getSqlVariable().getSqlVarType() == SQLVarType.CONDENSED) {
                        newQuery = new FlattenSQLOperator(newQuery, maxSPARQLOccurrence.getSqlVariable()).buildSQLQuery();

                        SQLVariable newSQLVar = maxSPARQLOccurrence.getSqlVariable();
                        newSQLVar.setSqlVarType(SQLVarType.ID);
                        maxSPARQLOccurrence.setSqlVariable(newSQLVar);
                    }

                    if (maxSPARQLOccurrence.getSqlVariable().getSqlVarType() == SQLVarType.ID) {
                        newQuery = new IdentifySQLOperator(newQuery, maxSPARQLOccurrence.getSqlVariable()).buildSQLQuery();
                    }
                }
            }
        }

        return newQuery;
    }
}
