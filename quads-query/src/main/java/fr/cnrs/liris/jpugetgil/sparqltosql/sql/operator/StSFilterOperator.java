package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
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
