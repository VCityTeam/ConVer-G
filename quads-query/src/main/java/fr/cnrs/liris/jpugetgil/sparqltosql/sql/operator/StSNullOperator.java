package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVarType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.apache.jena.sparql.algebra.op.OpNull;

import java.util.List;

public class StSNullOperator extends StSOperator {
    private final OpNull opNull;
    private final List<SQLVariable> variables;

    public StSNullOperator(SQLContext context, OpNull opNull) {
        super(context);
        this.opNull = opNull;
        if (context.hasGraphVariable()) {
            variables = List.of(new SQLVariable(SQLVarType.DATA, context.graph().getName()));
        } else {
            variables = List.of();
        }
    }

    @Override
    public SQLQuery buildSQLQuery() {
        if (context.hasGraphVariable()) {
            return new SQLQuery(
                    "SELECT id_versioned_named_graph as v$" + context.graph().getName() + " FROM versioned_named_graph",
                    getSQLVariables());
        } else {
            return new SQLQuery("SELECT 1 as dummy", getSQLVariables());
        }
    }

    @Override
    public List<SQLVariable> getSQLVariables() {
        return List.of();
    }
}
