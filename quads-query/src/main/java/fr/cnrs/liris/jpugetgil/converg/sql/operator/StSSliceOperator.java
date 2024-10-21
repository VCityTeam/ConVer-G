package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import org.apache.jena.sparql.algebra.op.OpSlice;

public class StSSliceOperator extends StSOperator {
    private final SQLQuery sqlQuery;

    private final OpSlice op;

    public StSSliceOperator(OpSlice op, SQLQuery sqlQuery) {
        this.sqlQuery = sqlQuery;
        this.op = op;
        this.sqlVariables = sqlQuery.getContext().sqlVariables();
    }

    @Override
    public SQLQuery buildSQLQuery() {
        sqlQuery.setOpSlice(op);
        return sqlQuery;
    }
}
