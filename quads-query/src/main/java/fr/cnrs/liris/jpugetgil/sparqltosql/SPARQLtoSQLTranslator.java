package fr.cnrs.liris.jpugetgil.sparqltosql;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Node_Variable;
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
import java.util.HashMap;

/**
 * This class is used to translate a SPARQL query into a SQL query
 */
public class SPARQLtoSQLTranslator {

    private static final Logger log = LoggerFactory.getLogger(SPARQLtoSQLTranslator.class);

    private final Session session;

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

        Transaction tx = session.beginTransaction();

        SQLQuery qu = buildSPARQLContext(op);
        log.info(qu.sql);

        tx.commit();

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
                if (context.graph() != null) {
                    return buildContextBGPWithGraph(opBGP, context);
                } else {
                    return buildContextBGPWorkspace(opBGP, context);
                }
            }
            case OpGraph opGraph -> {
                context.setGraph(opGraph.getNode());
                buildSPARQLContext(opGraph.getSubOp(), context);
            }
            case OpTriple opTriple -> {
//                context.getTriples().add(opTriple.getTriple());
//                return context.buildSQL();
            }
            default -> throw new IllegalArgumentException("TODO: Implementation of the operator");
        }
//        return context.buildSQL();
        throw new IllegalArgumentException("TODO: Implementation of the operator");
    }

    private SQLQuery buildContextBGPWorkspace(OpBGP opBGP, SQLContext context) {
        // FIXME: Implement
        throw new IllegalArgumentException("TODO: Implementation of the function");
    }

    private SQLQuery buildContextBGPWithGraph(OpBGP opBGP, SQLContext context) {
        // FIXME: Implement
        switch (context.graph()) {
            case Node_Variable nodeVariable -> {
                return new SQLQuery("""
                    
                """);
            }
            case Node_URI node_uri -> {
                return new SQLQuery("""
                        
                """);
            }
            default -> throw new IllegalStateException("Unexpected value: " + context.graph());
        }
    }
}
