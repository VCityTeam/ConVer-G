package fr.cnrs.liris.jpugetgil.sparqltosql;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLContextType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator.*;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is used to translate a SPARQL query into a SQL query
 */
public class SPARQLtoSQLTranslator {

    private static final Logger log = LoggerFactory.getLogger(SPARQLtoSQLTranslator.class);

    public SPARQLtoSQLTranslator() {
    }

    /**
     * Translate the SPARQL query into a SQL query
     *
     * @return the SQL query
     */
    public String translate(Query query) {
        Op op = Algebra.compile(query);
        SQLQuery qu = buildSPARQLContext(op);

        String select = "SELECT " + getSelectVariablesResourceOrLiteral(qu.getContext().sqlVariables());
        String from = " FROM (" + qu.getSql() + ") indexes_table";
        String join = getJoinVariablesResourceOrLiteral(qu.getContext().sqlVariables());

        SQLQuery finalQuery = new SQLQuery(
                select + from + join,
                qu.getContext()
        );

        log.info(finalQuery.getSql());
        return finalQuery.getSql();
    }

    private SQLQuery buildSPARQLContext(Op op) {
        return buildSPARQLContext(op, new SQLContext(null, new HashMap<>(), new ArrayList<>()));
    }

    private SQLQuery buildSPARQLContext(Op op, SQLContext context) {
        return switch (op) {
            case OpJoin opJoin -> {
                SQLQuery leftSQLQuery = buildSPARQLContext(opJoin.getLeft(), context);
                SQLQuery rightSQLQuery = buildSPARQLContext(opJoin.getRight(), context);
                yield new StSJoinOperator(leftSQLQuery, rightSQLQuery)
                        .buildSQLQuery();
            }
            case OpLeftJoin opLeftJoin -> {
                // Jointure avec un/des var qui sont dans un optional
                buildSPARQLContext(opLeftJoin.getLeft(), context);
                buildSPARQLContext(opLeftJoin.getRight(), context);
                yield null;
            }
            case OpUnion opUnion -> {
                SQLQuery leftQuery = buildSPARQLContext(opUnion.getLeft(), context);
                SQLQuery rightQuery = buildSPARQLContext(opUnion.getRight(), context);

                yield new StSUnionOperator(leftQuery, rightQuery)
                        .buildSQLQuery();
            }
            case OpProject opProject -> {
                SQLQuery sqlQuery = buildSPARQLContext(opProject.getSubOp(), context);
                yield new StSProjectOperator(opProject, sqlQuery)
                        .buildSQLQuery();
            }
            case OpTable ignored -> new SQLQuery(
                    null,
                    context
            );
            case OpQuadPattern opQuadPattern -> {
                opQuadPattern.getGraphNode().getURI();
                yield null;
            }
            case OpExtend opExtend -> {
                VarExprList varExprList = opExtend.getVarExprList();
                List<Var> vars = varExprList.getVars();
                Map<Var, Expr> exprMap = varExprList.getExprs();
                yield buildSPARQLContext(opExtend.getSubOp(), context);
            }
            case OpDistinct opDistinct -> {
                SQLQuery sqlQuery = buildSPARQLContext(opDistinct.getSubOp(), context);
                yield new StSDistinctOperator(sqlQuery)
                        .buildSQLQuery();
            }
            case OpFilter opFilter -> {
                yield buildSPARQLContext(opFilter.getSubOp(), null);
            }
            case OpOrder opOrder -> {
                yield buildSPARQLContext(opOrder.getSubOp(), null);
            }
            case OpGroup opGroup -> {
                SQLQuery sqlQuery = buildSPARQLContext(opGroup.getSubOp(), context);
                SQLContext sqlContext = new SQLContext(
                        sqlQuery.getContext().graph(),
                        sqlQuery.getContext().sparqlVarOccurrences(),
                        sqlQuery.getContext().sqlVariables() // FIXME
                );

                // TODO : GROUP BY
                VarExprList exprList = opGroup.getGroupVars();
                List<Var> vars = exprList.getVars();
                Map<Var, Expr> exprVar = exprList.getExprs();
                String groupBy = vars.stream()
                        .map(var -> {
                            if (sqlQuery.getContext().sparqlVarOccurrences().get(var).stream()
                                    .anyMatch(SPARQLOccurrence -> SPARQLOccurrence.getType() == SPARQLPositionType.GRAPH_NAME)) {
                                return "group_table.ng$" + var.getName();
                            } else {
                                return "group_table.v$" + var.getName();
                            }
                        })
                        .collect(Collectors.joining(", "));

                // FIXME : projections
                yield new SQLQuery(
                        "SELECT * FROM (" + sqlQuery.getSql() +
                                ") GROUP BY (" + groupBy + ")",
                        sqlContext
                );
            }
            case OpSlice opSlice -> {
                yield buildSPARQLContext(opSlice.getSubOp(), null);
            }
            case OpTopN opTopN -> {
                yield buildSPARQLContext(opTopN.getSubOp(), null);
            }
            case OpPath opPath -> {
                yield null;
            }
            case OpLabel opLabel -> {
                yield buildSPARQLContext(opLabel.getSubOp(), null);
            }
            case OpNull opNull -> {
                yield buildSPARQLContext(null, context);
            }
            case OpList opList -> {
                yield buildSPARQLContext(opList.getSubOp(), null);
            }
            case OpBGP opBGP -> {
                SQLContext cont = addURIsToContext(opBGP, context);
                yield new StSBGPOperator(opBGP, cont)
                        .buildSQLQuery();
            }
            case OpGraph opGraph -> {
                Integer count = context.sparqlVarOccurrences()
                        .keySet()
                        .stream()
                        .filter(
                                node -> context.sparqlVarOccurrences().get(node).stream()
                                        .anyMatch(SPARQLOccurrence -> SPARQLOccurrence.getType() == SPARQLPositionType.GRAPH_NAME)
                        )
                        .map(node -> 1)
                        .reduce(0, Integer::sum, Integer::sum);

                Map<Node, List<SPARQLOccurrence>> newVarOccurrences = new HashMap<>(context.sparqlVarOccurrences());

                newVarOccurrences
                        .computeIfAbsent(opGraph.getNode(), k -> new ArrayList<>())
                        .add(new SPARQLOccurrence(SPARQLPositionType.GRAPH_NAME, count, SPARQLContextType.DATASET));

                SQLContext cont = context.setGraph(opGraph.getNode())
                        .setVarOccurrences(newVarOccurrences);

                SQLQuery sqlQuery = buildSPARQLContext(opGraph.getSubOp(), cont);
                yield new StSGraphOperator(opGraph, sqlQuery)
                        .buildSQLQuery();
            }
            case OpTriple opTriple -> {
//                context.getTriples().add(opTriple.getTriple());
//                return context.buildSQL();
                yield null;
            }
            default -> throw new IllegalArgumentException("TODO: Unknown operator " + op.getClass().getName());
        };
    }

