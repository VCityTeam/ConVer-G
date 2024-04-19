package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.*;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.ast.Comparison;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.ast.Condition;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.ast.Conjunction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StSExplodeOperator extends StSOperator {
    private final StSOperator subOperator;
    private final String variableToExplode;
    private final List<SQLVariable> variables;
    private final List<SQLVariable> copiedVariables;
    private final SQLVariable explodedVariable;

    public StSExplodeOperator(SQLContext context, StSOperator subOperator,
                              String variableToExplode) {
        super(context);
        this.subOperator = subOperator;
        this.variableToExplode = variableToExplode;
        variables = subOperator.getSQLVariables()
                .stream()
                .filter(v -> !v.getSqlVarName().equals(variableToExplode))
                .collect(
                        Collectors.toCollection(ArrayList::new));
        copiedVariables = List.copyOf(variables);
        explodedVariable = new SQLVariable(SQLVarType.DATA, variableToExplode);
        variables.add(explodedVariable);
    }

    @Override
    public SQLQuery buildSQLQuery() {
        var subQuery = subOperator.buildSQLQuery();
        var subTable = "exploded";
        var vngTable = "vng";
        String copy_attributes = copiedVariables.stream()
                .map(v -> v.getSQLValue(subTable) + " as " + v.getSQLAttributeName())
                .collect(
                        Collectors.joining(", "));
        String get_vng =
                vngTable + "." + Schema.ID_VERSIONED_NAMED_GRAPH + " as " + explodedVariable.getSQLAttributeName();
        String atts = copy_attributes.length() == 0 ? get_vng : copy_attributes + ", " + get_vng;
        SQLVariable explodedGN = new SQLVariable(SQLVarType.GRAPH_NAME, variableToExplode);
        SQLVariable explodedBS = new SQLVariable(SQLVarType.BIT_STRING, variableToExplode);
        Condition condition = new Conjunction().and(
                Comparison.eq(explodedGN.getSQLValue(subTable), vngTable + "." + Schema.ID_NAMED_GRAPH)).and(
                Comparison.eq(
                        "get_bit(" + explodedBS.getSQLValue(subTable) + ", " + vngTable + "." + Schema.VERSION_INDEX +
                                ")", "1"));
        return new SQLQuery(
                "SELECT " + atts + "\nFROM (" + subQuery.getSql() + ") " + subTable + " JOIN " + Schema.VNG_TABLE +
                        " " + vngTable + " ON " + condition.asSQL(), getSQLVariables());
    }

    @Override
    public List<SQLVariable> getSQLVariables() {
        return variables;
    }
}
