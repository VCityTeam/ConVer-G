package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.connection.JdbcConnection;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLContextType;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.operator.*;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * This class is used to translate a SPARQL query into a SQL query
 */
public class SPARQLtoSQLTranslator extends SPARQLLanguageTranslator {

    private static final Logger log = LoggerFactory.getLogger(SPARQLtoSQLTranslator.class);

    private final JdbcConnection jdbcConnection;

    /**
     * Constructor of the SPARQLtoSQLTranslator
     *
     * @param condensedMode if true, the SQL query build will use the condensed mode
     */
    public SPARQLtoSQLTranslator(boolean condensedMode) {
        super(condensedMode);
        this.jdbcConnection = JdbcConnection.getInstance();
    }

    /**
     * Translate the SPARQL query into a SQL query
     *
     * @return the SQL query
     */
    public ResultSet translateAndExecSelect(Query query) {
        Op op = Algebra.compile(query);

        Long start = System.nanoTime();
        SQLQuery qu = buildSPARQLContext(op)
                .finalizeQuery();

        Long end = System.nanoTime();

        log.info("[Measure] (Query translation duration): {} ns;", end - start);
        log.info("Query result: {};", qu.getSql());

        try (java.sql.ResultSet rs = jdbcConnection.executeSQL(qu.getSql())) {

            // Change the List implementation to the Iterator one (heap space)
            List<Var> vars = new ArrayList<>();
            List<Binding> bindings = new ArrayList<>();

            while (Objects.requireNonNull(rs).next()) {
                ResultSetMetaData rsmd = rs.getMetaData();
                List<String> allVariables = new ArrayList<>();
                List<String> variables = new ArrayList<>();
                int nbColumns = rsmd.getColumnCount();
                for (int i = 1; i <= nbColumns; i++) {
                    String columnName = rsmd.getColumnName(i);
                    allVariables.add(columnName);
                    if (columnName.startsWith("name$")) {
                        variables.add(columnName.substring(5));
                    } else if (!columnName.startsWith("type$")) {
                        variables.add(columnName);
                    }
                }

                BindingBuilder bindingBuilder = Binding.builder();
                for (String v : variables) {
                    Var variable = Var.alloc(v);
                    Node variableValue;

                    if (hasColumn(rs, "name$" + v) && rs.getString("name$" + v) != null) {
                        String value = rs.getString("name$" + v);
                        String valueType;
                        if (allVariables.contains("type$" + v)) {
                            valueType = rs.getString("type$" + v);
                        } else {
                            valueType = getAssociatedRDFType(rsmd.getColumnType(rs.findColumn("name$" + v)));
                        }
                        variableValue = valueType == null ?
                                NodeFactory.createURI(value) : NodeFactory.createLiteral(value, NodeFactory.getType(valueType));
                    } else if (hasColumn(rs, v) && rs.getString(v) != null) {
                        String value = rs.getString(v);
                        String valueType = getAssociatedRDFType(rsmd.getColumnType(rs.findColumn(v)));
                        variableValue = NodeFactory.createLiteral(value, NodeFactory.getType(valueType));
                    } else {
                        variableValue = null;
                    }

                    if (!vars.contains(variable)) {
                        vars.add(variable);
                    }

                    bindingBuilder.add(variable, variableValue);
                }

                bindings.add(bindingBuilder.build());
            }

            return ResultSetStream.create(vars, bindings.iterator());

        } catch (SQLException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    private SQLQuery buildSPARQLContext(Op op) {
        return buildSPARQLContext(op, new SQLContext(null, new HashMap<>(), new ArrayList<>(), condensedMode));
    }

    private SQLQuery buildSPARQLContext(Op op, SQLContext context) {
        return switch (op) {
            case OpJoin opJoin -> new StSJoinOperator(
                    buildSPARQLContext(opJoin.getLeft(), context),
                    buildSPARQLContext(opJoin.getRight(), context)
            ).buildSQLQuery();
            case OpLeftJoin opLeftJoin -> {
                // Jointure avec un/des variable qui sont dans un optional
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
            case OpSlice opSlice -> new StSSliceOperator(opSlice, buildSPARQLContext(opSlice.getSubOp(), context))
                    .buildSQLQuery();
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
            case OpMinus opMinus -> new StSMinusOperator(
                    buildSPARQLContext(opMinus.getLeft(), context),
                    buildSPARQLContext(opMinus.getRight(), context)
            ).buildSQLQuery();
            case OpQuadPattern opQuadPattern ->
                    throw new IllegalArgumentException("TODO: OpQuadPattern not implemented");
            case OpFilter opFilter -> new StSFilterOperator(
                    buildSPARQLContext(opFilter.getSubOp()),
                    opFilter
            ).buildSQLQuery();
            case OpOrder opOrder -> throw new IllegalArgumentException("TODO: OpOrder not implemented");
            case OpTopN opTopN -> throw new IllegalArgumentException("TODO: OpTopN not implemented");
            case OpPath opPath -> throw new IllegalArgumentException("TODO: OpPath not implemented");
            case OpLabel opLabel -> throw new IllegalArgumentException("TODO: OpLabel not implemented");
            case OpList opList -> throw new IllegalArgumentException("TODO: OpList not implemented");
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
        SPARQLContextType sparqlContextType = context.graph() == null ? SPARQLContextType.METADATA : SPARQLContextType.DATASET;

        for (int i = 0; i < opBGP.getPattern().getList().size(); i++) {
            Triple triple = opBGP.getPattern().getList().get(i);
            Node subject = triple.getSubject();
            Node predicate = triple.getPredicate();
            Node object = triple.getObject();

            newVarOccurrences.computeIfAbsent(subject, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(SPARQLPositionType.SUBJECT, i, sparqlContextType));
            newVarOccurrences.computeIfAbsent(predicate, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(SPARQLPositionType.PREDICATE, i, sparqlContextType));
            newVarOccurrences.computeIfAbsent(object, k -> new ArrayList<>())
                    .add(new SPARQLOccurrence(SPARQLPositionType.OBJECT, i, sparqlContextType));

            context = context.setVarOccurrences(newVarOccurrences);
        }

        return context;
    }

    private String getAssociatedRDFType(int columnType) {
        return switch (columnType) {
            case java.sql.Types.INTEGER:
                yield "http://www.w3.org/2001/XMLSchema#integer";
            case java.sql.Types.BIGINT:
                yield "http://www.w3.org/2001/XMLSchema#long";
            case java.sql.Types.SMALLINT:
                yield "http://www.w3.org/2001/XMLSchema#short";
            case java.sql.Types.TINYINT:
                yield "http://www.w3.org/2001/XMLSchema#byte";
            case java.sql.Types.REAL, java.sql.Types.FLOAT:
                yield "http://www.w3.org/2001/XMLSchema#float";
            case java.sql.Types.DOUBLE:
                yield "http://www.w3.org/2001/XMLSchema#double";
            case java.sql.Types.DECIMAL, java.sql.Types.NUMERIC:
                yield "http://www.w3.org/2001/XMLSchema#decimal";
            case java.sql.Types.BOOLEAN:
                yield "http://www.w3.org/2001/XMLSchema#boolean";
            case java.sql.Types.DATE:
                yield "http://www.w3.org/2001/XMLSchema#date";
            case java.sql.Types.TIME:
                yield "http://www.w3.org/2001/XMLSchema#time";
            case java.sql.Types.TIMESTAMP:
                yield "http://www.w3.org/2001/XMLSchema#dateTime";
            case java.sql.Types.CHAR, java.sql.Types.VARCHAR, java.sql.Types.LONGVARCHAR:
                yield "http://www.w3.org/2001/XMLSchema#string";
            case java.sql.Types.BINARY, java.sql.Types.VARBINARY, java.sql.Types.LONGVARBINARY, java.sql.Types.BLOB:
                yield "http://www.w3.org/2001/XMLSchema#hexBinary";
            default:
                yield "";
        };
    }

    public static boolean hasColumn(java.sql.ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equals(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }
}
