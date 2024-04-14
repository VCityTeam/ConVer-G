package fr.cnrs.liris.jpugetgil.sparqltosql.sql.context;

import fr.cnrs.liris.jpugetgil.sparqltosql.SPARQLOccurrence;
import fr.cnrs.liris.jpugetgil.sparqltosql.SQLContext;
import fr.cnrs.liris.jpugetgil.sparqltosql.SQLQuery;
import fr.cnrs.liris.jpugetgil.sparqltosql.SQLVariable;
import org.apache.jena.graph.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StSUnionOperator extends StSOperator {

    private final SQLQuery leftQuery;

    private final SQLQuery rightQuery;

    public StSUnionOperator(SQLQuery leftQuery, SQLQuery rightQuery) {
        this.leftQuery = leftQuery;
        this.rightQuery = rightQuery;
    }

    @Override
    public SQLQuery buildSQLQuery() {
        Node graph = leftQuery.getContext().graph() != null ?
                leftQuery.getContext().graph() : rightQuery.getContext().graph();
        Map<Node, List<SPARQLOccurrence>> varOccurrences = mergeMapOccurrences(
                leftQuery.getContext().sparqlVarOccurrences(),
                rightQuery.getContext().sparqlVarOccurrences()
        );

        this.sqlVariables = leftQuery.getContext().sqlVariables();
        for (SQLVariable sqlVariable : rightQuery.getContext().sqlVariables()) {
            if (!this.sqlVariables.contains(sqlVariable)) {
                this.sqlVariables.add(sqlVariable);
            }
        }

        SQLContext sqlContext = new SQLContext(
                graph,
                varOccurrences,
                "union_table",
                leftQuery.getContext().tableIndex() == null ? 0 : leftQuery.getContext().tableIndex() + 1,
                this.sqlVariables
        );

        return new SQLQuery(
                "SELECT * FROM (" + leftQuery.getSql() + ") UNION (" + rightQuery.getSql() + ")",
                sqlContext
        );
    }

    private Map<Node, List<SPARQLOccurrence>> mergeMapOccurrences(
            Map<Node, List<SPARQLOccurrence>> leftMapOccurrences,
            Map<Node, List<SPARQLOccurrence>> rightMapOccurrences
    ) {
        Map<Node, List<SPARQLOccurrence>> mergedOccurrences = new HashMap<>(leftMapOccurrences);

        rightMapOccurrences.forEach((node, occurrences) ->
                mergedOccurrences.computeIfAbsent(node, k -> new ArrayList<>()).addAll(occurrences)
        );

        return mergedOccurrences;
    }
}
