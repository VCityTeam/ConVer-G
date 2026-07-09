package fr.cnrs.liris.jpugetgil.converg.path;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.op.OpExt;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

import java.util.Objects;

/**
 * Custom algebra operator representing the zero-length component of a
 * {@code path?} (zero-or-one) property path: it binds {@code varNode} to
 * {@code termNode} in every graph of the current scope.
 * <p>
 * Produced by the {@link PathRewriter} and consumed by the
 * {@code ZeroLengthPathSQLOperator}. Only the (concrete term, variable) form is
 * supported; a zero-length path between two unbound variables would have to
 * enumerate every term of the store.
 */
public class OpZeroLengthPath extends OpExt {

    private final Node graphNode;
    private final Node termNode;
    private final Node varNode;

    /**
     * @param graphNode the graph scope (variable, URI or the generated default graph node)
     * @param termNode  the concrete endpoint of the zero-length path
     * @param varNode   the variable endpoint, bound to {@code termNode}
     */
    public OpZeroLengthPath(Node graphNode, Node termNode, Node varNode) {
        super("zero-length-path");
        this.graphNode = graphNode;
        this.termNode = termNode;
        this.varNode = varNode;
    }

    public Node getGraphNode() {
        return graphNode;
    }

    public Node getTermNode() {
        return termNode;
    }

    public Node getVarNode() {
        return varNode;
    }

    @Override
    public org.apache.jena.sparql.algebra.Op effectiveOp() {
        return null;
    }

    @Override
    public QueryIterator eval(QueryIterator input, ExecutionContext execCxt) {
        throw new UnsupportedOperationException("OpZeroLengthPath is translated to SQL, not evaluated directly");
    }

    @Override
    public void outputArgs(IndentedWriter out, SerializationContext sCxt) {
        out.print("(" + graphNode + " " + termNode + " = " + varNode + ")");
    }

    @Override
    public int hashCode() {
        return Objects.hash(graphNode, termNode, varNode);
    }

    @Override
    public boolean equalTo(org.apache.jena.sparql.algebra.Op other, NodeIsomorphismMap labelMap) {
        if (!(other instanceof OpZeroLengthPath otherZLP)) return false;
        return Objects.equals(graphNode, otherZLP.graphNode)
                && Objects.equals(termNode, otherZLP.termNode)
                && Objects.equals(varNode, otherZLP.varNode);
    }
}
