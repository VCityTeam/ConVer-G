package fr.cnrs.liris.jpugetgil.converg.sql;

import fr.cnrs.liris.jpugetgil.converg.entailment.EntailmentRegime;
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
 * @param entailmentRegime     The entailment regime (NONE, RDFS, OWL_LITE)
 * @param opSlice              The slice operator (optional)
 * @param opOrder              The order operator (optional)
 * @param quadSource           The relation a BGP scan reads from: {@code versioned_quad}
 *                             by default, or the saturated relation when query-time
 *                             inference is active
 */
public record SQLContext(
        Map<Node, List<SPARQLOccurrence>> sparqlVarOccurrences,
        boolean condensedMode,
        EntailmentRegime entailmentRegime,
        OpSlice opSlice,
        OpOrder opOrder,
        String quadSource
) {
    /** The default base relation for a versioned quad-pattern scan. */
    public static final String DEFAULT_QUAD_SOURCE = "versioned_quad";

    public SQLContext(
            Map<Node, List<SPARQLOccurrence>> sparqlVarOccurrences,
            boolean condensedMode,
            EntailmentRegime entailmentRegime,
            OpSlice opSlice,
            OpOrder opOrder
    ) {
        this(sparqlVarOccurrences, condensedMode, entailmentRegime, opSlice, opOrder, DEFAULT_QUAD_SOURCE);
    }

    public SQLContext copyWithNewVarOccurrences(Map<Node, List<SPARQLOccurrence>> newVarOccurrences) {
        return new SQLContext(newVarOccurrences, condensedMode, entailmentRegime, opSlice, opOrder, quadSource);
    }

    public SQLContext copyWithNewOpSlice(OpSlice newOpSlice) {
        return new SQLContext(sparqlVarOccurrences, condensedMode, entailmentRegime, newOpSlice, opOrder, quadSource);
    }

    public SQLContext copyWithNewOpOrder(OpOrder newOpOrder) {
        return new SQLContext(sparqlVarOccurrences, condensedMode, entailmentRegime, opSlice, newOpOrder, quadSource);
    }

    public SQLContext copyWithQuadSource(String newQuadSource) {
        return new SQLContext(sparqlVarOccurrences, condensedMode, entailmentRegime, opSlice, opOrder, newQuadSource);
    }
}
