package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import com.google.common.collect.Streams;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.converg.sparql.expressions.Expression;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLUtils;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLVariable;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class FinalizeSQLOperator extends SQLOperator {

    SQLQuery query;

    private final String FINALIZE_TABLE_NAME = "indexes_table";

    public FinalizeSQLOperator(SQLQuery query) {
        this.query = query;
    }

    /**
     * @return the SQL query of the quad pattern
     */
    @Override
    public SQLQuery buildSQLQuery() {
        flattenAndIdentifyAllVariables();
        String select = buildSelect();
        String from = buildFrom();
        String join = buildJoin();

        this.query.setSql(select + from + join);

        if (this.query.getContext().opSlice() != null) {
            insertLimit();
        }

        if (this.query.getContext().opOrder() != null) {
            insertOrder();
        }

        return new SQLQuery(
                this.query.getSql(),
                this.query.getContext()
        );
    }

    private void insertOrder() {
        String select = "SELECT * ";
        String from = "FROM (" + this.query.getSql() + ") ord_table \n";
        String orderBy = "ORDER BY " + this.query.getContext().opOrder().getConditions().stream()
                .map(sortCondition -> {
                    String direction = switch (sortCondition.direction) {
                        case Query.ORDER_DEFAULT -> "";
                        case Query.ORDER_ASCENDING -> "ASC";
                        case Query.ORDER_DESCENDING -> "DESC";
                        default -> throw new IllegalStateException("Unexpected value: " + sortCondition.direction);
                    };
                    Expression columnExpression = Expression.fromJenaExpr(sortCondition.getExpression());
                    String column = columnExpression.toNameSQLString();
                    return column + " " + direction;
                }).collect(Collectors.joining(", "));

        this.query.setSql(select + from + orderBy);
    }

    private void insertLimit() {
        String select = "SELECT * ";
        String from = " FROM (" + this.query.getSql() + ") sl \n";
        String limit;
        if (this.query.getContext().opSlice().getStart() > 0) {
            limit = "LIMIT " + this.query.getContext().opSlice().getLength() + " OFFSET " + this.query.getContext().opSlice().getStart();
        } else {
            limit = "LIMIT " + this.query.getContext().opSlice().getLength();
        }
        this.query.setSql(select + from + limit);
    }

    private void flattenAndIdentifyAllVariables() {
        SQLQuery finalQuery = this.query;

        for (Map.Entry<Node, List<SPARQLOccurrence>> entry : this.query.getContext().sparqlVarOccurrences().entrySet()) {
            List<SPARQLOccurrence> sparqlOccurrences = entry.getValue();
            SPARQLOccurrence maxSPARQLOccurrence = SQLUtils.getMaxSPARQLOccurrence(sparqlOccurrences);

            if (maxSPARQLOccurrence.getSqlVariable().getSqlVarType() == SQLVarType.CONDENSED) {
                finalQuery = new FlattenSQLOperator(finalQuery, maxSPARQLOccurrence.getSqlVariable()).buildSQLQuery();
            }
        }

        this.query = finalQuery;
    }

    /**
     * @return the select part of extend operator
     */
    @Override
    protected String buildSelect() {
        return "SELECT " + Streams.mapWithIndex(this.query.getContext().sparqlVarOccurrences().keySet().stream(), (node, index) -> {
            SPARQLOccurrence maxSPARQLOccurrence = SQLUtils.getMaxSPARQLOccurrence(
                    this.query.getContext().sparqlVarOccurrences().get(node)
            );

            SQLVariable maxSQLVariable = maxSPARQLOccurrence.getSqlVariable();
            if (maxSPARQLOccurrence.getType() == SPARQLPositionType.AGGREGATED) {
                maxSQLVariable.setSqlVarName(maxSQLVariable.getSqlVarName().replace(".", "agg"));
                return (
                        maxSQLVariable.getSelect() + " as name$" +
                                maxSQLVariable.getSqlVarName()
                );
            } else {
                return (
                        "rl" + index + ".name as name$" + maxSQLVariable.getSqlVarName() +
                                ", rl" + index + ".type as type$" + maxSQLVariable.getSqlVarName()
                );
            }
        }).collect(Collectors.joining(", "));
    }

    /**
     * @return the from part of extend operator
     */
    @Override
    protected String buildFrom() {
        return " FROM (" + this.query.getSql() + ") " + FINALIZE_TABLE_NAME;
    }

    /**
     * @return the where part of extend operator
     */
    @Override
    protected String buildWhere() {
        return null;
    }

    private String buildJoin() {
        return Streams.mapWithIndex(this.query.getContext().sparqlVarOccurrences().keySet().stream(), (node, index) -> {
                    SPARQLOccurrence maxSPARQLOccurrence = SQLUtils.getMaxSPARQLOccurrence(
                            this.query.getContext().sparqlVarOccurrences().get(node)
                    );

                    SQLVariable maxSQLVariable = maxSPARQLOccurrence.getSqlVariable();
                    if (maxSPARQLOccurrence.getType() == SPARQLPositionType.AGGREGATED) {
                        return null;
                    } else {
                        return " JOIN resource_or_literal rl" + index + " ON " +
                                maxSQLVariable.getSelect(FINALIZE_TABLE_NAME) + " = rl" + index + ".id_resource_or_literal";
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n "));
    }
}
