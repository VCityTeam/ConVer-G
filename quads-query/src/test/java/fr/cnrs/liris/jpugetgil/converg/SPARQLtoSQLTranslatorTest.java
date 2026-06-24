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

    /**
     * Baseline OPTIONAL nested inside a single GRAPH block (as in sts-12/23/24/25):
     * it must translate to a LEFT JOIN.
     */
    @Test
    void optionalInsideSingleGraphBuildsLeftJoin() {
        Query query = QueryFactory.create(
                "PREFIX schema: <http://schema.org/> " +
                        "SELECT ?g ?s ?o ?o2 WHERE { GRAPH ?g { " +
                        "?s schema:height ?o . OPTIONAL { ?s schema:width ?o2 . } } }");
        SQLQuery sqlQuery = getSqlQuery(query, condensedSPARQLtoSQLTranslator);

        assertNotNull(sqlQuery.getSql());
        assertTrue(sqlQuery.getSql().contains("LEFT JOIN"),
                "OPTIONAL must translate to a LEFT JOIN: " + sqlQuery.getSql());
    }

    /**
     * Two distinct GRAPH variables connected through a shared subject by an OPTIONAL.
     * This is the case suspected to be buggy. At minimum the translation must build
     * (not throw) and produce a LEFT JOIN that keeps both graph variables
     * (?graph1 mandatory, ?graph2 optional).
     */
    @Test
    void optionalAcrossTwoGraphVariablesCondensedBuildsLeftJoin() {
        SQLQuery sqlQuery = getSqlQuery(twoGraphOptionalQuery(), condensedSPARQLtoSQLTranslator);

        assertNotNull(sqlQuery.getSql());
        assertTrue(sqlQuery.getSql().contains("LEFT JOIN"),
                "OPTIONAL across two graphs must translate to a LEFT JOIN: " + sqlQuery.getSql());
        assertTrue(sqlQuery.getSql().contains("graph1"),
                "The mandatory graph variable must be projected: " + sqlQuery.getSql());
        assertTrue(sqlQuery.getSql().contains("graph2"),
                "The optional graph variable must be projected: " + sqlQuery.getSql());
    }

    /**
     * Same two-graph OPTIONAL but for the flat (non-condensed) model, which takes a
     * different code path (no condensed UNION/difference branch).
     */
    @Test
    void optionalAcrossTwoGraphVariablesFlatBuildsLeftJoin() {
        SQLQuery sqlQuery = getSqlQuery(twoGraphOptionalQuery(), flatSPARQLtoSQLTranslator);

        assertNotNull(sqlQuery.getSql());
        assertTrue(sqlQuery.getSql().contains("LEFT JOIN"),
                "OPTIONAL across two graphs must translate to a LEFT JOIN: " + sqlQuery.getSql());
    }

    /**
     * OPTIONAL carrying a FILTER condition exercises the left-join WHERE/expression
     * path ({@link fr.cnrs.liris.jpugetgil.converg.sql.operator.LeftJoinSQLOperator#buildWhere}).
     */
    @Test
    void optionalWithFilterBuildsLeftJoin() {
        Query query = QueryFactory.create(
                "PREFIX schema: <http://schema.org/> " +
                        "SELECT ?g ?s ?o ?o2 WHERE { GRAPH ?g { " +
                        "?s schema:height ?o . OPTIONAL { ?s schema:width ?o2 . FILTER(?o2 > 3) } } }");
        SQLQuery sqlQuery = getSqlQuery(query, condensedSPARQLtoSQLTranslator);

        assertNotNull(sqlQuery.getSql());
        assertTrue(sqlQuery.getSql().contains("LEFT JOIN"),
                "OPTIONAL with FILTER must translate to a LEFT JOIN: " + sqlQuery.getSql());
    }

    private static Query twoGraphOptionalQuery() {
        return QueryFactory.create(
                "PREFIX schema: <http://schema.org/> " +
                        "SELECT * WHERE { " +
                        "  GRAPH ?graph1 { ?subject schema:height ?height . } " +
                        "  OPTIONAL { GRAPH ?graph2 { ?subject schema:width ?width . } } " +
                        "}");
    }

    private static SQLQuery getSqlQuery(Query query, SPARQLtoSQLTranslator condensedSPARQLtoSQLTranslator) {
        Op op = Algebra.compile(query);

        // Transform the op to a quad form
        Op quadOp = Algebra.toQuadForm(op);

        SQLQuery sqlQuery = condensedSPARQLtoSQLTranslator.buildSPARQLContext(quadOp);
        return sqlQuery;
    }
}