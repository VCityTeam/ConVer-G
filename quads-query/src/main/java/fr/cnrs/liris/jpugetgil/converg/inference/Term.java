package fr.cnrs.liris.jpugetgil.converg.inference;

/**
 * A term appearing in an {@link InferenceRule} atom: either a rule variable or a
 * concrete RDF node (IRI or literal). Concrete terms know how to resolve to their
 * {@code resource_or_literal} identifier in SQL.
 */
public sealed interface Term {

    /**
     * A rule variable, identified by name (shared occurrences of the same name unify).
     */
    record Variable(String name) implements Term {
    }

    /**
     * A concrete IRI.
     */
    record Iri(String uri) implements Term {
    }

    /**
     * A concrete literal (lexical form + optional datatype IRI).
     */
    record Literal(String lexical, String datatype) implements Term {
    }

    /**
     * @return a scalar SQL subquery yielding the {@code id_resource_or_literal} of a
     * concrete term. Only valid for {@link Iri} and {@link Literal}.
     */
    default String toIdSql() {
        return switch (this) {
            case Iri iri -> "(SELECT id_resource_or_literal FROM resource_or_literal WHERE name = '"
                    + escape(iri.uri()) + "' AND type IS NULL)";
            case Literal literal -> "(SELECT id_resource_or_literal FROM resource_or_literal WHERE name = '"
                    + escape(literal.lexical()) + "' AND type IS NOT NULL)";
            case Variable ignored ->
                    throw new IllegalStateException("A variable term has no resource identifier");
        };
    }

    static String escape(String value) {
        return value.replace("'", "''");
    }
}
