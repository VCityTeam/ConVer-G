package fr.cnrs.liris.jpugetgil.converg.sql;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import org.apache.jena.graph.Node;

import java.util.List;
import java.util.Map;

/**
 * This class represents the context of a SQL query.
 *
 * @param sparqlVarOccurrences The occurrences of the variables in the context
 * @param condensedMode        If the condensed mode is activated
 */
public record SQLContext(
        Map<Node, List<SPARQLOccurrence>> sparqlVarOccurrences,
        boolean condensedMode
) {
    public SQLContext setVarOccurrences(Map<Node, List<SPARQLOccurrence>> newVarOccurrences) {
        return new SQLContext(newVarOccurrences, condensedMode);
    }
}
