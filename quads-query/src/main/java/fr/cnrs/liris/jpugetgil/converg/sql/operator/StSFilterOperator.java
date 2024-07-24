package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import org.apache.jena.sparql.algebra.op.OpFilter;

public class StSFilterOperator extends StSOperator {
    private final SQLQuery sqlQuery;

    private final OpFilter op;


    public StSFilterOperator(SQLQuery sqlQuery, OpFilter op) {
        this.sqlQuery = sqlQuery;
        this.op = op;
    }


    @Override
    public SQLQuery buildSQLQuery() {
        return sqlQuery;
    }
}
