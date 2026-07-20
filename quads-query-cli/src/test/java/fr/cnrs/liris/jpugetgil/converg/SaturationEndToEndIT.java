package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.connection.JdbcConnection;
import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRegime;
import fr.cnrs.liris.jpugetgil.converg.inference.InferenceConfig;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.postgresql.ds.PGSimpleDataSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Results-level, database-backed proof that query-time saturation makes inferred
 * triples visible to <em>every</em> query shape — the defect the old query-rewriting
 * approach had (Q2/Q3 missed the inferred type).
 * <p>
 * Opt-in: needs a reachable, empty PostgreSQL. Enable with
 * {@code CONVERG_SATURATION_IT=true} and (optionally) {@code CONVERG_IT_JDBC}.
 * The dataset is the README example: {@code :M1 a :House} with
 * {@code :House rdfs:subClassOf :Building}, in one graph across two versions,
 * extended with a longer schema so that <em>nested</em> (chained) rule
 * applications can be exercised:
 * <ul>
 *   <li>{@code :Building rdfs:subClassOf :Structure} — a two-hop
 *       {@code subClassOf} chain, so a single asserted type must fan out to two
 *       inferred super-types (nested rdfs9 over the recursive TBox closure);</li>
 *   <li>{@code :locatedIn rdfs:domain :House} with {@code :M2 :locatedIn :Lyon}
 *       — a cross-rule chain where rdfs2 (domain) derives a type that rdfs9 then
 *       propagates up the whole {@code subClassOf} chain.</li>
 * </ul>
 */
@EnabledIfEnvironmentVariable(named = "CONVERG_SATURATION_IT", matches = "true")
class SaturationEndToEndIT {

    private static final String JDBC = System.getenv().getOrDefault(
            "CONVERG_IT_JDBC", "jdbc:postgresql://localhost:55432/converg_it");

    @BeforeAll
    static void seed() throws Exception {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(JDBC);
        ds.setUser(System.getenv().getOrDefault("CONVERG_IT_USER", "postgres"));
        ds.setPassword(System.getenv().getOrDefault("CONVERG_IT_PASSWORD", "password"));

        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            // Full ConVer-G schema (statements are separated by '^;')
            String schema = Files.readString(Path.of("../quads-loader/src/main/resources/schema.sql"));
            for (String stmt : schema.split("\\^;")) {
                if (!stmt.isBlank()) {
                    st.execute(stmt);
                }
            }
            // The metadata trigger is irrelevant to these GRAPH queries; skip it while seeding
            st.execute("ALTER TABLE versioned_named_graph DISABLE TRIGGER trg_insert_metadata_vng");

            st.execute("TRUNCATE versioned_quad, versioned_named_graph, resource_or_literal, version RESTART IDENTITY CASCADE");

            // Vocabulary + data terms (URIs -> type NULL)
            for (String uri : List.of(
                    "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                    "http://www.w3.org/2000/01/rdf-schema#subClassOf",
                    "http://www.w3.org/2000/01/rdf-schema#domain",
                    "http://ex/House", "http://ex/Building", "http://ex/Structure",
                    "http://ex/locatedIn",
                    "http://ex/M1", "http://ex/M2", "http://ex/Lyon", "http://ex/g1",
                    "http://ex/vng1", "http://ex/vng2")) {
                st.execute("INSERT INTO resource_or_literal (name, type) VALUES ('" + uri + "', NULL)");
            }
            st.execute("INSERT INTO version (message) VALUES ('v1'), ('v2')");

            // One named graph g1, two versions -> two versioned-named-graphs
            st.execute(vng("http://ex/vng1", "http://ex/g1", 1));
            st.execute(vng("http://ex/vng2", "http://ex/g1", 2));

            // Data (condensed): all quads valid in versions {v1, v2} -> validity B'11'
            st.execute(quad("http://ex/M1", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
                    "http://ex/House", "http://ex/g1", "11"));
            // TBox: a two-hop subClassOf chain House -> Building -> Structure
            st.execute(quad("http://ex/House", "http://www.w3.org/2000/01/rdf-schema#subClassOf",
                    "http://ex/Building", "http://ex/g1", "11"));
            st.execute(quad("http://ex/Building", "http://www.w3.org/2000/01/rdf-schema#subClassOf",
                    "http://ex/Structure", "http://ex/g1", "11"));
            // Cross-rule chain: :locatedIn has domain :House, and :M2 :locatedIn :Lyon
            st.execute(quad("http://ex/locatedIn", "http://www.w3.org/2000/01/rdf-schema#domain",
                    "http://ex/House", "http://ex/g1", "11"));
            st.execute(quad("http://ex/M2", "http://ex/locatedIn",
                    "http://ex/Lyon", "http://ex/g1", "11"));
        }

