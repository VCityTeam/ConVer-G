package fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions;

import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.transformer.FilterConfiguration;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.apache.jena.sparql.expr.ExprVar;

import java.util.List;

public class Var extends AbstractExpression<ExprVar> {
    /**
     * Builds an expression from a jena expr
     *
     * @param expr the source jena expr
     */
    public Var(ExprVar expr) {
        super(expr);
    }

    @Override
    public void updateFilterConfiguration(FilterConfiguration configuration, boolean requiresValue) {
        if (requiresValue) {
            configuration.addNeedLookupVariable(varName());
        }
    }

    /**
     * The variable's name
     *
     * @return the variable's name
     */
    public String varName() {
        return getJenaExpr().getVarName();
    }

    @Override
    public String toSQLString(List<SQLVariable> sqlVariables) {
        for (SQLVariable sqlVariable : sqlVariables) {
            if (sqlVariable.getSqlVarName().equals(varName()) && sqlVariable.getSqlVarType() == SQLVarType.DATA) {
                return "v$" + varName();
            }
            if (sqlVariable.getSqlVarName().equals(varName()) &&
                    (
                            sqlVariable.getSqlVarType() == SQLVarType.GRAPH_NAME ||
                                    sqlVariable.getSqlVarType() == SQLVarType.BIT_STRING
                    )
            ) {
                return "v$id_versioned_named_graph";
            }
        }

        return "v$" + varName();
    }

    @Override
    public String toSQLString() {
        return varName();
    }
}
