package fr.cnrs.liris.jpugetgil.sparqltosql;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLContextType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator.*;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        SQLQuery qu = buildSPARQLContext(op)
                .finalizeQuery();

        log.info(qu.getSql());
        return qu.getSql();
    }

    private SQLQuery buildSPARQLContext(Op op) {
        return buildSPARQLContext(op, new SQLContext(null, new HashMap<>(), new ArrayList<>()));
    }

    private SQLQuery buildSPARQLContext(Op op, SQLContext context) {
        return switch (op) {
            case OpJoin opJoin -> new StSJoinOperator(
                    buildSPARQLContext(opJoin.getLeft(), context),
                    buildSPARQLContext(opJoin.getRight(), context)
            ).buildSQLQuery();
            case OpLeftJoin opLeftJoin -> {
                // Jointure avec un/des var qui sont dans un optional
                buildSPARQLContext(opLeftJoin.getLeft(), context);
                buildSPARQLContext(opLeftJoin.getRight(), context);
                throw new IllegalArgumentException("TODO: OpLeftJoin not implemented");
            }
            case OpUnion opUnion -> new StSUnionOperator(
                    buildSPARQLContext(opUnion.getLeft(), context),
                    buildSPARQLContext(opUnion.getRight(), context)
            ).buildSQLQuery();
            case OpProject opProject -> new StSProjectOperator(
                    opProject,
                    buildSPARQLContext(opProject.getSubOp(), context)
            ).buildSQLQuery();
            case OpTable ignored -> new SQLQuery(
                    null,
                    context
            );
            case OpTriple ignored -> new SQLQuery(
                    null,
                    context
            );
            case OpNull ignored -> new SQLQuery(
                    null,
                    context
            );
            case OpDistinct opDistinct -> new StSDistinctOperator(
                    buildSPARQLContext(opDistinct.getSubOp(), context)
            ).buildSQLQuery();
            case OpExtend opExtend -> new StSExtendOperator(
                    opExtend,
                    buildSPARQLContext(opExtend.getSubOp(), context)
            ).buildSQLQuery();
            case OpGroup opGroup -> new StSGroupOperator(opGroup, buildSPARQLContext(opGroup.getSubOp(), context))
                    .buildSQLQuery();
            case OpQuadPattern opQuadPattern ->
                    throw new IllegalArgumentException("TODO: OpQuadPattern not implemented");
            case OpFilter opFilter -> throw new IllegalArgumentException("TODO: OpFilter not implemented");
            case OpSlice opSlice -> throw new IllegalArgumentException("TODO: OpSlice not implemented");
            case OpOrder opOrder -> throw new IllegalArgumentException("TODO: OpOrder not implemented");
            case OpTopN opTopN -> throw new IllegalArgumentException("TODO: OpTopN not implemented");
            case OpPath opPath -> throw new IllegalArgumentException("TODO: OpPath not implemented");
            case OpLabel opLabel -> throw new IllegalArgumentException("TODO: OpLabel not implemented");
            case OpList opList -> throw new IllegalArgumentException("TODO: OpList not implemented");
            case OpBGP opBGP -> new StSBGPOperator(opBGP, addURIsToContext(opBGP, context))
                    .buildSQLQuery();
            case OpGraph opGraph -> {
                Integer count = context.sparqlVarOccurrences()
                        .keySet()
                        .stream()
                        .filter(
                                node -> context.sparqlVarOccurrences().get(node).stream()
                                        .anyMatch(sparqlOccurrence -> sparqlOccurrence.getType() == SPARQLPositionType.GRAPH_NAME)
                        )
                        .map(node -> 1)
                        .reduce(0, Integer::sum, Integer::sum);

                Map<Node, List<SPARQLOccurrence>> newVarOccurrences = new HashMap<>(context.sparqlVarOccurrences());

                newVarOccurrences
                        .computeIfAbsent(opGraph.getNode(), k -> new ArrayList<>())
                        .add(new SPARQLOccurrence(SPARQLPositionType.GRAPH_NAME, count, SPARQLContextType.DATASET));

                SQLContext cont = context.setGraph(opGraph.getNode())
                        .setVarOccurrences(newVarOccurrences);

                yield new StSGraphOperator(opGraph, buildSPARQLContext(opGraph.getSubOp(), cont))
                        .buildSQLQuery();
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
}
