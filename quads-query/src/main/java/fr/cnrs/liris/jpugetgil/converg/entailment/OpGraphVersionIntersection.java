package fr.cnrs.liris.jpugetgil.converg.entailment;

import org.apache.jena.atlas.io.IndentedWriter;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpExt;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.serializer.SerializationContext;
import org.apache.jena.sparql.util.NodeIsomorphismMap;

import java.util.Objects;

/**
 * Custom algebra operator that intersects the version sets of two graph
 * coordinates of its sub-op, so an entailment premise found in one named graph
 * (typically a dedicated ontology/TBox graph) can be combined with data from
 * another named graph while keeping versioned semantics.
 * <p>
 * The sub-op is expected to bind:
 * <ul>
 *   <li>the data graph ({@code dataGraphNode}, variable or versioned graph URI)
 *       scoping the instance pattern;</li>
 *   <li>the schema graph variable ({@code schemaGraphVar}, rewriter-allocated)
 *       scoping the schema pattern (subClassOf/subPropertyOf/domain/range).</li>
 * </ul>
 * The operator keeps only the rows whose two version sets intersect, rebinds the
 * data graph's version set to that intersection, and discards the schema graph
 * variable. The inferred triple therefore only holds in versions where both the
 * data premise and the schema premise exist — even when they live in different
 * named graphs.
 * <p>
 * Produced by the {@link EntailmentRewriter} rules and consumed by the
 * {@code GraphVersionIntersectionSQLOperator}.
 */
public class OpGraphVersionIntersection extends OpExt {

    private final Op subOp;
    private final Node dataGraphNode;
    private final Var schemaGraphVar;

    /**
     * @param subOp         the wrapped op (join of a data pattern and a schema pattern)
     * @param dataGraphNode the graph scoping the data pattern (variable or versioned graph URI)
     * @param schemaGraphVar the rewriter-allocated variable scoping the schema pattern
     */
    public OpGraphVersionIntersection(Op subOp, Node dataGraphNode, Var schemaGraphVar) {
        super("graph-version-intersection");
        this.subOp = subOp;
        this.dataGraphNode = dataGraphNode;
        this.schemaGraphVar = schemaGraphVar;
    }

    public Op getSubOp() {
        return subOp;
    }

    public Node getDataGraphNode() {
        return dataGraphNode;
    }

    public Var getSchemaGraphVar() {
        return schemaGraphVar;
    }

    @Override
    public Op effectiveOp() {
        return null;
    }

    @Override
    public QueryIterator eval(QueryIterator input, ExecutionContext execCxt) {
        throw new UnsupportedOperationException(
                "OpGraphVersionIntersection is translated to SQL, not evaluated directly");
    }

    @Override
    public void outputArgs(IndentedWriter out, SerializationContext sCxt) {
        out.print("(" + dataGraphNode + " ∩ " + schemaGraphVar + ")");
    }

    @Override
    public int hashCode() {
        return Objects.hash(subOp, dataGraphNode, schemaGraphVar);
    }

    @Override
    public boolean equalTo(Op other, NodeIsomorphismMap labelMap) {
        if (!(other instanceof OpGraphVersionIntersection otherGVI)) return false;
        return Objects.equals(dataGraphNode, otherGVI.dataGraphNode)
                && Objects.equals(schemaGraphVar, otherGVI.schemaGraphVar)
                && subOp.equalTo(otherGVI.subOp, labelMap);
    }
}
