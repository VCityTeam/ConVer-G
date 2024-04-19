package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLContext;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLVariable;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpDistinct;
import org.apache.jena.sparql.algebra.op.OpNull;

import java.util.List;

public abstract class StSOperator {
    // List<SQLVariable> sqlVariables = new ArrayList<>();
    protected final SQLContext context;

    public StSOperator(SQLContext context) {
        this.context = context;
    }

    public static StSOperator fromSPARQL(SQLContext context, Op op) {
        return switch (op) {
            case OpBGP opBGP -> new StSBGPOperator(context, opBGP);
            case OpDistinct opDistinct -> new StSDistinctOperator(context, opDistinct);
//            case OpJoin opJoin -> new StSJoinOperator(opJoin, context);
            case OpNull opNull -> new StSNullOperator(context, opNull);
            default -> throw new IllegalStateException("Managing " + op.getClass() + " is not implemented yet.");
        };
    }

    /**
     * Builds a SQLQuery representing the SQL query generated for evaluating this operator
     *
     * @return the query and some metadata
     */
    public abstract SQLQuery buildSQLQuery();

    public abstract List<SQLVariable> getSQLVariables();
}
