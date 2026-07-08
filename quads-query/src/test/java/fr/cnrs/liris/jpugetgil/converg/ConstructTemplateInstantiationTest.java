package fr.cnrs.liris.jpugetgil.converg;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the materialization of a CONSTRUCT template against one SQL solution row
 * ({@link SPARQLtoSQLTranslator#instantiateTemplate}).
 */
class ConstructTemplateInstantiationTest {

    private static final Node GRAPH = Quad.defaultGraphNodeGenerated;
    private static final Node PREDICATE = NodeFactory.createURI("http://example.org/p");

    @Test
    void unboundVariableSkipsOnlyTheAffectedTemplateQuad() throws SQLException {
        List<Quad> template = List.of(
                new Quad(GRAPH, Var.alloc("s"), PREDICATE, Var.alloc("o")),
                new Quad(GRAPH, Var.alloc("s"), PREDICATE, NodeFactory.createURI("http://example.org/o2"))
        );
        Map<String, String> row = new LinkedHashMap<>();
        row.put("name$s", "http://example.org/s");
        row.put("type$s", null);
        row.put("name$o", null);
        row.put("type$o", null);

        List<Quad> quads = SPARQLtoSQLTranslator.instantiateTemplate(template, resultSetOf(row, Map.of()));

        assertEquals(1, quads.size(), "The quad with the unbound ?o must be skipped, the other kept");
        assertTrue(quads.get(0).getSubject().isURI());
        assertEquals("http://example.org/s", quads.get(0).getSubject().getURI());
        assertEquals("http://example.org/o2", quads.get(0).getObject().getURI());
    }

    @Test
    void variableAbsentFromResultSetSkipsTemplateQuad() throws SQLException {
        List<Quad> template = List.of(
                new Quad(GRAPH, Var.alloc("missing"), PREDICATE, NodeFactory.createURI("http://example.org/o"))
        );
        Map<String, String> row = new LinkedHashMap<>();
        row.put("name$s", "http://example.org/s");

        List<Quad> quads = SPARQLtoSQLTranslator.instantiateTemplate(template, resultSetOf(row, Map.of()));

        assertTrue(quads.isEmpty(), "A template variable with no SQL column must not produce a quad");
    }

    @Test
    void typeColumnBuildsTypedLiteral() throws SQLException {
        List<Quad> template = List.of(
                new Quad(GRAPH, NodeFactory.createURI("http://example.org/s"), PREDICATE, Var.alloc("o"))
        );
        Map<String, String> row = new LinkedHashMap<>();
        row.put("name$o", "42");
        row.put("type$o", XSDDatatype.XSDinteger.getURI());

        List<Quad> quads = SPARQLtoSQLTranslator.instantiateTemplate(template, resultSetOf(row, Map.of()));

        assertEquals(1, quads.size());
        Node object = quads.get(0).getObject();
        assertTrue(object.isLiteral());
        assertEquals("42", object.getLiteralLexicalForm());
        assertEquals(XSDDatatype.XSDinteger.getURI(), object.getLiteralDatatypeURI());
    }

    @Test
    void valueWithoutTypeColumnFallsBackToSqlColumnType() throws SQLException {
        List<Quad> template = List.of(
                new Quad(GRAPH, NodeFactory.createURI("http://example.org/s"), PREDICATE, Var.alloc("count"))
        );
        Map<String, String> row = new LinkedHashMap<>();
        row.put("name$count", "42");

        List<Quad> quads = SPARQLtoSQLTranslator.instantiateTemplate(
                template, resultSetOf(row, Map.of("name$count", Types.BIGINT)));

        assertEquals(1, quads.size());
        Node object = quads.get(0).getObject();
        assertTrue(object.isLiteral(), "An aggregated value must become a literal, not a URI: " + object);
        assertEquals(XSDDatatype.XSDlong.getURI(), object.getLiteralDatatypeURI());
    }

    @Test
    void literalInSubjectPositionSkipsTemplateQuad() throws SQLException {
        List<Quad> template = List.of(
                new Quad(GRAPH, Var.alloc("s"), PREDICATE, NodeFactory.createURI("http://example.org/o"))
        );
        Map<String, String> row = new LinkedHashMap<>();
        row.put("name$s", "42");
        row.put("type$s", XSDDatatype.XSDinteger.getURI());

        List<Quad> quads = SPARQLtoSQLTranslator.instantiateTemplate(template, resultSetOf(row, Map.of()));

        assertTrue(quads.isEmpty(), "A literal subject is not legal RDF and must be skipped");
    }

    @Test
    void blankNodesAreSharedWithinASolutionAndFreshAcrossSolutions() throws SQLException {
        Node templateBNode = NodeFactory.createBlankNode("template");
        List<Quad> template = List.of(
                new Quad(GRAPH, templateBNode, PREDICATE, Var.alloc("o")),
                new Quad(GRAPH, templateBNode, NodeFactory.createURI("http://example.org/p2"), Var.alloc("o"))
        );
        Map<String, String> row = new LinkedHashMap<>();
        row.put("name$o", "http://example.org/o");
        row.put("type$o", null);

        List<Quad> firstSolution = SPARQLtoSQLTranslator.instantiateTemplate(template, resultSetOf(row, Map.of()));
        List<Quad> secondSolution = SPARQLtoSQLTranslator.instantiateTemplate(template, resultSetOf(row, Map.of()));

        assertEquals(2, firstSolution.size());
        assertTrue(firstSolution.get(0).getSubject().isBlank());
        assertEquals(firstSolution.get(0).getSubject(), firstSolution.get(1).getSubject(),
                "The same template blank node must map to one node within a solution");
        assertNotEquals(templateBNode, firstSolution.get(0).getSubject(),
                "The template blank node itself must not be emitted");
        assertNotEquals(firstSolution.get(0).getSubject(), secondSolution.get(0).getSubject(),
                "Each solution must instantiate fresh blank nodes");
    }

    /**
     * Minimal read-only {@link ResultSet} stub positioned on a single row, backed by
     * the given column-name → value map ({@code sqlTypes} maps column names to
     * {@link Types} constants, defaulting to VARCHAR).
     */
    private static ResultSet resultSetOf(Map<String, String> row, Map<String, Integer> sqlTypes) {
        List<String> columns = new ArrayList<>(row.keySet());

        ResultSetMetaData metaData = (ResultSetMetaData) Proxy.newProxyInstance(
                ResultSetMetaData.class.getClassLoader(),
                new Class<?>[]{ResultSetMetaData.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getColumnCount" -> columns.size();
                    case "getColumnType" -> sqlTypes.getOrDefault(columns.get((int) args[0] - 1), Types.VARCHAR);
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findColumn" -> {
                        int index = columns.indexOf((String) args[0]);
                        if (index < 0) {
                            throw new SQLException("Column not found: " + args[0]);
                        }
                        yield index + 1;
                    }
                    case "getString" -> row.get((String) args[0]);
                    case "getMetaData" -> metaData;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
