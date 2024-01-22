package fr.cnrs.liris.jpugetgil.sparqltosql;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementWalker;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.stream.Collectors;

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

        String sql = buildSPARQLContext(op, null);
        log.info(sql);

        tx.commit();

        return null;
    }

    private String buildSPARQLContext(Op op, OpContext context) {
        if (context == null) {
            context = new OpContext();
        }

        return switch (op) {
            case OpJoin opJoin -> {
                buildSPARQLContext(opJoin.getLeft(), context);
                buildSPARQLContext(opJoin.getRight(), context);
                yield buildSQLFromContext(context);
            }
            case OpLeftJoin opLeftJoin -> {
                buildSPARQLContext(opLeftJoin.getLeft(), context);
                buildSPARQLContext(opLeftJoin.getRight(), context);
                yield buildSQLFromContext(context);
            }
            case OpUnion opUnion -> {
                // TODO
//                buildSelectSQL(opUnion.getLeft(), context);
//                buildSelectSQL(opUnion.getRight(), context);
                yield buildSQLFromContext(context);
            }
            case OpProject opProject -> {
                context.setProjections(opProject.getVars());

                buildSPARQLContext(opProject.getSubOp(), context);
                yield buildSQLFromContext(context);
            }
            case OpTable opTable -> {
                opTable.getTable().toString();
                yield buildSQLFromContext(context);
            }
            case OpQuadPattern opQuadPattern -> {
                opQuadPattern.getGraphNode().getURI();

                yield buildSQLFromContext(context);
            }
            case OpExtend opExtend -> {
                buildSPARQLContext(opExtend.getSubOp(), null);
                yield buildSQLFromContext(context);
            }
            case OpDistinct opDistinct -> {
                buildSPARQLContext(opDistinct.getSubOp(), null);
                yield buildSQLFromContext(context);
            }
            case OpFilter opFilter -> {
                buildSPARQLContext(opFilter.getSubOp(), null);
                yield buildSQLFromContext(context);
            }
            case OpOrder opOrder -> {
                buildSPARQLContext(opOrder.getSubOp(), null);
                yield buildSQLFromContext(context);
            }
            case OpGroup opGroup -> {
                buildSPARQLContext(opGroup.getSubOp(), null);
                yield buildSQLFromContext(context);
            }
            case OpSlice opSlice -> {
                buildSPARQLContext(opSlice.getSubOp(), null);
                yield buildSQLFromContext(context);
            }
            case OpTopN opTopN -> {
                buildSPARQLContext(opTopN.getSubOp(), null);
                yield buildSQLFromContext(context);
            }
            case OpPath opPath -> {
                // TODO : implement
                yield buildSQLFromContext(context);
            }
            case OpLabel opLabel -> {
                buildSPARQLContext(opLabel.getSubOp(), null);
                yield buildSQLFromContext(context);
            }
            case OpNull ignored -> buildSQLFromContext(context);
            case OpList opList -> {
                buildSPARQLContext(opList.getSubOp(), null);
                yield buildSQLFromContext(context);
            }
            case OpBGP opBGP -> {
                context.setTriples(opBGP.getPattern().getList());
                yield buildSQLFromContext(context);
            }
            case OpGraph opGraph -> {
                context.setGraph(opGraph.getNode());

                buildSPARQLContext(opGraph.getSubOp(), context);

                yield buildSQLFromContext(context);
            }
            case OpTriple opTriple -> {
                context.getTriples().add(opTriple.getTriple());
                yield buildSQLFromContext(context);
            }
            default -> throw new IllegalStateException("Unexpected value: " + op.getName());
        };
    }

    private String buildSQLFromContext(OpContext context) {
        String sql = "SELECT {{projections}} FROM {{table}} table \n";

        if (!context.getTriples().isEmpty()) {
            sql += Streams.mapWithIndex(context.getTriples().stream(), (triple, index) -> {
                String tripleSql = "";

                if (triple.getSubject().isURI()) {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index) + " ON table.id_subject = rl" + (4 * index) + ".id_resource_or_literal AND rl" + (4 * index) + ".name = '" + triple.getSubject().getURI() + "'\n";
                } else {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index) + " ON table.id_subject = rl" + (4 * index) + ".id_resource_or_literal\n";
                }

                if (triple.getPredicate().isURI()) {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index + 1) + " ON table.id_property = rl" + (4 * index + 1) + ".id_resource_or_literal AND rl" + (4 * index + 1) + ".name = '" + triple.getPredicate().getURI() + "'\n";
                } else {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index + 1) + " ON table.id_property = rl" + (4 * index + 1) + ".id_resource_or_literal\n";
                }

                if (triple.getObject().isURI()) {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index + 2) + " ON table.id_object = rl" + (4 * index + 2) + ".id_resource_or_literal AND rl" + (4 * index + 2) + ".name = '" + triple.getObject().getURI() + "'\n";

                } else {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index + 2) + " ON table.id_object = rl" + (4 * index + 2) + ".id_resource_or_literal\n";
                }

                if (context.getGraph() != null && context.getGraph().isURI()) {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index + 3) + " ON table.id_named_graph = rl" + (4 * index + 3) + ".id_resource_or_literal AND rl" + (4 * index + 3) + ".name = '" + context.getGraph().getURI() + "'\n";
                } else if (context.getGraph() != null) {
                    tripleSql += "JOIN resource_or_literal rl" + (4 * index + 3) + " ON table.id_named_graph = rl" + (4 * index + 3) + ".id_resource_or_literal\n";
                }

                return tripleSql;
            }).collect(Collectors.joining(" AND "));
        }

        if (context.getGraph() != null) {
            sql = sql.replace("{{table}}", "versioned_quad");
        } else {
            sql = sql.replace("{{table}}", "workspace");
        }

        if (context.getProjections().isEmpty()) {
            // FIXME : add all columns that are variables
            sql = sql.replace("{{projections}}", "*");
        } else {
            // FIXME : add all prefix to variables
            sql = sql.replace("{{projections}}", context.getProjections().stream().map(Var::getVarName).collect(Collectors.joining(", ")));
        }

        return sql;
    }
}
