package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.Aggregator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;

import java.util.List;
import java.util.stream.Collectors;

public class StSGroupOperator extends StSOperator {
    private final OpGroup op;

    private SQLQuery sqlQuery;

    public StSGroupOperator(OpGroup op, SQLQuery sqlQuery) {
        this.op = op;
        this.sqlQuery = sqlQuery;
        this.sqlVariables = sqlQuery.getContext().sqlVariables();
    }

    @Override
    public SQLQuery buildSQLQuery() {
        if (sqlQuery.getContext().sqlVariables().stream()
                .anyMatch(sqlVar -> sqlVar.getSqlVarType() == SQLVarType.GRAPH_NAME)) {
            disaggregateBitVector();
        }

        getIdValues();

        VarExprList exprList = op.getGroupVars();
        List<Var> vars = exprList.getVars();
        String groupByVars = vars.stream()
                .map(variable -> "v$" + variable.getName())
                .collect(Collectors.joining(", "));
        String aggregatorsString = op.getAggregators().stream()
                .map(agg -> new Aggregator(agg).toSQLString())
                .collect(Collectors.joining(", "));

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

    private void getIdValues() {
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
        this.sqlQuery = new SQLQuery(select + from + join, sqlContext);
    }

    private void disaggregateBitVector() {
        String select = "SELECT " + getSelectDisaggregator();
        String from = " FROM (" + this.sqlQuery.getSql() + ") disagg \n";
        String join = getJoinDisaggregator();

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

        this.sqlQuery = new SQLQuery(select + from + join, sqlContext);
    }

    private String getJoinDisaggregator() {
        return this.sqlQuery.getContext().sqlVariables().stream()
                .filter(sqlVariable -> sqlVariable.getSqlVarType() == SQLVarType.GRAPH_NAME).map(sqlVariable -> (
                        "JOIN versioned_named_graph vng ON vng.id_named_graph = disagg.ng$" + sqlVariable.getSqlVarName() +
                                " AND get_bit(disagg.bs$" + sqlVariable.getSqlVarName() + ", vng.index_version) = 1"
                )).collect(Collectors.joining(" "));
    }

    /**
     * Gets the SELECT clause of the SQL query with the resource or literal table
     *
     * @return the SELECT clause of the SQL query
     */
    private String getSelectDisaggregator() {
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
