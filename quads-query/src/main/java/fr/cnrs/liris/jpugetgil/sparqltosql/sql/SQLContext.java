package fr.cnrs.liris.jpugetgil.sparqltosql.sql;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_Variable;

/**
 * This class represents the context of a SQL query.
 *
 * @param graph The graph of the context
 *              // @param sparqlVarOccurrences The occurrences of the variables in the context
 *              // @param sqlVariables         The variables of the SQL query made by the context
 */
public record SQLContext(Node graph)
        //, Map<Node, List<SPARQLOccurrence>> sparqlVarOccurrences, List<SQLVariable> sqlVariables)
{
    public SQLContext setGraph(Node newGraph) {
        return new SQLContext(newGraph); //, sparqlVarOccurrences, sqlVariables);
    }

    /**
     * Checks whether there is a ghraph in this context
     * @return true if there is a context graph
     */
    public boolean hasGraph() {
        return graph != null;
    }

    /**
     * Checks whether there is a context graph and this is a variable
     * @return true if it is the case
     */
    public boolean hasGraphVariable() {
        return graph instanceof Node_Variable;
    }
//    public SQLContext setVarOccurrences(Map<Node, List<SPARQLOccurrence>> newVarOccurrences) {
//        return new SQLContext(graph, newVarOccurrences, sqlVariables);
//    }
//
//    public SQLContext setSQLVariables(List<SQLVariable> newSQLVariables) {
//        return new SQLContext(graph, sparqlVarOccurrences, newSQLVariables);
//    }
}
