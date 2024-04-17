package fr.cnrs.liris.jpugetgil.sparqltosql.sql.operator;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.SPARQLPositionType;
import fr.cnrs.liris.jpugetgil.sparqltosql.sparql.expressions.Aggregator;
import fr.cnrs.liris.jpugetgil.sparqltosql.sql.SQLQuery;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;

import java.util.List;
import java.util.stream.Collectors;

public class StSGroupOperator extends StSOperator {
    private final OpGroup op;

    private final SQLQuery sqlQuery;

    public StSGroupOperator(OpGroup op, SQLQuery sqlQuery) {
        this.op = op;
        this.sqlQuery = sqlQuery;
        this.sqlVariables = sqlQuery.getContext().sqlVariables();
    }

    @Override
    public SQLQuery buildSQLQuery() {
        // TODO : GROUP BY
        VarExprList exprList = op.getGroupVars();
        List<Var> vars = exprList.getVars();
        String projections = op.getAggregators().stream()
                .map(exprAggregator -> new Aggregator(exprAggregator)
                        .toSQLString(this.sqlVariables)
                ).collect(Collectors.joining(", "));
        String groupBy = vars.stream()
                .map(variable -> {
                    if (sqlQuery.getContext().sparqlVarOccurrences().get(variable).stream()
                            .anyMatch(sparqlOccurrence -> sparqlOccurrence.getType() == SPARQLPositionType.GRAPH_NAME)) {
                        throw new IllegalArgumentException("TODO: GROUP BY GRAPH NAME");
                    } else {
                        return "sq.v$" + variable.getName();
                    }
                })
                .collect(Collectors.joining(", "));

        String explosions = Streams.mapWithIndex(sqlQuery.getContext().sparqlVarOccurrences().keySet()
                        .stream()
                        .filter((Node node) -> sqlQuery.getContext().sparqlVarOccurrences().get(node)
                                .stream()
                                .anyMatch(sparqlOccurrence -> sparqlOccurrence.getType() == SPARQLPositionType.GRAPH_NAME)),
                (node, index) -> ("JOIN versioned_named_graph vng" + index + " ON vng" +
                        index + ".id_named_graph = sq.ng$versioned_graph AND get_bit(sq.bs$versioned_graph, vng"
                        + index + ".index_version) = 1 \n")
        ).collect(Collectors.joining(", "));

        return new SQLQuery(
                "SELECT " + projections + " FROM (" + sqlQuery.getSql() +
                        ") sq " + explosions + " GROUP BY (" + groupBy + ")",
                sqlQuery.getContext()
        );
    }
}
