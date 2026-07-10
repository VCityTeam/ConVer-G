package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRegime;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.converg.sql.operator.FinalizeSQLOperator;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.ARQNotImplemented;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SPARQLtoSQLTranslatorTest {
    SPARQLtoSQLTranslator condensedSPARQLtoSQLTranslator = new SPARQLtoSQLTranslator(true, EntailmentRegime.NONE);
    SPARQLtoSQLTranslator flatSPARQLtoSQLTranslator = new SPARQLtoSQLTranslator(false, EntailmentRegime.NONE);

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
     * Two SHARED condensed graph variables between the left and the OPTIONAL force an
     * n = 2 Product-Set Difference. Per Lemma 5.1 the difference must be the disjoint
     * union of slices, not the coordinate-wise product (A1\B1)x(A2\B2) which undercounts
     * for n >= 2 (Example 5.9). A "blocked" factor (A_j & B_j, rendered as "& bit_or("
     * without negation) appears only in the correct multi-slice decomposition.
     */
    @Test
    void optionalTwoSharedGraphVariablesUsesLemma51Slicing() {
        Query query = QueryFactory.create(
                "PREFIX schema: <http://schema.org/> " +
                        "SELECT * WHERE { " +
                        "  GRAPH ?g1 { ?s schema:height ?o1 } " +
                        "  GRAPH ?g2 { ?s schema:width ?o2 } " +
                        "  OPTIONAL { GRAPH ?g1 { ?s schema:depth ?o3 } GRAPH ?g2 { ?s schema:alpha ?o4 } } }");
        SQLQuery sqlQuery = getSqlQuery(query, condensedSPARQLtoSQLTranslator);
        String sql = sqlQuery.getSql();

        assertNotNull(sql);
        assertTrue(sql.contains("& ~bit_or("),
                "Expected a version-set difference factor (A_i \\ B_i): " + sql);
        assertTrue(sql.contains("& bit_or("),
                "Expected a blocked factor (A_j & B_j) from Lemma 5.1 slicing, absent in the "
                        + "coordinate-wise difference: " + sql);
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

    /**
     * A computed projection over bound variables - {@code (?a - ?b AS ?diff)} - must
     * reference the value columns ({@code v$a} / {@code v$b}), not the bare SPARQL names,
     * and must first materialise the operands to their actual values. Regression test for
     * the {@code column "a" does not exist} failure (renaming / computed SELECT expression).
     */
    @Test
    void computedProjectionOverBoundVariablesReferencesValueColumns() {
        Query query = QueryFactory.create(
                "PREFIX ex: <http://example.org/> " +
                        "SELECT ?s (?a - ?b AS ?diff) WHERE { GRAPH ?g { " +
                        "?s ex:a ?a ; ex:b ?b . } }");
        SQLQuery sqlQuery = getSqlQuery(query, condensedSPARQLtoSQLTranslator);
        String sql = sqlQuery.getSql();

        assertNotNull(sql);
        assertTrue(sql.contains("(v$a::float-v$b::float)"),
                "The computed binding must reference the value columns: " + sql);
        assertFalse(sql.contains("(a::float"),
                "The computed binding must not reference a bare variable name: " + sql);
        assertTrue(sql.contains("rl.numeric_value as num$a"),
                "The operands must be identified to their actual values: " + sql);
    }

    /**
     * Renaming an aggregate result - {@code (AVG(...) AS ?avg)} - still references the
     * GROUP BY column alias ({@code aggN}) and must not be rewritten to a (non-existent)
     * value column. Guards the aggregate branch of the computed-projection fix.
     */
    @Test
    void aggregateRenameStillReferencesAggregateColumnAlias() {
        Query query = QueryFactory.create(
                "PREFIX ex: <http://example.org/> " +
                        "SELECT ?s (AVG(?o) AS ?avg) WHERE { GRAPH ?g { ?s ex:p ?o } } GROUP BY ?s");
        SQLQuery sqlQuery = getSqlQuery(query, condensedSPARQLtoSQLTranslator);
        String sql = sqlQuery.getSql();

        assertNotNull(sql);
        assertTrue(sql.contains("agg0 AS v$avg"),
                "The aggregate rename must reference the aggregate column alias: " + sql);
    }

    /**
     * FILTER comparing two CONDENSED graph variables with {@code !=} must flatten them to a
     * concrete {@code id_versioned_named_graph} so the WHERE clause has real {@code v$}
     * columns to compare. Regression test for {@code column "v$g2" does not exist}
     * (change-representation-twice).
     */
    @Test
    void inequalityFilterOnCondensedGraphVariablesFlattensThem() {
        Query query = QueryFactory.create(
                "PREFIX ex: <http://example.org/> " +
                        "SELECT ?s1 ?s2 WHERE { " +
                        "  GRAPH ?g1 { ?s1 ex:p ?o1 } " +
                        "  GRAPH ?g2 { ?s2 ex:p ?o2 } " +
                        "  FILTER(?g1 != ?g2) }");
        SQLQuery sqlQuery = getSqlQuery(query, condensedSPARQLtoSQLTranslator);
        String sql = sqlQuery.getSql();

        assertNotNull(sql);
        assertTrue(sql.contains("id_versioned_named_graph AS v$g1"),
                "Graph variable ?g1 must be flattened to a concrete v$ column: " + sql);
        assertTrue(sql.contains("id_versioned_named_graph AS v$g2"),
                "Graph variable ?g2 must be flattened to a concrete v$ column: " + sql);
        assertTrue(sql.contains("v$g1!=v$g2") || sql.contains("v$g2!=v$g1"),
                "The inequality must compare the flattened value columns: " + sql);
    }

    /**
     * A one-or-more property path must translate to a recursive CTE anchored on
     * the concrete start node, in both storage layouts.
     */
    @Test
    void oneOrMorePathGeneratesRecursiveClosure() {
        Query query = QueryFactory.create(
                "PREFIX ex: <http://example.edu/> " +
                        "SELECT ?g ?x WHERE { GRAPH ?g { <http://example.edu/Building#1> ex:adjacentTo+ ?x } }");

        String condensedSql = getSqlQuery(query, condensedSPARQLtoSQLTranslator).getSql();
        String flatSql = getSqlQuery(query, flatSPARQLtoSQLTranslator).getSql();

        assertTrue(condensedSql.contains("WITH RECURSIVE"),
                "A + path must generate a recursive CTE: " + condensedSql);
        assertTrue(condensedSql.contains("'http://example.edu/Building#1'"),
                "The closure must be anchored on the concrete start: " + condensedSql);
        assertTrue(flatSql.contains("WITH RECURSIVE"),
                "A + path must generate a recursive CTE in flat mode too: " + flatSql);
    }

    /**
     * With a bound end and an unbound start, the closure must explore the chain
     * backward from the end; filtering the base case on the end would lose
     * multi-hop chains (regression test for the anchoring direction).
     */
    @Test
    void oneOrMorePathWithBoundEndExploresChainBackward() {
        Query query = QueryFactory.create(
                "PREFIX ex: <http://example.edu/> " +
                        "SELECT ?g ?x WHERE { GRAPH ?g { ?x ex:adjacentTo+ <http://example.edu/Building#3> } }");
        String sql = getSqlQuery(query, condensedSPARQLtoSQLTranslator).getSql();

        assertTrue(sql.contains("ON c.id_start = t1.id_object"),
                "A bound-end closure must recurse backward through the subjects: " + sql);
    }

    /**
     * A zero-or-more path adds a zero-length arm (the start node itself, valid in
     * every version where the graph exists) and collapses duplicate chains.
     */
    @Test
    void zeroOrMorePathAddsZeroLengthArm() {
        Query query = QueryFactory.create(
                "PREFIX ex: <http://example.edu/> " +
                        "SELECT ?g ?x WHERE { GRAPH ?g { <http://example.edu/Building#1> ex:adjacentTo* ?x } }");
        String sql = getSqlQuery(query, condensedSPARQLtoSQLTranslator).getSql();

        assertTrue(sql.contains("WITH RECURSIVE"), "A * path must generate a recursive CTE: " + sql);
        assertTrue(sql.contains("bit_or"),
                "The zero-length arm and chain collapsing rely on bit_or aggregation: " + sql);
    }

    /**
     * The zero-length component of a * path binds both endpoints to the same
     * term, which cannot be enumerated when both are variables.
     */
    @Test
    void zeroOrMorePathWithTwoVariablesIsRejected() {
        Query query = QueryFactory.create(
                "PREFIX ex: <http://example.edu/> " +
                        "SELECT ?g ?s ?x WHERE { GRAPH ?g { ?s ex:adjacentTo* ?x } }");

        assertThrows(ARQNotImplemented.class, () -> getSqlQuery(query, condensedSPARQLtoSQLTranslator));
    }

    /**
     * A zero-or-one path is the union of the zero-length binding and the single
     * hop.
     */
    @Test
    void zeroOrOnePathUnionsZeroLengthBinding() {
        Query query = QueryFactory.create(
                "PREFIX ex: <http://example.edu/> " +
                        "SELECT ?g ?x WHERE { GRAPH ?g { <http://example.edu/Building#1> ex:adjacentTo? ?x } }");
        String sql = getSqlQuery(query, condensedSPARQLtoSQLTranslator).getSql();

        assertTrue(sql.contains("UNION"), "A ? path must union the zero and one hop cases: " + sql);
        assertTrue(sql.contains("'http://example.edu/Building#1'"),
                "The zero-length arm must bind the variable to the concrete term: " + sql);
    }

    /**
     * A sequence path joins its two steps through a generated intermediate
     * variable.
     */
    @Test
    void sequencePathJoinsThroughFreshVariable() {
        Query query = QueryFactory.create(
                "PREFIX ex: <http://example.edu/> " +
                        "SELECT ?g ?x WHERE { GRAPH ?g { <http://example.edu/Building#1> (ex:adjacentTo/ex:adjacentTo) ?x } }");
        String sql = getSqlQuery(query, condensedSPARQLtoSQLTranslator).getSql();

        assertTrue(sql.contains("__path_p"),
                "A sequence path must join through a fresh intermediate variable: " + sql);
    }

    /**
     * An alternation path is a union of its branches.
     */
    @Test
    void alternationPathTranslatesToUnion() {
        Query query = QueryFactory.create(
                "PREFIX schema: <http://schema.org/> " +
                        "SELECT ?g ?s ?v WHERE { GRAPH ?g { ?s (schema:height|schema:width) ?v } }");
        String sql = getSqlQuery(query, condensedSPARQLtoSQLTranslator).getSql();

        assertTrue(sql.contains("UNION"), "An alternation path must translate to a UNION: " + sql);
        assertTrue(sql.contains("'http://schema.org/height'") && sql.contains("'http://schema.org/width'"),
                "Both branches must be present: " + sql);
    }

    /**
     * An inverse path swaps subject and object of the underlying pattern.
     */
    @Test
    void inversePathSwapsSubjectAndObject() {
        Query query = QueryFactory.create(
                "PREFIX ex: <http://example.edu/> " +
                        "SELECT ?g ?o WHERE { GRAPH ?g { ?o ^ex:adjacentTo <http://example.edu/Building#2> } }");
        String sql = getSqlQuery(query, condensedSPARQLtoSQLTranslator).getSql();

        assertNotNull(sql);
        assertTrue(sql.contains("id_subject") && sql.contains("'http://example.edu/Building#2'"),
                "The inverse path must place the concrete node in subject position: " + sql);
    }

    /**
     * A property path over the default graph runs on the metadata table.
     */
    @Test
    void metadataPathUsesMetadataTable() {
        Query query = QueryFactory.create(
                "PREFIX prov: <http://www.w3.org/ns/prov#> " +
                        "SELECT ?vng ?ng WHERE { ?vng prov:specializationOf+ ?ng }");
        String sql = getSqlQuery(query, condensedSPARQLtoSQLTranslator).getSql();

        assertTrue(sql.contains("FROM metadata"),
                "A default-graph path must recurse over the metadata table: " + sql);
        assertTrue(sql.contains("WITH RECURSIVE"), "It must still be a recursive closure: " + sql);
    }

    /**
     * LET(?v := expr) (OpAssign) is translated like BIND for a fresh variable.
     */
    @Test
    void letAssignmentProjectsComputedVariable() {
        Query query = QueryFactory.create(
                "PREFIX schema: <http://schema.org/> " +
                        "SELECT ?g ?h ?d WHERE { GRAPH ?g { <http://example.edu/Building#1> schema:height ?h } LET(?d := ?h + 1) }");
        String sql = getSqlQuery(query, condensedSPARQLtoSQLTranslator).getSql();

        assertNotNull(sql);
        assertTrue(sql.contains("$d"), "The assigned variable must be projected: " + sql);
    }

    /**
     * The optimizer rewrites ORDER BY + LIMIT into OpTopN; it must translate to
     * the same SQL as the unoptimized slice over an order.
     */
    @Test
    void topNTranslatesToOrderAndLimit() {
        Query query = QueryFactory.create(
                "PREFIX schema: <http://schema.org/> " +
                        "SELECT ?h WHERE { GRAPH ?g { ?s schema:height ?h } } ORDER BY DESC(?h) LIMIT 3");
        Op quadOp = Algebra.toQuadForm(Algebra.optimize(Algebra.compile(query)));
        assertTrue(quadOp.toString().contains("(top"),
                "Expected the optimizer to produce an OpTopN: " + quadOp);

        SQLQuery finalized = new FinalizeSQLOperator(condensedSPARQLtoSQLTranslator.buildSPARQLContext(quadOp))
                .buildSQLQuery();
        String sql = finalized.getSql();

        assertTrue(sql.contains("ORDER BY"), "TopN must order the results: " + sql);
        assertTrue(sql.contains("LIMIT 3"), "TopN must limit the results: " + sql);
    }

    /**
     * A LATERAL right-hand side without slice or grouping behaves like a join.
     */
    @Test
    void lateralWithoutModifiersTranslatesAsJoin() {
        Query query = QueryFactory.create(
                "PREFIX schema: <http://schema.org/> " +
                        "SELECT * WHERE { GRAPH ?g { ?s schema:height ?h } LATERAL { GRAPH ?g2 { ?s schema:width ?w } } }");
        String sql = getSqlQuery(query, condensedSPARQLtoSQLTranslator).getSql();

        assertNotNull(sql);
        assertTrue(sql.contains("'http://schema.org/width'"),
                "The lateral right-hand side must be joined in: " + sql);
    }

    /**
     * A LATERAL right-hand side with a LIMIT applies once per left binding; that
     * cannot be expressed as a global join and must be rejected.
     */
    @Test
    void lateralWithLimitIsRejected() {
        Query query = QueryFactory.create(
                "PREFIX schema: <http://schema.org/> " +
                        "SELECT * WHERE { GRAPH ?g { ?s schema:height ?h } " +
                        "LATERAL { { SELECT ?s ?w WHERE { GRAPH ?g2 { ?s schema:width ?w } } LIMIT 1 } } }");

        assertThrows(ARQNotImplemented.class, () -> getSqlQuery(query, condensedSPARQLtoSQLTranslator));
    }

    /**
     * OpLabel is a transparent annotation: the labelled operator must translate
     * exactly like its sub-operator.
     */
    @Test
    void labelUnwrapsToSubOperator() {
        Query query = QueryFactory.create("SELECT ?g ?s WHERE { GRAPH ?g { ?s ?p ?o } }");
        Op quadOp = Algebra.toQuadForm(Algebra.compile(query));

        String unwrapped = condensedSPARQLtoSQLTranslator.buildSPARQLContext(quadOp).getSql();
        String labelled = condensedSPARQLtoSQLTranslator.buildSPARQLContext(OpLabel.create("note", quadOp)).getSql();

        assertEquals(unwrapped, labelled);
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

        return condensedSPARQLtoSQLTranslator.buildSPARQLContext(quadOp);
    }
}