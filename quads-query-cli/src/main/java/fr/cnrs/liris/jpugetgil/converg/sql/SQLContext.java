package fr.cnrs.liris.jpugetgil.converg.sql;

import fr.cnrs.liris.jpugetgil.converg.sparql.SPARQLOccurrence;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpOrder;
import org.apache.jena.sparql.algebra.op.OpSlice;

import java.util.List;
import java.util.Map;

/**
 * This record represents the context of a SQL query.
 *
 * @param sparqlVarOccurrences The occurrences of the variables in the context
 * @param condensedMode        If the condensed mode is activated
 * @param opSlice              The slice operator (optional)
 * @param opOrder              The order operator (optional)
 */
public record SQLContext(
        Map<Node, List<SPARQLOccurrence>> sparqlVarOccurrences,
        boolean condensedMode,
        OpSlice opSlice,
        OpOrder opOrder
) {
    public SQLContext copyWithNewVarOccurrences(Map<Node, List<SPARQLOccurrence>> newVarOccurrences) {
        return new SQLContext(newVarOccurrences, condensedMode, opSlice, opOrder);
    }

    public SQLContext copyWithNewOpSlice(OpSlice newOpSlice) {
        return new SQLContext(sparqlVarOccurrences, condensedMode, newOpSlice, opOrder);
    }

    public SQLContext copyWithNewOpOrder(OpOrder newOpOrder) {
        return new SQLContext(sparqlVarOccurrences, condensedMode, opSlice, newOpOrder);
    }
}
