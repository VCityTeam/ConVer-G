package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Expression;
import fr.cnrs.liris.jpugetgil.converg.sql.*;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpFilter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FilterSQLOperator extends SQLOperator {
    private final OpFilter opFilter;
    //    private final Expression expression;
    private SQLQuery sqlQuery;

    private final String FILTER_TABLE_NAME = "filtered_table";

    public FilterSQLOperator(OpFilter opFilter, SQLQuery sqlQuery) {
        this.opFilter = opFilter;
        this.sqlQuery = sqlQuery;
//        this.expression = Expression.fromJenaExpr(opFilter.getExprs().get(0));
    }

    /**
     * @return the SQL query of the filter operator
     */
    @Override
    public SQLQuery buildSQLQuery() {
//        var filterCfg = new FilterConfiguration();
//        expression.updateFilterConfiguration(filterCfg, true);

        this.sqlQuery = flattenIdentifyQuery();

        String select = buildSelect();
        String materialization = buildMaterialization();
        String from = buildFrom();
        String where = buildWhere();

        return new SQLQuery(
                materialization + select + from + where,
                new SQLContext(
                        sqlQuery.getContext().sparqlVarOccurrences(), sqlQuery.getContext().condensedMode(), null, null
                ));
    }

    private String buildMaterialization() {
        return "WITH " + FILTER_TABLE_NAME + " AS MATERIALIZED (" + sqlQuery.getSql() + ")\n";
    }

    /**
     * @return the select of the filter operator
     */
    @Override
    protected String buildSelect() {
        return "SELECT * ";
    }

    /**
     * @return the from of the filter operator
     */
    @Override
    protected String buildFrom() {
        return "FROM " + FILTER_TABLE_NAME + " ";
    }

    /**
     * @return the where of the filter operator
     */
    @Override
    protected String buildWhere() {
        return "WHERE " + this.opFilter.getExprs().getList()
                .stream()
                .map(Expression::fromJenaExpr)
                .map(Expression::toSQLString)
                .collect(Collectors.joining(" AND "));
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
