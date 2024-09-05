package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import com.google.common.collect.Streams;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractAggregator;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Aggregator;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class StSGroupOperator extends StSOperator {
    private static final Logger log = LoggerFactory.getLogger(StSGroupOperator.class);

    private final OpGroup op;

    private SQLQuery sqlQuery;

    public StSGroupOperator(OpGroup op, SQLQuery sqlQuery) {
        this.op = op;
        this.sqlQuery = sqlQuery;
    }

    @Override
    public SQLQuery buildSQLQuery() {
        String groupByVars = op.getGroupVars().getVars()
                .stream()
                .map(variable -> "v$" + variable.getName())
                .collect(Collectors.joining(", "));
        String aggregatorsString;

        if (sqlQuery.getContext().condensedMode() &&
                isAggregatedOnTriple() &&
                isCountableAggregator()) {
            log.info("Condensed mode and aggregation on triple and countable aggregators.");
            StringJoiner joiner = new StringJoiner(", ");
            for (ExprAggregator agg : op.getAggregators()) {
                AbstractAggregator<?> aggregator = new Aggregator(agg).getAggregator();
                if (aggregator.getRequiresValue()) {
                    this.sqlQuery = new StSIdentifyOperator(sqlQuery, aggregator.getAggregator().getExprList().getVarsMentioned())
                            .buildSQLQuery();
                }
                String sqlString = new Aggregator(agg).toSQLString(op, sqlQuery.getContext().graph().getName());
                joiner.add(sqlString);
            }
            aggregatorsString = joiner.toString();

        } else {
            this.sqlQuery = disaggregateBitVectorAndReturnQuery();
            this.sqlQuery = setIdValuesAndReturnQuery();

            aggregatorsString = op.getAggregators().stream()
                    .map(agg -> new Aggregator(agg).toSQLString())
                    .collect(Collectors.joining(", "));
        }

        String projections;
        if (!groupByVars.isBlank() && !aggregatorsString.isBlank()) {
            projections = groupByVars + ", " + aggregatorsString;
        } else {
            projections = aggregatorsString + groupByVars;
        }

        return new SQLQuery(
                "SELECT " + projections + " FROM (" + sqlQuery.getSql() +
                        ") sq\n GROUP BY (" + groupByVars + ")",
                sqlQuery.getContext()
        );
    }

    /**
     * Checks if the aggregation is a countable-compatible aggregator
     *
     * @return true if the aggregation is a countable-compatible aggregator, false otherwise
     */
    private boolean isCountableAggregator() {
        return op.getAggregators()
                .stream()
                .allMatch(agg ->
                        new Aggregator(agg).isCountable()
                );
    }

    /**
     * Checks if the aggregation is done on the triple (not on the graph name)
     *
     * @return true if the aggregation is done on the triple, false otherwise
     */
    private boolean isAggregatedOnTriple() {
        return op.getGroupVars().getVars()
                .stream()
                .noneMatch(
                        groupVar -> sqlQuery.getContext().sqlVariables()
                                .stream()
                                .anyMatch(
                                        sqlVar -> sqlVar.getSqlVarName().equals(groupVar.getName()) &&
                                                sqlVar.getSqlVarType() == SQLVarType.GRAPH_NAME
                                )
                );
    }

    /**
     * Sets the id values of the resources or literals
     *
     * @return the new SQL query with the id values of the resources or literals
     */
    private SQLQuery setIdValuesAndReturnQuery() {
        String select = "SELECT " + getSelectIdValues();
        String from = " FROM (" + this.sqlQuery.getSql() + ") idValues \n";
        String join = getJoinIdValues();

        List<SQLVariable> sqlVariables = this.sqlQuery.getContext().sqlVariables().stream()
                .filter(sqlVar -> sqlVar.getSqlVarType() != SQLVarType.BIT_STRING)
                .map(sqlVar -> {
                    if (sqlVar.getSqlVarType() == SQLVarType.GRAPH_NAME) {
                        return new SQLVariable(SQLVarType.DATA, sqlVar.getSqlVarName(), true);
                    } else {
                        return new SQLVariable(sqlVar.getSqlVarType(), sqlVar.getSqlVarName(), true);
                    }
                }).toList();

        SQLContext sqlContext = this.sqlQuery.getContext().setSQLVariables(sqlVariables);
        return new SQLQuery(select + from + join, sqlContext);
    }

    /**
     * Disaggregates the bit vector
     *
     * @return the new SQL query with the disaggregated bit vector
     */
    private SQLQuery disaggregateBitVectorAndReturnQuery() {
        String select = "SELECT " + getSelectDisaggregation();
        String from = " FROM (" + this.sqlQuery.getSql() + ") disagg \n";
        String join = getJoinDisaggregation();

        List<SQLVariable> sqlVariables = this.sqlQuery.getContext().sqlVariables().stream()
                .filter(sqlVar -> sqlVar.getSqlVarType() != SQLVarType.BIT_STRING)
                .map(sqlVar -> {
                    if (sqlVar.getSqlVarType() == SQLVarType.GRAPH_NAME) {
                        return new SQLVariable(SQLVarType.DATA, sqlVar.getSqlVarName());
                    } else {
                        return new SQLVariable(sqlVar.getSqlVarType(), sqlVar.getSqlVarName());
                    }
                }).toList();

        SQLContext sqlContext = this.sqlQuery.getContext().setSQLVariables(sqlVariables);

        return new SQLQuery(select + from + join, sqlContext);
    }

    /**
     * Gets the JOIN clause of the SQL query with the versioned named graph table (bitstring explosion)
     *
     * @return the JOIN clause of the SQL query
     */
    private String getJoinDisaggregation() {
        return this.sqlQuery.getContext().sqlVariables().stream()
                .filter(sqlVariable -> sqlVariable.getSqlVarType() == SQLVarType.GRAPH_NAME).map(sqlVariable -> (
                        "JOIN versioned_named_graph vng ON vng.id_named_graph = disagg.ng$" + sqlVariable.getSqlVarName() +
                                " AND get_bit(disagg.bs$" + sqlVariable.getSqlVarName() + ", vng.index_version - 1) = 1"
                )).collect(Collectors.joining(" "));
    }

    /**
     * Gets the SELECT clause of the SQL query with the resource or literal table
     *
     * @return the SELECT clause of the SQL query
     */
    private String getSelectDisaggregation() {
        return this.sqlQuery.getContext().sqlVariables().stream()
                .filter(sqlVariable -> sqlVariable.getSqlVarType() != SQLVarType.BIT_STRING)
                .map(sqlVariable -> {
                    if (sqlVariable.getSqlVarType() == SQLVarType.GRAPH_NAME) {
                        return (
                                "vng.id_versioned_named_graph AS v$" + sqlVariable.getSqlVarName()
                        );
                    } else {
                        return (
                                "v$" + sqlVariable.getSqlVarName()
                        );
                    }
                }).collect(Collectors.joining(", "));
    }

    private String getSelectIdValues() {
        return Streams.mapWithIndex(this.sqlQuery.getContext().sqlVariables().stream()
                                .filter(sqlVariable -> sqlVariable.getSqlVarType() != SQLVarType.BIT_STRING),
                        (sqlVariable, index) -> "rol" + index + ".name AS v$" + sqlVariable.getSqlVarName())
                .collect(Collectors.joining(", "));
    }

    private String getJoinIdValues() {
        return Streams.mapWithIndex(this.sqlQuery.getContext().sqlVariables().stream()
                                .filter(sqlVariable -> sqlVariable.getSqlVarType() != SQLVarType.BIT_STRING),
                        (sqlVariable, index) -> "JOIN resource_or_literal rol" + index + " ON rol" + index +
                                ".id_resource_or_literal = idValues.v$" + sqlVariable.getSqlVarName())
                .collect(Collectors.joining(" "));

    }
}
