package fr.cnrs.liris.jpugetgil.converg.inference;

import java.util.List;

/**
 * A Datalog-style inference rule over quads: when every {@code body} atom matches
 * (sharing variable bindings), the {@code head} atom is inferred. Its version set is
 * the intersection of the matching body atoms' version sets.
 * <p>
 * Rules are compiled to a SQL arm by {@link SaturationSqlBuilder}. SWRL rules are
 * turned into instances of this type by {@link SwrlRuleTranslator}; the RDFS/OWL
 * regime rules are emitted directly by the builder because they rely on the
 * transitive {@code subClassOf}/{@code subPropertyOf} closures.
 *
 * @param name the human-readable rule name (used in logs)
 * @param body the conjunctive premises (non-empty)
 * @param head the single inferred atom
 */
public record InferenceRule(String name, List<RuleAtom> body, RuleAtom head) {
}
