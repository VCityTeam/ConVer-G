package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.apache.jena.sparql.algebra.op.OpDistinct;

import java.util.List;

public class StSDistinctOperator extends StSOperator {
    private final OpDistinct op;
    private final StSOperator subQuery;
    private List<SQLVariable> variables = null;
    //private final SQLQuery sqlQuery;


    public StSDistinctOperator(SQLContext context, OpDistinct op) {
        super(context);
        this.op = op;
        subQuery = fromSPARQL(context, op.getSubOp());
    }

    @Override
    public List<SQLVariable> getSQLVariables() {
        if (variables == null) {
            variables = subQuery.getSQLVariables();
        }
        return variables;
    }

    @Override
    public SQLQuery buildSQLQuery() {
//        SQLContext sqlContext = new SQLContext(
//                sqlQuery.getContext().graph()
//                sqlQuery.getContext().sparqlVarOccurrences(),
//                sqlQuery.getContext().sqlVariables()
//        );
        SQLQuery sqlSubQuery = subQuery.buildSQLQuery();
        return new SQLQuery(
                "SELECT DISTINCT * FROM (" + sqlSubQuery.getSql() +
                        ") distinct_table",
                getSQLVariables()
        );
    }
}
