package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import org.apache.jena.sparql.algebra.op.OpSlice;

public class SliceSQLOperator extends SQLOperator {

    OpSlice opSlice;

    SQLQuery query;

    public SliceSQLOperator(OpSlice opSlice, SQLQuery query) {
        this.opSlice = opSlice;
        this.query = query;
    }

    /**
     * @return the SQL query with the slice operator
     */
    @Override
    public SQLQuery buildSQLQuery() {
        query.setContext(query.getContext().copyWithNewOpSlice(opSlice));
        return query;
    }

    /**
     * @return nothing
     */
    @Override
    protected String buildSelect() {
        return null;
    }

    /**
     * @return nothing
     */
    @Override
    protected String buildFrom() {
        return null;
    }

    /**
     * @return nothing
     */
    @Override
    protected String buildWhere() {
        return null;
    }
}
