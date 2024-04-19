package fr.cnrs.liris.jpugetgil.sparqltosql.sql.ast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Represents an "and" of conditions.
 */
public class Conjunction implements Condition {
    private Collection<Condition> subConditions = new ArrayList<>();

    /**
     * Adds a condition to the conjunction
     *
     * @param condition the condition to add
     * @return this condition for chaining
     */
    public Conjunction and(Condition condition) {
        subConditions.add(condition);
        return this;
    }

    @Override
    public String asSQL() {
        return "(" + subConditions.stream().map(Condition::asSQL).collect(Collectors.joining("\n AND ")) + ")";
    }
}
