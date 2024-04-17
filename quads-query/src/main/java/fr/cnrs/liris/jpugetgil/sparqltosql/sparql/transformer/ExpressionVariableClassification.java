package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.transformer;

import java.util.HashSet;
import java.util.Set;

public class ExpressionVariableClassification {
    private Set<String> variablesToExplode = new HashSet<>();
    private Set<String> variablesToIntersect = new HashSet<>();
    private Set<String> normalVariables = new HashSet<>();
    private Set<String> variableNeedingLookup = new HashSet<>();

    public Set<String> getNormalVariables() {
        return normalVariables;
    }

    public Set<String> getVariablesToExplode() {
        return variablesToExplode;
    }

    public Set<String> getVariablesToIntersect() {
        return variablesToIntersect;
    }

    public void addGraphVariableCondensed(String variable) {
        if (!variablesToExplode.contains(variable) && !normalVariables.contains(variable)) {
            variablesToIntersect.add(variable);
        } else if (variablesToExplode.contains(variable)) {
            normalVariables.remove(variable);
        }
    }

    public void addGraphVariableExploded(String variable) {
        variablesToExplode.add(variable);
        variablesToIntersect.remove(variable);
        normalVariables.remove(variable);
    }

    public void addNormalVariable(String variable) {
        if (variablesToIntersect.contains(variable) || variablesToExplode.contains(variable)) {
            addGraphVariableExploded(variable);
        } else {
            normalVariables.add(variable);
        }
    }

    public void addNeedLookupVariable(String variable) {
        variableNeedingLookup.add(variable);
        addNormalVariable(variable);
    }
}
