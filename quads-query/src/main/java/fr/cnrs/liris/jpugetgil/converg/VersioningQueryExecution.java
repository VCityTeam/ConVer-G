package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.connection.JdbcConnection;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class VersioningQueryExecution implements QueryExecution {

    private static final Logger log = LoggerFactory.getLogger(VersioningQueryExecution.class);

    private final Query query;

    private final JdbcConnection jdbcConnection;

    public VersioningQueryExecution(Query query) {
        this.query = query;
        this.jdbcConnection = JdbcConnection.getInstance();
    }

    @Override
    public Dataset getDataset() {
        return null;
    }

    @Override
    public Context getContext() {
        return Context.emptyContext();
    }

    @Override
    public Query getQuery() {
        return query;
    }

    @Override
    public String getQueryString() {
        return query.toString();
    }

    @Override
    public org.apache.jena.query.ResultSet execSelect() {
        SPARQLtoSQLTranslator translator = new SPARQLtoSQLTranslator();

        Long start = System.nanoTime();
        String sqlQuery = translator.translate(query);
        Long end = System.nanoTime();

        log.info("[Measure] (Query translation duration): {} ns;", end - start);

        try (ResultSet rs = jdbcConnection.executeSQL(sqlQuery)) {

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

    @Override
    public Model execConstruct() {
        return null;
    }

    @Override
    public Model execConstruct(Model model) {
        return null;
    }

    @Override
    public Iterator<Triple> execConstructTriples() {
        return null;
    }

    @Override
    public Iterator<Quad> execConstructQuads() {
        return null;
    }

    @Override
    public Dataset execConstructDataset() {
        return null;
    }

    @Override
    public Dataset execConstructDataset(Dataset dataset) {
        return null;
    }

    @Override
    public Model execDescribe() {
        return null;
    }

    @Override
    public Model execDescribe(Model model) {
        return null;
    }

    @Override
    public Iterator<Triple> execDescribeTriples() {
        return null;
    }

    @Override
    public boolean execAsk() {
        return false;
    }

    @Override
    public JsonArray execJson() {
        return new JsonArray();
    }

    @Override
    public Iterator<JsonObject> execJsonItems() {
        return null;
    }

    @Override
    public void abort() {
        // Override engine execution, ignoring this method
    }

    @Override
    public void close() {
        // Override engine execution, ignoring this method
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public long getTimeout1() {
        return 0;
    }

    @Override
    public long getTimeout2() {
        return 0;
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

    public static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
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
