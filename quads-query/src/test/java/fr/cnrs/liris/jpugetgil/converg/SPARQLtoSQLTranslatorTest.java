package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SPARQLtoSQLTranslatorTest {
    SPARQLtoSQLTranslator condensedSPARQLtoSQLTranslator = new SPARQLtoSQLTranslator(true);
    SPARQLtoSQLTranslator flatSPARQLtoSQLTranslator = new SPARQLtoSQLTranslator(false);

    @Test
    void testBuildSPARQLContextCondensed() {
        Query query = QueryFactory.create("SELECT ?g ?s ?p ?o WHERE { GRAPH ?g { ?s ?p ?o } }");
        SQLQuery sqlQuery = getSqlQuery(query, condensedSPARQLtoSQLTranslator);
        assertNotNull(sqlQuery.getSql());
    }

    @Test
    void testBuildSPARQLContextFlat() {
        Query query = QueryFactory.create("SELECT ?g ?s ?p ?o WHERE { GRAPH ?g { ?s ?p ?o } }");
        SQLQuery sqlQuery = getSqlQuery(query, flatSPARQLtoSQLTranslator);
        assertNotNull(sqlQuery.getSql());
    }

    /**
     * A numeric comparison filter must read the native {@code numeric_value} column
     * (projected as {@code num$<var>}) instead of casting the textual value with
     * {@code ::float}. Guards the native-type optimisation on every comparison
     * operator that was switched over (&gt;, &lt;, &gt;=, &lt;=).
     */
    @ParameterizedTest
    @ValueSource(strings = {">", "<", ">=", "<="})
    void numericComparisonFilterUsesNativeColumnInsteadOfCast(String operator) {
        Query query = QueryFactory.create(
                "SELECT ?graph ?predicate ?object WHERE { GRAPH ?graph { " +
                        "<http://example.edu/Building#1> ?predicate ?object . " +
                        "FILTER(?object " + operator + " 10) } }");
        SQLQuery sqlQuery = getSqlQuery(query, condensedSPARQLtoSQLTranslator);

        assertNotNull(sqlQuery.getSql());
        assertTrue(sqlQuery.getSql().contains("num$"),
                "Expected the filter to reference the native num$ column: " + sqlQuery.getSql());
        assertFalse(sqlQuery.getSql().contains("::float"),
                "Expected no ::float cast in a native numeric comparison: " + sqlQuery.getSql());
    }

    /**
     * Arithmetic inside a filter must compose the native numeric columns without
     * {@code ::float} casts (Add / Subtract / Multiply / Divide were switched over).
     */
    @Test
    void arithmeticFilterUsesNativeColumnInsteadOfCast() {
        Query query = QueryFactory.create(
                "SELECT ?graph ?predicate ?object WHERE { GRAPH ?graph { " +
                        "<http://example.edu/Building#1> ?predicate ?object . " +
                        "FILTER(?object + 2 > 10) } }");
        SQLQuery sqlQuery = getSqlQuery(query, condensedSPARQLtoSQLTranslator);

        assertNotNull(sqlQuery.getSql());
        assertTrue(sqlQuery.getSql().contains("num$"),
                "Expected arithmetic to reference the native num$ column: " + sqlQuery.getSql());
        assertFalse(sqlQuery.getSql().contains("::float"),
                "Expected no ::float cast in native arithmetic: " + sqlQuery.getSql());
    }

    /**
     * A BIND-computed variable must expose a native numeric sibling
     * ({@code num$<var>}) built with the exception-safe {@code try_cast_numeric},
     * so a later numeric filter on that variable resolves like any stored literal.
     */
    @Test
    void bindProjectsSafeNativeNumericSibling() {
        Query query = QueryFactory.create(
                "SELECT ?graph ?object ?doubled WHERE { GRAPH ?graph { " +
                        "<http://example.edu/Building#1> ?predicate ?object . " +
                        "BIND(?object * 2 AS ?doubled) " +
                        "FILTER(?doubled > 10) } }");
        SQLQuery sqlQuery = getSqlQuery(query, condensedSPARQLtoSQLTranslator);

        assertNotNull(sqlQuery.getSql());
        assertTrue(sqlQuery.getSql().contains("try_cast_numeric"),
                "Expected BIND to project a safe native numeric sibling: " + sqlQuery.getSql());
        assertTrue(sqlQuery.getSql().contains("num$"),
                "Expected a num$ column for the computed variable: " + sqlQuery.getSql());
    }

    private static SQLQuery getSqlQuery(Query query, SPARQLtoSQLTranslator condensedSPARQLtoSQLTranslator) {
        Op op = Algebra.compile(query);

        // Transform the op to a quad form
        Op quadOp = Algebra.toQuadForm(op);

        SQLQuery sqlQuery = condensedSPARQLtoSQLTranslator.buildSPARQLContext(quadOp);
        return sqlQuery;
    }
}