package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import org.apache.jena.sparql.algebra.op.OpOrder;

public class OrderSQLOperator extends SQLOperator {

    OpOrder opOrder;

    SQLQuery query;

    public OrderSQLOperator(OpOrder opOrder, SQLQuery query) {
        this.opOrder = opOrder;
        this.query = query;
    }

    /**
     * @return the SQL query of the distinct operator
     */
    @Override
    public SQLQuery buildSQLQuery() {
        query.setContext(query.getContext().setOpOrder(opOrder));
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
        return "";
    }
}
