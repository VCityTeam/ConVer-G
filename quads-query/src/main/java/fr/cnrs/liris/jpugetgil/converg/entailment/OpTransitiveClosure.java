package fr.cnrs.liris.jpugetgil.converg.entailment;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpExt;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

import java.util.Objects;

/**
 * Custom algebra operator representing a transitive closure over a property.
 * For example, {@code ?start rdfs:subClassOf* ?end} within a graph {@code ?g}.
 * <p>
 * This operator is produced by the {@link EntailmentRewriter} and consumed by
 * the {@code TransitiveClosureSQLOperator} to generate a recursive CTE.
 * The version set at each step of the chain is intersected (conjunctive semantics).
 */
public class OpTransitiveClosure extends OpExt {

    private final Node graphNode;
    private final Node startNode;
    private final Node endNode;
    private final String predicateURI;

    /**
     * @param graphNode    the graph scope (variable or URI)
     * @param startNode    the start of the transitive path (variable or URI)
     * @param endNode      the end of the transitive path (variable or URI)
     * @param predicateURI the URI of the transitive property (e.g., rdfs:subClassOf)
     */
    public OpTransitiveClosure(Node graphNode, Node startNode, Node endNode, String predicateURI) {
        super("transitive-closure");
        this.graphNode = graphNode;
        this.startNode = startNode;
        this.endNode = endNode;
        this.predicateURI = predicateURI;
    }

    public Node getGraphNode() {
        return graphNode;
    }

    public Node getStartNode() {
        return startNode;
    }

    public Node getEndNode() {
        return endNode;
    }

    public String getPredicateURI() {
        return predicateURI;
    }

    @Override
    public org.apache.jena.sparql.algebra.Op effectiveOp() {
        return null;
    }

    @Override
    public QueryIterator eval(QueryIterator input, ExecutionContext execCxt) {
        throw new UnsupportedOperationException("OpTransitiveClosure is translated to SQL, not evaluated directly");
    }

    @Override
    public void outputArgs(IndentedWriter out, SerializationContext sCxt) {
        out.print("(" + graphNode + " " + startNode + " <" + predicateURI + ">* " + endNode + ")");
    }

    @Override
    public int hashCode() {
        return Objects.hash(graphNode, startNode, endNode, predicateURI);
    }

    @Override
    public boolean equalTo(org.apache.jena.sparql.algebra.Op other, NodeIsomorphismMap labelMap) {
        if (!(other instanceof OpTransitiveClosure otherTC)) return false;
        return Objects.equals(graphNode, otherTC.graphNode)
                && Objects.equals(startNode, otherTC.startNode)
                && Objects.equals(endNode, otherTC.endNode)
                && Objects.equals(predicateURI, otherTC.predicateURI);
    }
}
