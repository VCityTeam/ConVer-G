package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sql.*;
import fr.cnrs.liris.jpugetgil.converg.utils.Pair;
import org.apache.jena.graph.Node;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JoinSQLOperator extends SQLOperator {

    SQLQuery leftQuery;

    SQLQuery rightQuery;

    List<Pair<SQLVariable, SQLVariable>> commonVariables;

    Map<Node, List<SPARQLOccurrence>> mergedMapOccurrences;

    public JoinSQLOperator(SQLQuery leftQuery, SQLQuery rightQuery) {
        this.leftQuery = leftQuery;
        this.rightQuery = rightQuery;
        this.commonVariables = SQLUtils.buildCommonsVariables(
                leftQuery.getContext().sparqlVarOccurrences(),
                rightQuery.getContext().sparqlVarOccurrences()
        );
    }

    /**
     * @return
     */
    @Override
    protected String buildWhere() {
        return "";
    }

    /**
     * @return
     */
    @Override
    protected String buildFrom() {
        SQLClause.SQLClauseBuilder sqlJoinClauseBuilder = new SQLClause.SQLClauseBuilder();

        if (commonVariables.isEmpty()) {
            sqlJoinClauseBuilder.and("1 = 1");
        } else {
            commonVariables.forEach(sqlVariablePair -> sqlJoinClauseBuilder.and(
                    sqlVariablePair.getLeft().joinJoin(
                            sqlVariablePair.getRight(),
                            "left_table",
                            "right_table"
                    )
            ));
        }

        return " FROM (" + leftQuery.getSql() + ") left_table JOIN (" +
                rightQuery.getSql() + ") right_table ON " + sqlJoinClauseBuilder.build().clause;
    }

    /**
     * @return
     */
    @Override
    protected String buildSelect() {
        mergedMapOccurrences = SQLUtils.mergeMapOccurrences(
                leftQuery.getContext().sparqlVarOccurrences(),
                rightQuery.getContext().sparqlVarOccurrences()
        );

        return "SELECT " + mergedMapOccurrences
                .keySet()
                .stream()
                .map((node) -> SQLUtils.generateNodeProjectionByListSPARQLOccurrences(
                        leftQuery.getContext().sparqlVarOccurrences().get(node),
                        rightQuery.getContext().sparqlVarOccurrences().get(node)
                ))
                .collect(Collectors.joining(", ")) + "\n";
    }

    /**
     * @return
     */
    @Override
    public SQLQuery buildSQLQuery() {
        joinSubQueries();

        String select = buildSelect();
        String from = buildFrom();
        String where = buildWhere();

        return new SQLQuery(
                select + from + where,
                new SQLContext(
                        mergedMapOccurrences,
                        leftQuery.getContext().condensedMode()
                ));
    }

    private void joinSubQueries() {
        commonVariables.forEach(sqlVariablePair -> {
            if (sqlVariablePair.getLeft().getSqlVarType().isLower(sqlVariablePair.getRight().getSqlVarType())) {
                // Change the representation of the variable
                leftQuery = new FlattenSQLOperator(leftQuery, sqlVariablePair.getRight()).buildSQLQuery();
                sqlVariablePair.setLeft(sqlVariablePair.getRight());
            }
            if (sqlVariablePair.getLeft().getSqlVarType().isHigher(sqlVariablePair.getRight().getSqlVarType())) {
                // Change the representation of the variable
                rightQuery = new FlattenSQLOperator(rightQuery, sqlVariablePair.getRight()).buildSQLQuery();
                sqlVariablePair.setRight(sqlVariablePair.getLeft());
            }
        });
    }
}
