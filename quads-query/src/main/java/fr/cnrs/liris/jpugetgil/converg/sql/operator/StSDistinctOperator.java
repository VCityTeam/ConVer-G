package fr.cnrs.liris.jpugetgil.converg.sql.operator;

import fr.cnrs.liris.jpugetgil.converg.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;

public class StSDistinctOperator extends StSOperator {
    private final SQLQuery sqlQuery;

    public StSDistinctOperator(SQLQuery sqlQuery) {
        this.sqlQuery = sqlQuery;
        this.sqlVariables = sqlQuery.getContext().sqlVariables();
    }

    @Override
    public SQLQuery buildSQLQuery() {
        SQLContext sqlContext = new SQLContext(
                sqlQuery.getContext().graph(),
                sqlQuery.getContext().sparqlVarOccurrences(),
                sqlQuery.getContext().sqlVariables(),
                sqlQuery.getContext().condensedMode()
        );

        return new SQLQuery(
                "SELECT DISTINCT * FROM (" + sqlQuery.getSql() +
                        ") distinct_table",
                sqlContext
        );
    }
}
