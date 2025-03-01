package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLClause;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLUtils;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;
import fr.cnrs.liris.jpugetgil.converg.utils.Pair;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Variable;

import java.util.List;
import java.util.Map;

public class JoinSQLOperator extends SQLOperator {

    SQLQuery leftQuery;

    SQLQuery rightQuery;

    List<Pair<SQLVariable, SQLVariable>> commonVariables;

    public JoinSQLOperator(SQLQuery leftQuery, SQLQuery rightQuery) {
        this.leftQuery = leftQuery;
        this.rightQuery = rightQuery;
        this.commonVariables = SQLUtils.buildCommonsVariables(
                leftQuery.getContext().sqlVariables(),
                rightQuery.getContext().sqlVariables()
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
            commonVariables.forEach(sqlVariablePair -> {
                if (sqlVariablePair.getLeft().getSqlVarType().isLower(sqlVariablePair.getRight().getSqlVarType())) {
                    // Change the representation of the variable
                    leftQuery = new FlattenSQLOperator(leftQuery).buildSQLQuery();
                    sqlVariablePair.setLeft(sqlVariablePair.getRight());
                }
                if (sqlVariablePair.getLeft().getSqlVarType().isHigher(sqlVariablePair.getRight().getSqlVarType())) {
                    // Change the representation of the variable
                    rightQuery = new FlattenSQLOperator(rightQuery).buildSQLQuery();
                    sqlVariablePair.setRight(sqlVariablePair.getLeft());
                }

                sqlJoinClauseBuilder.and(
                        SQLVariable.join(
                                sqlVariablePair.getLeft(),
                                sqlVariablePair.getRight(),
                                "left_table",
                                "right_table"
                        )
                );
            });
        }

        return " FROM (" + leftQuery.getSql() + ") left_table JOIN (" +
                rightQuery.getSql() + ") right_table ON " + sqlJoinClauseBuilder.build().clause;
    }

    /**
     * @return
     */
    @Override
    protected String buildSelect() {
        Map<Node, List<SPARQLOccurrence>> mergedMapOccurrences = SQLUtils.mergeMapOccurrences(
                leftQuery.getContext().sparqlVarOccurrences(),
                rightQuery.getContext().sparqlVarOccurrences()
        );

        mergedMapOccurrences
                .keySet()
                .stream()
                .filter(Node_Variable.class::isInstance)
                .forEach((node) -> {
//            String variableSelect = SQLUtils.digestVarOccurrences(node, mergedMapOccurrences.get(node));
        });

²        return "";
    }

    /**
     * @return
     */
    @Override
    public SQLQuery buildSQLQuery() {
        String select = buildSelect();
        String from = buildFrom();
        String where = buildWhere();


        return null;
    }
}
