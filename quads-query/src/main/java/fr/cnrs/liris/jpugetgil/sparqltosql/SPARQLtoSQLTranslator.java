package fr.cnrs.liris.jpugetgil.sparqltosql;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import fr.cnrs.liris.jpugetgil.sparqltosql.dao.ResourceOrLiteral;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Node_Variable;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
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

    private final Session session;

    private final Map<String, Integer> uriToIdMap = new HashMap<>();

    private final Map<Node, List<Occurence>> varOccurences = new HashMap<>();

    public SPARQLtoSQLTranslator(SessionFactory sessionFactory) {
        this.session = sessionFactory.openSession();
    }

    /**
     * Translate the SPARQL query into a SQL query
     *
     * @return the SQL query
     */
    public ResultSet translate(Query query) {
        Op op = Algebra.compile(query);

        SQLQuery qu = buildSPARQLContext(op);
        log.info(qu.sql);

        return null;
    }

    private SQLQuery buildSPARQLContext(Op op) {
        return buildSPARQLContext(op, new SQLContext(null, new HashMap<>()));
    }

    private SQLQuery buildSPARQLContext(Op op, SQLContext context) {

        switch (op) {
            case OpJoin opJoin -> {
                buildSPARQLContext(opJoin.getLeft(), context);
                buildSPARQLContext(opJoin.getRight(), context);
            }
            case OpLeftJoin opLeftJoin -> {
                buildSPARQLContext(opLeftJoin.getLeft(), context);
                buildSPARQLContext(opLeftJoin.getRight(), context);
            }
            case OpUnion opUnion -> {
                // TODO
//                buildSelectSQL(opUnion.getLeft(), context);
//                buildSelectSQL(opUnion.getRight(), context);
            }
            case OpProject opProject -> {
//                context.setProjections(opProject.getVars());
                buildSPARQLContext(opProject.getSubOp(), context);
            }
            case OpTable opTable -> {
                opTable.getTable().toString();
            }
            case OpQuadPattern opQuadPattern -> {
                opQuadPattern.getGraphNode().getURI();
            }
            case OpExtend opExtend -> {
                buildSPARQLContext(opExtend.getSubOp(), null);
            }
            case OpDistinct opDistinct -> {
                buildSPARQLContext(opDistinct.getSubOp(), null);
            }
            case OpFilter opFilter -> {
                buildSPARQLContext(opFilter.getSubOp(), null);
            }
            case OpOrder opOrder -> {
                buildSPARQLContext(opOrder.getSubOp(), null);
            }
            case OpGroup opGroup -> {
                buildSPARQLContext(opGroup.getSubOp(), null);
            }
            case OpSlice opSlice -> {
                buildSPARQLContext(opSlice.getSubOp(), null);
            }
            case OpTopN opTopN -> {
                buildSPARQLContext(opTopN.getSubOp(), null);
            }
            case OpPath opPath -> {
                // TODO : implement
            }
            case OpLabel opLabel -> {
                buildSPARQLContext(opLabel.getSubOp(), null);
            }
            case OpNull ignored -> {
                buildSPARQLContext(null, context);
            }
            case OpList opList -> {
                buildSPARQLContext(opList.getSubOp(), null);
            }
            case OpBGP opBGP -> {
                setURIsInMap(opBGP);
                getURIsIds();

                if (context.graph() != null) {
                    return buildContextBGPWithGraph(opBGP, context.setURI_ids(uriToIdMap));
                } else {
                    return buildContextBGPWorkspace(opBGP, context.setURI_ids(uriToIdMap));
                }
            }
            case OpGraph opGraph -> {
                SQLContext cont = context.setGraph(opGraph.getNode());
                buildSPARQLContext(opGraph.getSubOp(), cont);
            }
            case OpTriple opTriple -> {
//                context.getTriples().add(opTriple.getTriple());
//                return context.buildSQL();
            }
            default -> throw new IllegalArgumentException("TODO: Implementation of the operator "  + op.getClass().getName());
        }

        throw new IllegalArgumentException("TODO: Implementation of the operator " + op.getClass().getName());
    }

    private void setURIsInMap(OpBGP opBGP) {
        for (int i = 0; i < opBGP.getPattern().getList().size(); i++) {
            Triple triple = opBGP.getPattern().getList().get(i);
            Node subject = triple.getSubject();
            Node predicate = triple.getPredicate();
            Node object = triple.getObject();

            varOccurences
                    .computeIfAbsent(subject, k -> new ArrayList<>())
                    .add(new Occurence("subject", i));
            varOccurences
                    .computeIfAbsent(predicate, k -> new ArrayList<>())
                    .add(new Occurence("predicate", i));
            varOccurences
                    .computeIfAbsent(object, k -> new ArrayList<>())
                    .add(new Occurence("object", i));

            if (subject instanceof Node_URI) {
                uriToIdMap.put(subject.getURI(), null);
            }
            if (predicate instanceof Node_URI) {
                uriToIdMap.put(predicate.getURI(), null);
            }
            if (object instanceof Node_URI) {
                uriToIdMap.put(object.getURI(), null);
            }
        }
    }

    /**
     * Build the SQL query in a workspace context (no graph)
     *
     * @param opBGP   the BGP operator of the SPARQL query
     * @param context the context of the SQL query
     * @return the SQL query
     */
    private SQLQuery buildContextBGPWorkspace(OpBGP opBGP, SQLContext context) {
        String select = generateSelectWorkspace(opBGP);
        String tables = generateFromTables(opBGP, true);
        String where = generateWhereWorkspace(opBGP);

        return new SQLQuery("""
                SELECT {select}
                FROM {tables}
                WHERE {where}
                """
                .replace(
                        "{select}",
                        select
                )
                .replace(
                        "{tables}",
                        tables
                )
                .replace(
                        "{where}",
                        where
                )
        );
    }

    /**
     * Build the SQL query in a graph context
     *
     * @param opBGP   the BGP operator of the SPARQL query
     * @param context the context of the SQL query
     * @return the SQL query
     */
    private SQLQuery buildContextBGPWithGraph(OpBGP opBGP, SQLContext context) {
        String select = generateSelect(opBGP);
        String tables = generateFromTables(opBGP, false);

        // FIXME: End implementation
        switch (context.graph()) {
            case Node_Variable nodeVariable -> {
                String where = generateWhere(opBGP, nodeVariable);

                return new SQLQuery("""
                        SELECT {select}
                        FROM {tables}
                        WHERE {where}
                        """
                        .replace(
                                "{select}",
                                select
                        )
                        .replace(
                                "{tables}",
                                tables
                        )
                        .replace(
                                "{where}",
                                where
                        )
                );
            }
            case Node_URI nodeUri -> {
                String where = generateWhere(opBGP, nodeUri);
                return new SQLQuery("""
                            SELECT {select}
                            FROM {tables}
                            WHERE {where}
                        """
                        .replace(
                                "{select}",
                                select
                        )
                        .replace(
                                "{tables}",
                                tables
                        )
                        .replace(
                                "{where}",
                                where
                        )
                );
            }
            default -> throw new IllegalStateException("Unexpected value: " + context.graph());
        }
    }

    /**
     * Generate the SELECT clause of the SQL query
     *
     * @param opBGP the BGP operator of the SPARQL query
     * @return the SELECT clause of the SQL query
     */
    private String generateSelect(OpBGP opBGP) {
        return Streams.mapWithIndex(opBGP.getPattern().getList().stream(), (triple, index) ->
                "t" + index + ".id_subject as v" + index + "$subject, " +
                        "t" + index + ".id_property as v" + index + "$predicate, " +
                        "t" + index + ".id_object as v" + index + "$object, " +
                        "t" + index + ".id_named_graph as gn" + index + "$graph, " +
                        "t" + index + ".validity & t" + index + ".validity as bs" + index + "$graph"
        ).collect(Collectors.joining(",\n"));
    }

    /**
     * Generate the SELECT clause of the SQL query in a workspace context
     *
     * @param opBGP the BGP operator of the SPARQL query
     * @return the SELECT clause of the SQL query
     */
    private String generateSelectWorkspace(OpBGP opBGP) {
        return Streams.mapWithIndex(opBGP.getPattern().getList().stream(), (triple, index) ->
                "t" + index + ".id_subject as w" + index + "$subject, " +
                        "t" + index + ".id_property as w" + index + "$predicate, " +
                        "t" + index + ".id_object as w" + index + "$object "
        ).collect(Collectors.joining(",\n"));
    }

    /**
     * Generate the FROM clause of the SQL query
     *
     * @param opBGP       the BGP operator of the SPARQL query
     * @param isWorkspace true if the query is in a workspace context, false otherwise
     * @return the FROM clause of the SQL query
     */
    private String generateFromTables(OpBGP opBGP, boolean isWorkspace) {
        return Streams.mapWithIndex(opBGP.getPattern().getList().stream(), (triple, index) -> {
            if (isWorkspace) {
                return ("workspace t" + index);
            } else {
                return ("versioned_quad t" + index);
            }
        }).collect(Collectors.joining(", "));
    }

    /**
     * Generate the WHERE clause of the SQL query with a graph URI
     *
     * @param opBGP   the BGP operator of the SPARQL query
     * @param nodeUri the graph URI of the SPARQL query
     * @return the WHERE clause of the SQL query
     */
    private String generateWhere(OpBGP opBGP, Node_URI nodeUri) {
        Integer idNamedGraph = uriToIdMap.get(nodeUri.getURI());
        throw new IllegalArgumentException("TODO: Implementation of the function");
    }

    /**
     * Generate the WHERE clause of the SQL query with a graph variable
     *
     * @param opBGP   the BGP operator of the SPARQL query
     * @param ignored the graph variable of the SPARQL query
     * @return the WHERE clause of the SQL query
     */
    private String generateWhere(OpBGP opBGP, Node_Variable ignored) {
        StringBuilder where = new StringBuilder();
        StringBuilder validity = new StringBuilder("(");
        StringBuilder idSelect = new StringBuilder();
        List<Triple> triples = opBGP.getPattern().getList();

        for (int i = 0; i < triples.size() - 1; i++) {
            where.append("t").append(i).append(".id_named_graph = t").append(i + 1).append(".id_named_graph AND ");
            validity.append("t").append(i).append(".validity & t").append(i + 1).append(".validity");
            if (i < opBGP.getPattern().getList().size() - 2) {
                validity.append(" AND ");
            }

            Node subject = triples.get(i).getSubject();
            Node predicate = triples.get(i).getPredicate();
            Node object = triples.get(i).getObject();

            if (subject instanceof Node_URI) {
                idSelect.append("t").append(i).append(".id_subject = ").append(uriToIdMap.get(subject.getURI())).append(" AND ");
            } else {
                idSelect.append("(1 = 1) AND ");
            }
            if (predicate instanceof Node_URI) {
                idSelect.append("t").append(i).append(".id_property = ").append(uriToIdMap.get(predicate.getURI())).append(" AND ");
            } else {
                idSelect.append("(1 = 1) AND ");
            }
            if (object instanceof Node_URI) {
                idSelect.append("t").append(i).append(".id_object = ").append(uriToIdMap.get(object.getURI())).append(" ");
            } else {
                idSelect.append("(1 = 1) ");
            }
        }
        validity.append(") <> B'0' AND ");
        where.append(validity);

        if (!idSelect.isEmpty()) {
            where.append(idSelect);
        } else {
            where.append("(1 = 1)");
        }
        return where.toString();
    }

    /**
     * Generate the WHERE clause of the SQL query in a workspace context
     * @param opBGP the BGP operator of the SPARQL query
     * @return the WHERE clause of the SQL query
     */
    private String generateWhereWorkspace(OpBGP opBGP) {
        throw new IllegalArgumentException("TODO: Implementation of the function");
    }

    /**
     * Fills the map of URIs with their IDs
     */
    private void getURIsIds() {
        if (!uriToIdMap.isEmpty()) {
            Transaction tx = session.beginTransaction();
            List<ResourceOrLiteral> resourceOrLiterals =
                    session.createSelectionQuery("from ResourceOrLiteral where name in :names", ResourceOrLiteral.class)
                            .setParameter("names", uriToIdMap.keySet())
                            .getResultList();
            tx.commit();

            resourceOrLiterals.forEach(resourceOrLiteral -> {
                uriToIdMap.put(resourceOrLiteral.getName(), resourceOrLiteral.getIdResourceOrLiteral());
            });
        }
    }
}
