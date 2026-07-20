package fr.cnrs.liris.jpugetgil.converg.inference;

import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRegime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the query-time <em>deductive closure</em> of the versioned quad store as a
 * single {@code WITH RECURSIVE} prefix. Unlike query rewriting (which only rewrites a
 * pattern that names an inferred predicate/class), the closure materialises inferred
 * triples as rows, so every triple pattern — including the open {@code ?s ?p ?o} — sees
 * them.
 * <p>
 * The prefix defines a relation named {@link #INFERRED_RELATION} with the same shape as
 * {@code versioned_quad} ({@code id_subject, id_predicate, id_object, id_named_graph,
 * validity}); {@code QuadPatternSQLOperator} scans it in place of {@code versioned_quad}
 * when inference is active. Version semantics are preserved: an inferred triple's
 * {@code validity} bitstring is the intersection ({@code &}) of its premises' bitstrings,
 * and triples derived through several paths are merged with {@code bit_or}.
 * <p>
 * Structure (each stratum unions the previous one with one round of rule applications):
 * <ol>
 *   <li>{@code inf_sco}/{@code inf_spo} — transitive {@code subClassOf}/{@code subPropertyOf}
 *       closures (the only genuinely recursive parts);</li>
 *   <li>RDFS strata — sub-property propagation, then {@code domain}/{@code range} typing,
 *       then {@code subClassOf} type propagation;</li>
 *   <li>a single SWRL stratum over the RDFS closure.</li>
 * </ol>
 * SWRL rule heads are computed in one pass and not fed back into the closure; chained
 * SWRL rules (a rule head matching another rule's body) are therefore applied to one
 * level only. Only the condensed storage layout is supported.
 */
public class SaturationSqlBuilder {

    private static final Logger log = LoggerFactory.getLogger(SaturationSqlBuilder.class);

    /** The name of the saturated relation exposed to the query translator. */
    public static final String INFERRED_RELATION = "inf_quad";

    private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    private static final String RDFS_SUBCLASS_OF = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
    private static final String RDFS_SUBPROPERTY_OF = "http://www.w3.org/2000/01/rdf-schema#subPropertyOf";
    private static final String RDFS_DOMAIN = "http://www.w3.org/2000/01/rdf-schema#domain";
    private static final String RDFS_RANGE = "http://www.w3.org/2000/01/rdf-schema#range";

    private final boolean condensedMode;
    private final InferenceConfig config;
    private final List<InferenceRule> swrlRules;

    public SaturationSqlBuilder(boolean condensedMode, InferenceConfig config, List<InferenceRule> swrlRules) {
        this.condensedMode = condensedMode;
        this.config = config;
        this.swrlRules = swrlRules == null ? List.of() : swrlRules;
    }

    private boolean rdfsActive() {
        return config.regime() != EntailmentRegime.NONE;
    }

    private boolean swrlActive() {
        return config.swrl() && !swrlRules.isEmpty();
    }

    /**
     * @return true when at least one inference source will produce a closure.
     */
    public boolean isActive() {
        return rdfsActive() || swrlActive();
    }

    /**
     * @return the relation the BGP scan should read: the saturated relation when
     * inference is active, otherwise the base {@code versioned_quad} table.
     */
    public String quadSourceRelation() {
        return isActive() ? INFERRED_RELATION : "versioned_quad";
    }

    /**
     * @return the {@code WITH RECURSIVE ...} prefix (with a trailing newline) to prepend
     * to the final SQL, or an empty string when inference is inactive.
     * @throws IllegalStateException if inference is requested in the (unsupported) flat mode
     */
    public String buildWithPrefix() {
        if (!isActive()) {
            return "";
        }
        if (!condensedMode) {
            throw new IllegalStateException(
                    "Query-time inference is only supported in condensed mode (versioned_quad), not the flat layout");
        }

        List<String> ctes = new ArrayList<>();

        if (rdfsActive()) {
            ctes.add(transitiveClosureCte("inf_sco_c", "inf_sco", RDFS_SUBCLASS_OF));
            ctes.add(transitiveClosureCte("inf_spo_c", "inf_spo", RDFS_SUBPROPERTY_OF));
        }

        // Base stratum: the asserted quads.
        ctes.add("""
                inf_s0 AS (
                  SELECT id_subject, id_predicate, id_object, id_named_graph, validity FROM versioned_quad
                )""");
        String current = "inf_s0";

        if (rdfsActive()) {
            ctes.add(subPropertyStratum("inf_s1", current));
            current = "inf_s1";
            ctes.add(domainRangeStratum("inf_s2", current));
            current = "inf_s2";
            ctes.add(subClassStratum("inf_s3", current));
            current = "inf_s3";
        }

        if (swrlActive()) {
            ctes.add(swrlStratum("inf_s4", current));
            current = "inf_s4";
        }

        // Merge derivations of the same triple: a triple reachable through several
        // rules/paths holds in the union of the paths' version sets.
        ctes.add(INFERRED_RELATION + " AS (\n" +
                "  SELECT id_subject, id_predicate, id_object, id_named_graph, bit_or(validity) AS validity\n" +
                "  FROM " + current + "\n" +
                "  GROUP BY id_subject, id_predicate, id_object, id_named_graph\n" +
                ")");

        log.info("Query-time saturation active (regime: {}, SWRL rules: {})",
                config.regime(), swrlActive() ? swrlRules.size() : 0);

        return "WITH RECURSIVE\n" + String.join(",\n", ctes) + "\n";
    }

    /**
     * A versioned transitive closure of {@code property} over {@code versioned_quad}.
     * The validity is carried as text through the recursion (bit varying is not
     * hashable for the {@code UNION} dedup) and intersected at each hop; chains reaching
     * the same pair are merged with {@code bit_or} in the aggregated CTE.
     */
    private String transitiveClosureCte(String recName, String aggName, String property) {
        String pred = iriId(property);
        return recName + "(id_sub, id_sup, validity) AS (\n" +
                "    SELECT t0.id_subject, t0.id_object, t0.validity::text\n" +
                "    FROM versioned_quad t0\n" +
                "    WHERE t0.id_predicate = " + pred + "\n" +
                "  UNION\n" +
                "    SELECT c.id_sub, t1.id_object, (c.validity::varbit & t1.validity)::text\n" +
                "    FROM " + recName + " c\n" +
                "    JOIN versioned_quad t1 ON c.id_sup = t1.id_subject AND t1.id_predicate = " + pred + "\n" +
                "    WHERE bit_count(c.validity::varbit & t1.validity) <> 0\n" +
                "),\n" +
                aggName + " AS (\n" +
                "  SELECT id_sub, id_sup, bit_or(validity::varbit) AS validity\n" +
                "  FROM " + recName + " GROUP BY id_sub, id_sup\n" +
                ")";
    }

    /**
     * rdfs7: {@code ?s ?q ?o . ?q rdfs:subPropertyOf+ ?p => ?s ?p ?o}.
     */
    private String subPropertyStratum(String name, String source) {
        return name + " AS (\n" +
                "  SELECT * FROM " + source + "\n" +
                "  UNION ALL\n" +
                "  SELECT b.id_subject, sp.id_sup, b.id_object, b.id_named_graph, (b.validity & sp.validity)\n" +
                "  FROM " + source + " b JOIN inf_spo sp ON b.id_predicate = sp.id_sub\n" +
                "  WHERE bit_count(b.validity & sp.validity) <> 0\n" +
                ")";
    }

    /**
     * rdfs2 (domain) and rdfs3 (range): a property used on a subject/object types that
     * subject/object. Declarations are read from the asserted quads ({@code inf_s0}).
     */
    private String domainRangeStratum(String name, String source) {
        String type = iriId(RDF_TYPE);
        return name + " AS (\n" +
                "  SELECT * FROM " + source + "\n" +
                "  UNION ALL\n" +
                "  SELECT b.id_subject, " + type + ", d.id_object, b.id_named_graph, (b.validity & d.validity)\n" +
                "  FROM " + source + " b JOIN inf_s0 d ON b.id_predicate = d.id_subject\n" +
                "  WHERE d.id_predicate = " + iriId(RDFS_DOMAIN) + " AND bit_count(b.validity & d.validity) <> 0\n" +
                "  UNION ALL\n" +
                "  SELECT b.id_object, " + type + ", r.id_object, b.id_named_graph, (b.validity & r.validity)\n" +
                "  FROM " + source + " b JOIN inf_s0 r ON b.id_predicate = r.id_subject\n" +
                "  WHERE r.id_predicate = " + iriId(RDFS_RANGE) + " AND bit_count(b.validity & r.validity) <> 0\n" +
                ")";
    }

    /**
     * rdfs9: {@code ?x rdf:type ?C . ?C rdfs:subClassOf+ ?D => ?x rdf:type ?D}.
     */
    private String subClassStratum(String name, String source) {
        String type = iriId(RDF_TYPE);
        return name + " AS (\n" +
                "  SELECT * FROM " + source + "\n" +
                "  UNION ALL\n" +
                "  SELECT b.id_subject, " + type + ", s.id_sup, b.id_named_graph, (b.validity & s.validity)\n" +
                "  FROM " + source + " b JOIN inf_sco s ON b.id_object = s.id_sub\n" +
                "  WHERE b.id_predicate = " + type + " AND bit_count(b.validity & s.validity) <> 0\n" +
                ")";
    }

    /**
     * One stratum unioning {@code source} with each SWRL rule applied over it.
     */
    private String swrlStratum(String name, String source) {
        StringBuilder sb = new StringBuilder(name + " AS (\n  SELECT * FROM " + source + "\n");
        for (InferenceRule rule : swrlRules) {
            String arm = swrlRuleArm(rule, source);
            if (arm != null) {
                sb.append("  UNION ALL\n").append(arm);
            }
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Compile one SWRL rule into a SELECT arm over {@code source}. Body atoms become
     * self-joined aliases {@code b0, b1, ...}; shared variables are equated, all atoms
     * are required to share a named graph, and the head's version set is the intersection
     * of the body atoms' validities. Returns {@code null} (with a warning) for an unsafe
     * rule whose head uses a variable absent from the body.
     */
    private String swrlRuleArm(InferenceRule rule, String source) {
        List<RuleAtom> body = rule.body();
        if (body.isEmpty()) {
            log.warn("Skipping SWRL rule '{}' with an empty body", rule.name());
            return null;
        }

        // First occurrence (alias index + column) of each variable in the body.
        Map<String, String> varBinding = new HashMap<>();
        List<String> conditions = new ArrayList<>();
        List<String> validityTerms = new ArrayList<>();

        for (int i = 0; i < body.size(); i++) {
            RuleAtom atom = body.get(i);
            String alias = "b" + i;
            validityTerms.add(alias + ".validity");
            if (i > 0) {
                conditions.add("b0.id_named_graph = " + alias + ".id_named_graph");
            }
            bindPosition(atom.subject(), alias, "id_subject", varBinding, conditions);
            bindPosition(atom.predicate(), alias, "id_predicate", varBinding, conditions);
            bindPosition(atom.object(), alias, "id_object", varBinding, conditions);
        }

        String validityExpr = "(" + String.join(" & ", validityTerms) + ")";

        String headSubject = headColumn(rule.head().subject(), varBinding, rule);
        String headPredicate = headColumn(rule.head().predicate(), varBinding, rule);
        String headObject = headColumn(rule.head().object(), varBinding, rule);
        if (headSubject == null || headPredicate == null || headObject == null) {
            return null;
        }

        StringBuilder from = new StringBuilder();
        for (int i = 0; i < body.size(); i++) {
            from.append(i == 0 ? "" : ", ").append(source).append(" b").append(i);
        }

        conditions.add("bit_count" + validityExpr + " <> 0");

        return "  SELECT " + headSubject + ", " + headPredicate + ", " + headObject
                + ", b0.id_named_graph, " + validityExpr + "\n"
                + "  FROM " + from + "\n"
                + "  WHERE " + String.join("\n    AND ", conditions) + "\n";
    }

    /**
     * Record a body-atom position: a concrete term becomes an equality filter; a
     * variable records its binding on first sight and an equality on later sights.
     */
    private void bindPosition(Term term, String alias, String column,
                              Map<String, String> varBinding, List<String> conditions) {
        String cell = alias + "." + column;
        switch (term) {
            case Term.Variable variable -> {
                String existing = varBinding.putIfAbsent(variable.name(), cell);
                if (existing != null) {
                    conditions.add(existing + " = " + cell);
                }
            }
            case Term.Iri iri -> conditions.add(cell + " = " + iri.toIdSql());
            case Term.Literal literal -> conditions.add(cell + " = " + literal.toIdSql());
        }
    }

    /**
     * The SQL expression producing a head position: a concrete term resolves to its
     * identifier; a variable resolves to the body cell it is bound to (or {@code null}
     * with a warning when the rule is unsafe).
     */
    private String headColumn(Term term, Map<String, String> varBinding, InferenceRule rule) {
        return switch (term) {
            case Term.Iri iri -> iri.toIdSql();
            case Term.Literal literal -> literal.toIdSql();
            case Term.Variable variable -> {
                String cell = varBinding.get(variable.name());
                if (cell == null) {
                    log.warn("Skipping unsafe SWRL rule '{}': head variable '{}' does not occur in the body",
                            rule.name(), variable.name());
                }
                yield cell;
            }
        };
    }

    private static String iriId(String uri) {
        return "(SELECT id_resource_or_literal FROM resource_or_literal WHERE name = '"
                + Term.escape(uri) + "' AND type IS NULL)";
    }
}