    /**
     * Collect the occurrences of the variables in the BGP
     *
     * @param opBGP   the current BGP
     * @param context the current SQL context
     * @return the modified SQL context
     */
    private SQLContext addURIsToContext(OpBGP opBGP, SQLContext context) {
        Map<Node, List<SPARQLOccurrence>> newVarOccurrences = new HashMap<>(context.sparqlVarOccurrences());
        SPARQLContextType sparqlContextType = context.graph() == null ? SPARQLContextType.WORKSPACE : SPARQLContextType.DATASET;

        for (int i = 0; i < opBGP.getPattern().getList().size(); i++) {
            Triple triple = opBGP.getPattern().getList().get(i);
            Node subject = triple.getSubject();
            Node predicate = triple.getPredicate();
            Node object = triple.getObject();

            newVarOccurrences.computeIfAbsent(subject, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(SPARQLPositionType.SUBJECT, i, sparqlContextType));
            newVarOccurrences.computeIfAbsent(predicate, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(SPARQLPositionType.PROPERTY, i, sparqlContextType));
            newVarOccurrences.computeIfAbsent(object, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(SPARQLPositionType.OBJECT, i, sparqlContextType));

            context = context.setVarOccurrences(newVarOccurrences);
        }

        return context;
    }

    /**
     * Get the SELECT clause of the SQL query with the resource or literal table
     *
     * @param sqlVariables the list of SQL variables
     * @return the SELECT clause of the SQL query
     */
    private String getSelectVariablesResourceOrLiteral(List<SQLVariable> sqlVariables) {
        return Streams.mapWithIndex(sqlVariables.stream()
                .filter(sqlVariable -> sqlVariable.getSqlVarType() != SQLVarType.BIT_STRING), (sqlVariable, index) -> (
                "rl" + index + ".name as name$" + sqlVariable.getSqlVarName() + ", rl" + index + ".type as type$" + sqlVariable.getSqlVarName()
        )).collect(Collectors.joining(", "));
    }

    private String getJoinVariablesResourceOrLiteral(List<SQLVariable> sqlVariables) {
        return Streams.mapWithIndex(sqlVariables.stream()
                .filter(sqlVariable -> sqlVariable.getSqlVarType() != SQLVarType.BIT_STRING), (sqlVariable, index) ->
                switch (sqlVariable.getSqlVarType()) {
                    case DATA:
                        yield " JOIN resource_or_literal rl" + index + " ON indexes_table.v$" +
                                sqlVariable.getSqlVarName() +
                                " = rl" + index + ".id_resource_or_literal";
                    case GRAPH_NAME:
                        yield " JOIN versioned_named_graph vng" + index + " ON indexes_table.ng$" +
                                sqlVariable.getSqlVarName() + " = vng" + index +
                                ".id_named_graph AND get_bit(indexes_table.bs$" +
                                sqlVariable.getSqlVarName() + ", vng" + index + ".index_version) = 1 \n" +
                                " JOIN resource_or_literal rl" + index + " ON vng" + index + ".id_versioned_named_graph = rl" +
                                index + ".id_resource_or_literal";
                    case VERSIONED_NAMED_GRAPH:
                        yield " JOIN resource_or_literal rl" + index + " ON " +
                                "indexes_table.vng$" + sqlVariable.getSqlVarName() +
                                " = rl" + index + ".id_resource_or_literal";
                    default:
                        yield "";
                }).collect(Collectors.joining(" \n"));
    }
}
