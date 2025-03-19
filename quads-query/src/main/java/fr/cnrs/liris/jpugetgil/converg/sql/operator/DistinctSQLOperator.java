package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import org.apache.jena.sparql.algebra.op.OpDistinct;

public class DistinctSQLOperator extends SQLOperator {

    OpDistinct opDistinct;

    SQLQuery query;

    public DistinctSQLOperator(OpDistinct opDistinct, SQLQuery query) {
        this.opDistinct = opDistinct;
        this.query = query;
    }

    /**
     * @return the SQL query of the distinct operator
     */
    @Override
    public SQLQuery buildSQLQuery() {
        String select = buildSelect();
        String from = buildFrom();
        String where = buildWhere();
        String sql = select + from + where;

        return new SQLQuery(
                sql,
                query.getContext()
        );
    }

    /**
     * @return the select part of distinct operator
     */
    @Override
    protected String buildSelect() {
        return "SELECT DISTINCT * ";
    }

    /**
     * @return the from part of distinct operator
     */
    @Override
    protected String buildFrom() {
        String DISTINCT_TABLE_NAME = "dist_table";
        return "FROM (" + this.query.getSql() + ") " + DISTINCT_TABLE_NAME;
    }

    /**
     * @return the where part of distinct operator
     */
    @Override
    protected String buildWhere() {
        return "";
    }
}
