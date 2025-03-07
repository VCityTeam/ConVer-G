package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.AbstractAggregator;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Aggregator;
import fr.cnrs.liris.jpugetgil.converg.sql.*;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprAggregator;

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

        // FIXME: implement the select, from and where functions
        String select = buildSelect();
        String from = buildFrom();
        String where = buildWhere();

        return new SQLQuery(
                select + from + where,
                null // FIXME
        );
    }

    /**
     * @return the select part of a group operator
     */
    @Override
    protected String buildSelect() {
        return null;
    }

    /**
     * @return the from part of a group operator
     */
    @Override
    protected String buildFrom() {
        return null;
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

        for (Node node: this.query.getContext().sparqlVarOccurrences()
                .keySet()) {
            SQLVariable maxVariable = SQLUtils.getMaxSQLVariableByOccurrences(this.query.getContext().sparqlVarOccurrences().get(node));

            if (
                    maxVariable.getSqlVarType() == SQLVarType.CONDENSED &&
                            opGroup.getGroupVars().getVars().stream()
                                    .anyMatch(var -> var.getVarName().equals(maxVariable.getSqlVarName()))
            ) {
                newQuery = new FlattenSQLOperator(newQuery, maxVariable).buildSQLQuery();
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
                for (Var var: aggregator.getAggregator().getExprList().getVarsMentioned()) {
                    newQuery = new IdentifySQLOperator(newQuery, new SQLVariable(SQLVarType.ID, var.getVarName()))
                            .buildSQLQuery();
                }
            }
        }

        return newQuery;
    }
}
