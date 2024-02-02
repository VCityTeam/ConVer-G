package fr.cnrs.liris.jpugetgil.sparqltosql;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import fr.cnrs.liris.jpugetgil.sparqltosql.dao.ResourceOrLiteral;
import fr.cnrs.liris.jpugetgil.sparqltosql.dao.VersionedNamedGraph;
import org.apache.jena.graph.*;
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
        log.info(qu.getSql());

        return null;
    }

    private SQLQuery buildSPARQLContext(Op op) {
        return buildSPARQLContext(op, new SQLContext(null, new HashMap<>()));
    }

    private SQLQuery buildSPARQLContext(Op op, SQLContext context) {

        return switch (op) {
            case OpJoin opJoin -> {
                buildSPARQLContext(opJoin.getLeft(), context);
                buildSPARQLContext(opJoin.getRight(), context);
                yield null;
            }
            case OpLeftJoin opLeftJoin -> {
                buildSPARQLContext(opLeftJoin.getLeft(), context);
                buildSPARQLContext(opLeftJoin.getRight(), context);
                yield null;
            }
            case OpUnion opUnion -> {
//                buildSelectSQL(opUnion.getLeft(), context);
//                buildSelectSQL(opUnion.getRight(), context);
                yield null;
            }
            case OpProject opProject -> {
//                context.setProjections(opProject.getVars());
                yield buildSPARQLContext(opProject.getSubOp(), context);
            }
            case OpTable opTable -> {
                opTable.getTable().toString();
                yield null;
            }
            case OpQuadPattern opQuadPattern -> {
                opQuadPattern.getGraphNode().getURI();
                yield null;
            }
            case OpExtend opExtend -> {
                yield buildSPARQLContext(opExtend.getSubOp(), null);
            }
            case OpDistinct opDistinct -> {
                yield buildSPARQLContext(opDistinct.getSubOp(), null);
            }
            case OpFilter opFilter -> {
                yield buildSPARQLContext(opFilter.getSubOp(), null);
            }
            case OpOrder opOrder -> {
                yield buildSPARQLContext(opOrder.getSubOp(), null);
            }
            case OpGroup opGroup -> {
                yield buildSPARQLContext(opGroup.getSubOp(), null);
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
            case OpNull ignored -> {
                yield buildSPARQLContext(null, context);
            }
            case OpList opList -> {
                yield buildSPARQLContext(opList.getSubOp(), null);
            }
            case OpBGP opBGP -> {
                SQLContext cont = setURIsInMap(opBGP, context);
                getURIsIds();

                if (cont.graph() != null) {
                    yield buildContextBGPWithGraph(opBGP, cont);
                } else {
                    yield buildContextBGPWorkspace(opBGP, cont);
                }
            }
            case OpGraph opGraph -> {
                Map<Node, List<Occurence>> newVarOccurences = new HashMap<>(context.varOccurrences());

                Integer count = newVarOccurences
                        .keySet()
                        .stream()
                        .filter(
                                node -> newVarOccurences.get(node).stream().anyMatch(occurence -> occurence.getType().equals("graph"))
                        )
                        .map(node -> 1)
                        .reduce(0, Integer::sum, Integer::sum);

                newVarOccurences
                        .computeIfAbsent(opGraph.getNode(), k -> new ArrayList<>())
                        .add(new Occurence("graph", count));

                SQLContext cont = context.setGraph(opGraph.getNode());
                cont = cont.setVarOccurrences(newVarOccurences);
                yield buildSPARQLContext(opGraph.getSubOp(), cont);
                // FIXME : Add the graph to the SQLQuery
            }
            case OpTriple opTriple -> {
//                context.getTriples().add(opTriple.getTriple());
//                return context.buildSQL();
                yield null;
            }
            default -> throw new IllegalArgumentException("TODO: Unknown operator " + op.getClass().getName());
        };
    }

    private SQLContext setURIsInMap(OpBGP opBGP, SQLContext context) {
        Map<Node, List<Occurence>> newVarOccurrences = new HashMap<>(context.varOccurrences());

        for (int i = 0; i < opBGP.getPattern().getList().size(); i++) {
            Triple triple = opBGP.getPattern().getList().get(i);
            Node subject = triple.getSubject();
            Node predicate = triple.getPredicate();
            Node object = triple.getObject();

            newVarOccurrences
                    .computeIfAbsent(subject, k -> new ArrayList<>())
                    .add(new Occurence("subject", i));
            newVarOccurrences
                    .computeIfAbsent(predicate, k -> new ArrayList<>())
                    .add(new Occurence("predicate", i));
            newVarOccurrences
                    .computeIfAbsent(object, k -> new ArrayList<>())
                    .add(new Occurence("object", i));

            context = context.setVarOccurrences(newVarOccurrences);

            if (subject instanceof Node_URI) {
                uriToIdMap.put(subject.getURI(), null);
            }
            if (predicate instanceof Node_URI) {
                uriToIdMap.put(predicate.getURI(), null);
            }
            if (object instanceof Node_URI) {
                uriToIdMap.put(object.getURI(), null);
            }
            if (object instanceof Node_Literal) {
                uriToIdMap.put(object.getLiteralLexicalForm(), null);
            }
        }

        return context;
    }

    /**
     * Build the SQL query in a workspace context (no graph)
     *
     * @param opBGP   the BGP operator of the SPARQL query
     * @param context the context of the SQL query
     * @return the SQL query
     */
    private SQLQuery buildContextBGPWorkspace(OpBGP opBGP, SQLContext context) {
        String select = generateSelectWorkspace(context);
        String tables = generateFromTables(opBGP, true);
        return getSqlQuery(opBGP, context, select, tables, true);
    }

    /**
     * Build the SQL query in a graph context
     *
     * @param opBGP   the BGP operator of the SPARQL query
     * @param context the context of the SQL query
     * @return the SQL query
     */
    private SQLQuery buildContextBGPWithGraph(OpBGP opBGP, SQLContext context) {
        String select = generateSelect(opBGP, context);
        String tables = generateFromTables(opBGP, false);
        return getSqlQuery(opBGP, context, select, tables, false);
    }

    private SQLQuery getSqlQuery(OpBGP opBGP, SQLContext context, String select, String tables, boolean isWorkspace) {
        StringBuilder query = new StringBuilder("""
                SELECT {select}
                FROM {tables}
                """
                .replace(
                        "{select}",
                        select
                )
                .replace(
                        "{tables}",
                        tables
                )
        );

        String where;
        if (isWorkspace) {
            where = generateWhereWorkspace(opBGP);
        } else {
            where = generateWhere(opBGP, context);
        }
        if (!where.isEmpty()) {
            query.append("""
                    WHERE {where}
                    """
                    .replace("{where}", where)
            );
        }

        return new SQLQuery(query.toString(),
                context
        );
    }

    /**
     * Generate the SELECT clause of the SQL query
     *
     * @param context the context of the SPARQL query
     * @return the SELECT clause of the SQL query
     */
    private String generateSelect(OpBGP opBGP, SQLContext context) {
        return intersectionValidity(opBGP) + " as bs$" + context.graph().getName() + ", " +
               Streams.mapWithIndex(context.varOccurrences().keySet().stream().filter(node -> node instanceof Node_Variable), (node, index) -> {
                   if (context.varOccurrences().get(node).getFirst().getType().equals("graph")) {
                       return (
                               "t" + context.varOccurrences().get(node).getFirst().getPosition() +
                               ".id_named_graph as ng$" + node.getName()
                       );
                   }
                   return (
                           "t" + context.varOccurrences().get(node).getFirst().getPosition() +
                           "." + getColumnByOccurrence(context.varOccurrences().get(node).getFirst()) +
                           " as v$" + node.getName()
                   );
               }).collect(Collectors.joining(", \n"));
    }

    private static String intersectionValidity(OpBGP opBGP) {
        return "(" + Streams.mapWithIndex(opBGP.getPattern().getList().stream(), (triple, index) ->
                "t" + index + ".validity"
        ).collect(Collectors.joining(" & "))  + ")";
    }

    /**
     * Return the column name of the SQL query according to the occurrence type
     * @param occurrence the occurrence of the Node
     * @return the column name of the versioned quad table
     */
    private String getColumnByOccurrence(Occurence occurrence) {
        return switch (occurrence.getType()) {
            case "subject" -> "id_subject";
            case "predicate" -> "id_property";
            case "object" -> "id_object";
            default -> throw new IllegalArgumentException();
        };
    }

    /**
     * Generate the SELECT clause of the SQL query in a workspace context
     *
     * @param context the context of the SPARQL query
     * @return the SELECT clause of the SQL query
     */
    private String generateSelectWorkspace(SQLContext context) {
        return Streams.mapWithIndex(context.varOccurrences().keySet().stream().filter(node -> node instanceof Node_Variable), (node, index) ->
                "t" + context.varOccurrences().get(node).getFirst().getPosition() +
                "." + getColumnByOccurrence(context.varOccurrences().get(node).getFirst()) +
                " as t$" + node.getName()
        ).collect(Collectors.joining(", \n"));
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
     * Generate the WHERE clause of the SQL query with a graph variable
     *
     * @param opBGP   the BGP operator of the SPARQL query
     * @param context the context of the SPARQL query
     * @return the WHERE clause of the SQL query
     */
    private String generateWhere(OpBGP opBGP, SQLContext context) {
        StringBuilder where = new StringBuilder();
        StringBuilder validity = new StringBuilder();
        StringBuilder idSelect = new StringBuilder();
        List<Triple> triples = opBGP.getPattern().getList();

        if (context.graph() instanceof Node_Variable) {
            validity.append("bit_count").append(intersectionValidity(opBGP)).append(" <> 0");
        }

        for (int i = 0; i < triples.size(); i++) {
            switch (context.graph()) {
                case Node_Variable ignored -> {
                    StringBuilder temp = new StringBuilder();
                    // where
                    if (i < triples.size() - 1) {
                        temp.append("t").append(i).append(".id_named_graph = t").append(i + 1).append(".id_named_graph");
                        chainStringBuilders(where, temp);
                    }
                }
                case Node_URI nodeUri -> {
                    // where
                    VersionedNamedGraph versionedNamedGraph = getAssociatedVNG(nodeUri.getURI());
                    where.append("t").append(i).append(".id_named_graph = ").append(versionedNamedGraph.getIdNamedGraph());
                    // validity
                    validity.append("get_bit(t").append(i).append(".validity,").append(versionedNamedGraph.getIndex()).append(") = 1");
                }
                default -> throw new IllegalStateException("Unexpected value: " + context.graph());
            }

            buildFiltersOnIds(idSelect, triples, i);
        }

        chainStringBuilders(where, validity);
        chainStringBuilders(where, idSelect);

        return where.toString();
    }

    /**
     * Chain the StringBuilders with AND operator
     * @param sb the initial StringBuilder
     * @param sbs the chained StringBuilders
     */
    private void chainStringBuilders(StringBuilder sb, StringBuilder... sbs) {
        for (StringBuilder s : sbs) {
            if (!sb.isEmpty() && !s.isEmpty()) {
                sb.append(" AND ");
            }
            sb.append(s);
        }
    }

    /**
     * Build the filters on the IDs of the triple
     * @param idSelect the StringBuilder of the ids
     * @param triples the list of triples
     * @param i the index of the current triple
     */
    private void buildFiltersOnIds(StringBuilder idSelect, List<Triple> triples, int i) {
        Node subject = triples.get(i).getSubject();
        Node predicate = triples.get(i).getPredicate();
        Node object = triples.get(i).getObject();
        StringBuilder idSelectSubject = new StringBuilder();
        StringBuilder idSelectPredicate = new StringBuilder();
        StringBuilder idSelectObject = new StringBuilder();

        if (subject instanceof Node_URI) {
            idSelectSubject.append("t").append(i).append(".id_subject = ").append(uriToIdMap.get(subject.getURI()));
        }
        if (predicate instanceof Node_URI) {
            idSelectPredicate.append("t").append(i).append(".id_property = ").append(uriToIdMap.get(predicate.getURI()));
        }
        if (object instanceof Node_URI) {
            idSelectObject.append("t").append(i).append(".id_object = ").append(uriToIdMap.get(object.getURI()));
        } else if (object instanceof Node_Literal) {
            idSelectObject.append("t").append(i).append(".id_object = ").append(uriToIdMap.get(object.getLiteralLexicalForm()));
        }
        chainStringBuilders(idSelect, idSelectSubject, idSelectPredicate, idSelectObject);
    }

    private VersionedNamedGraph getAssociatedVNG(String uri) {
        Transaction tx = session.beginTransaction();
        VersionedNamedGraph versionedNamedGraph = session.createQuery(
                        "from VersionedNamedGraph vng join ResourceOrLiteral rl " +
                        "on vng.idVersionedNamedGraph = rl.idResourceOrLiteral where rl.name = :uri",
                        VersionedNamedGraph.class
                )
                .setParameter("uri", uri)
                .getSingleResult();
        tx.commit();

        return versionedNamedGraph;
    }

    /**
     * Generate the WHERE clause of the SQL query in a workspace context
     *
     * @param opBGP the BGP operator of the SPARQL query
     * @return the WHERE clause of the SQL query
     */
    private String generateWhereWorkspace(OpBGP opBGP) {
        StringBuilder where = new StringBuilder();
        StringBuilder idSelect = new StringBuilder();
        List<Triple> triples = opBGP.getPattern().getList();

        for (int i = 0; i < triples.size(); i++) {
            // where
            buildFiltersOnIds(idSelect, triples, i);
        }
        chainStringBuilders(where, idSelect);

       return where.toString();
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

            resourceOrLiterals.forEach(resourceOrLiteral -> uriToIdMap.put(resourceOrLiteral.getName(), resourceOrLiteral.getIdResourceOrLiteral()));
        }
    }
}