        JdbcConnection.setDataSource(ds);
    }

    @Test
    void inferredTypeIsVisibleToOpenTypeQuery() {
        // Q2: ?b a ?type  -> must now include the inferred Building, not only House
        Set<String> types = new HashSet<>();
        ResultSet rs = select("SELECT * WHERE { GRAPH ?g { ?b a ?type } }");
        while (rs.hasNext()) {
            QuerySolution s = rs.next();
            types.add(s.get("type").toString());
        }
        assertTrue(types.contains("http://ex/House"), "explicit type must be present");
        assertTrue(types.contains("http://ex/Building"),
                "INFERRED type Building must be visible to ?b a ?type (was missing before)");
    }

    @Test
    void inferredTripleIsVisibleToOpenTriplePattern() {
        // Q3: ?b ?p ?o  -> must include the inferred (M1, rdf:type, Building)
        Set<String> triples = new HashSet<>();
        ResultSet rs = select("SELECT * WHERE { GRAPH ?g { ?b ?p ?o } }");
        while (rs.hasNext()) {
            QuerySolution s = rs.next();
            triples.add(s.get("b") + " " + s.get("p") + " " + s.get("o"));
        }
        assertTrue(triples.contains(
                        "http://ex/M1 http://www.w3.org/1999/02/22-rdf-syntax-ns#type http://ex/Building"),
                "INFERRED triple M1 a Building must be visible to ?b ?p ?o (was missing before). Got: " + triples);
    }

    @Test
    void goalDirectedQueryStillWorks() {
        // Q1: ?b a Building  -> M1
        Set<String> subjects = new HashSet<>();
        ResultSet rs = select("SELECT * WHERE { GRAPH ?g { ?b a <http://ex/Building> } }");
        while (rs.hasNext()) {
            subjects.add(rs.next().get("b").toString());
        }
        assertTrue(subjects.contains("http://ex/M1"), "M1 should be a Building by inference");
    }

    @Test
    void nestedRuleApplicationsFanTypeUpTheWholeSubclassChain() {
        // :M1 a :House, with :House ⊑ :Building ⊑ :Structure.
        // rdfs9 must be applied along the *whole* transitive closure, not one hop:
        // deriving :Building requires one application, :Structure a second, nested one.
        Set<String> types = typesOf("http://ex/M1");
        assertTrue(types.contains("http://ex/House"), "explicit type present");
        assertTrue(types.contains("http://ex/Building"), "first-hop inferred type present");
        assertTrue(types.contains("http://ex/Structure"),
                "NESTED type Structure (two subClassOf hops) must be derived. Got: " + types);
    }

    @Test
    void oneRuleFeedsAnotherAcrossRuleTypes() {
        // :M2 has no asserted type. The chain is:
        //   rdfs2 (:locatedIn domain :House)      -> :M2 a :House
        //   rdfs9 (:House ⊑ :Building)            -> :M2 a :Building
        //   rdfs9 (:Building ⊑ :Structure)        -> :M2 a :Structure
        // i.e. a domain-derived type must itself feed the subClassOf propagation.
        Set<String> types = typesOf("http://ex/M2");
        assertTrue(types.contains("http://ex/House"),
                "rdfs2 (domain) must derive the base type. Got: " + types);
        assertTrue(types.contains("http://ex/Building"),
                "rdfs9 must fire on the rdfs2-derived type (nested rules). Got: " + types);
        assertTrue(types.contains("http://ex/Structure"),
                "the domain-derived type must fan up the whole subClassOf chain. Got: " + types);
    }

    @Test
    void inferenceOffReturnsOnlyExplicitFacts() {
        // Same open query, inference disabled -> Building must NOT appear
        Set<String> types = new HashSet<>();
        ResultSet rs = new SPARQLtoSQLTranslator(true, InferenceConfig.NONE, List.of())
                .translateAndExecSelect(QueryFactory.create("SELECT * WHERE { GRAPH ?g { ?b a ?type } }"));
        while (rs.hasNext()) {
            types.add(rs.next().get("type").toString());
        }
        assertTrue(types.contains("http://ex/House"), "explicit type present");
        assertFalse(types.contains("http://ex/Building"), "with inference OFF, the inferred Building must not appear");
    }

    private static ResultSet select(String sparql) {
        SPARQLtoSQLTranslator translator =
                new SPARQLtoSQLTranslator(true, new InferenceConfig(EntailmentRegime.RDFS, false), List.of());
        Query query = QueryFactory.create(sparql);
        return translator.translateAndExecSelect(query);
    }

    /** The set of {@code rdf:type} values (explicit + inferred) of {@code subjectUri}. */
    private static Set<String> typesOf(String subjectUri) {
        Set<String> types = new HashSet<>();
        ResultSet rs = select("SELECT ?type WHERE { GRAPH ?g { <" + subjectUri + "> a ?type } }");
        while (rs.hasNext()) {
            types.add(rs.next().get("type").toString());
        }
        return types;
    }

    private static String id(String uri) {
        return "(SELECT id_resource_or_literal FROM resource_or_literal WHERE name = '" + uri + "')";
    }

    private static String vng(String vngUri, String ngUri, int version) {
        return "INSERT INTO versioned_named_graph (id_versioned_named_graph, id_named_graph, index_version) VALUES ("
                + id(vngUri) + ", " + id(ngUri) + ", " + version + ")";
    }

    private static String quad(String s, String p, String o, String g, String validity) {
        return "INSERT INTO versioned_quad (id_subject, id_predicate, id_object, id_named_graph, validity) VALUES ("
                + id(s) + ", " + id(p) + ", " + id(o) + ", " + id(g) + ", B'" + validity + "')";
    }
}
