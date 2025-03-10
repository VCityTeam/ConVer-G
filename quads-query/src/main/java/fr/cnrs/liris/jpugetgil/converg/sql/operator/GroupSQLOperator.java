package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractAggregator;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Aggregator;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLUtils;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprAggregator;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class GroupSQLOperator extends SQLOperator {

    OpGroup opGroup;

    SQLQuery query;

    public GroupSQLOperator(OpGroup opGroup, SQLQuery query) {
        this.opGroup = opGroup;
        this.query = query;
    }

    /**
     * @return the SQL query of the group operator
     */
    @Override
    public SQLQuery buildSQLQuery() {
        this.query = computeSQLQueryWithGroupByVars();
        this.query = getValueFromAggregates();

        String select = buildSelect();
        String from = buildFrom();
        String groupByVars = " GROUP BY (" + getGroupByVars() + ")";

        return new SQLQuery(
                "SELECT " + select + from + groupByVars,
                query.getContext()
        );
    }

    /**
     * @return the select part of a group operator
     */
    @Override
    protected String buildSelect() {
        String groupByVars = getGroupByVars();

        Map<Node, List<SPARQLOccurrence>> sparqlVarOccurrences = this.query.getContext().sparqlVarOccurrences();

        List<SQLVariable> minSQLVariables = sparqlVarOccurrences.keySet()
                .stream()
                .map(sparqlVarOccurrences::get)
                .map(SQLUtils::getMinSQLVariableByOccurrences)
                .toList();

        String aggregatorsString;

        if (minSQLVariables.stream().anyMatch(sqlVariable -> sqlVariable.getSqlVarType() == SQLVarType.CONDENSED)) {
            log.debug("Use condensed representation for aggregations function");
            SQLVariable condensedSQLVariable = minSQLVariables.stream()
                    .min(Comparator.comparingInt((SQLVariable sqlVar) -> sqlVar.getSqlVarType().level)
                    ).orElseThrow();

            StringJoiner joiner = new StringJoiner(", ");
            for (ExprAggregator agg : opGroup.getAggregators()) {
                String sqlString = new Aggregator(agg).toSQLString(opGroup, condensedSQLVariable.getSqlVarName());
                joiner.add(sqlString);
            }
            aggregatorsString = joiner.toString();
        } else {
            log.debug("Cannot use the consended representation for aggregations function");

            aggregatorsString = opGroup.getAggregators().stream()
                    .map(agg -> new Aggregator(agg).toSQLString())
                    .collect(Collectors.joining(", "));
        }

        return !groupByVars.isBlank() && !aggregatorsString.isBlank() ? groupByVars + ", " + aggregatorsString :
                aggregatorsString + groupByVars;
    }

    /**
     * @return the from part of a group operator
     */
    @Override
    protected String buildFrom() {
        String GROUP_BY_TABLE_NAME = "gp";
        return " FROM (" + this.query.getSql() + ") " + GROUP_BY_TABLE_NAME;
    }

    /**
     * @return the where part of a group operator
     */
    @Override
    protected String buildWhere() {
        return null;
    }

    private SQLQuery computeSQLQueryWithGroupByVars() {
        // check if the group by variable are not in CONDENSED representation, else flatten them
        SQLQuery newQuery = this.query;

        for (Node node : this.query.getContext().sparqlVarOccurrences()
                .keySet()) {
            SPARQLOccurrence maxSPARQLOccurrence = SQLUtils.getMaxSPARQLOccurrence(this.query.getContext().sparqlVarOccurrences().get(node));

            // check if the variable is in CONDENSED representation and is a group by variable
            if (
                    maxSPARQLOccurrence.getSqlVariable().getSqlVarType() == SQLVarType.CONDENSED &&
                            opGroup.getGroupVars().getVars().stream()
                                    .anyMatch(var -> var.getVarName().equals(maxSPARQLOccurrence.getSqlVariable().getSqlVarName()))
            ) {
                newQuery = new FlattenSQLOperator(newQuery, maxSPARQLOccurrence.getSqlVariable()).buildSQLQuery();
            }
        }

        return newQuery;
    }

    private SQLQuery getValueFromAggregates() {
        // check if the projected variables are in VALUE representation, else transform them
        SQLQuery newQuery = this.query;

        for (ExprAggregator agg : opGroup.getAggregators()) {
            AbstractAggregator<?> aggregator = new Aggregator(agg).getAggregator();
            if (aggregator.getRequiresValue()) {
                for (Var var : aggregator.getAggregator().getExprList().getVarsMentioned()) {
                    newQuery = new IdentifySQLOperator(newQuery, new SQLVariable(SQLVarType.ID, var.getVarName()))
                            .buildSQLQuery();
                }
            }
        }

        return newQuery;
    }

    /**
     * Computes the SQL formatting of the group by variables
     *
     * @return Return the group by variables in the select or group by part of a group operator
     */
    private String getGroupByVars() {
        return opGroup.getGroupVars().getVars()
                .stream()
                .map(variable -> new SQLVariable(SQLVarType.VALUE, variable.getName()).getSelect()) //
                .collect(Collectors.joining(", "));
    }
}
