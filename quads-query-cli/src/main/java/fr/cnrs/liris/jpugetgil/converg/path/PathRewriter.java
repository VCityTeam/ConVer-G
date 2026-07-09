package fr.cnrs.liris.jpugetgil.converg.path;

import fr.cnrs.liris.jpugetgil.converg.entailment.OpTransitiveClosure;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.ARQNotImplemented;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.*;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Rewrites property-path operators ({@link OpPath}, possibly wrapped in
 * {@link OpGraph}) into the algebra already supported by the SQL translator:
 * quad patterns, joins, unions, {@link OpTransitiveClosure} (recursive CTE) and
 * {@link OpZeroLengthPath}.
 * <p>
 * Supported path forms: {@code <p>}, {@code ^path}, {@code path1/path2},
 * {@code path1|path2}, {@code <p>+}, {@code <p>*} (one concrete endpoint
 * required) and {@code <p>?}. Negated property sets and {@code path{n,m}}
 * modifiers are not implemented.
 */
public final class PathRewriter {

    private static final AtomicLong FRESH_VAR_COUNTER = new AtomicLong();

    private PathRewriter() {
    }

    /**
     * Rewrite the content of a {@code GRAPH} scope (or the default graph) so that
     * it no longer contains {@link OpPath} operators.
     *
     * @param graph the graph node in scope (variable, URI or generated default graph node)
     * @param op    the operator under the graph scope
     * @return an equivalent operator tree without path operators
     */
    public static Op rewrite(Node graph, Op op) {
        return switch (op) {
            case OpPath opPath -> transformPath(
                    graph,
                    opPath.getTriplePath().getSubject(),
                    opPath.getTriplePath().getPath(),
                    opPath.getTriplePath().getObject());
            case OpBGP opBGP -> new OpQuadPattern(graph, opBGP.getPattern());
            case OpSequence opSequence -> {
                OpSequence rewritten = OpSequence.create();
                opSequence.getElements().forEach(element -> rewritten.add(rewrite(graph, element)));
                yield rewritten;
            }
            case OpJoin opJoin -> OpJoin.create(
                    rewrite(graph, opJoin.getLeft()),
                    rewrite(graph, opJoin.getRight()));
            case OpUnion opUnion -> OpUnion.create(
                    rewrite(graph, opUnion.getLeft()),
                    rewrite(graph, opUnion.getRight()));
            case OpFilter opFilter -> OpFilter.filterBy(
                    opFilter.getExprs(),
                    rewrite(graph, opFilter.getSubOp()));
            default -> throw new ARQNotImplemented(
                    "GRAPH sub-operator not supported with property paths: " + op.getClass().getSimpleName());
        };
    }

    private static Op transformPath(Node graph, Node subject, Path path, Node object) {
        return switch (path) {
            case P_Link link -> quadPattern(graph, subject, link.getNode(), object);
            case P_ReverseLink reverseLink -> quadPattern(graph, object, reverseLink.getNode(), subject);
            case P_Inverse inverse -> transformPath(graph, object, inverse.getSubPath(), subject);
            case P_Seq seq -> {
                Var mid = freshVar();
                yield OpJoin.create(
                        transformPath(graph, subject, seq.getLeft(), mid),
                        transformPath(graph, mid, seq.getRight(), object));
            }
            case P_Alt alt -> OpUnion.create(
                    transformPath(graph, subject, alt.getLeft(), object),
                    transformPath(graph, subject, alt.getRight(), object));
            case P_OneOrMore1 oneOrMore -> closure(graph, subject, oneOrMore.getSubPath(), object, 1);
            case P_OneOrMoreN oneOrMore -> closure(graph, subject, oneOrMore.getSubPath(), object, 1);
            case P_ZeroOrMore1 zeroOrMore -> closure(graph, subject, zeroOrMore.getSubPath(), object, 0);
            case P_ZeroOrMoreN zeroOrMore -> closure(graph, subject, zeroOrMore.getSubPath(), object, 0);
            case P_ZeroOrOne zeroOrOne -> OpUnion.create(
                    zeroLength(graph, subject, object),
                    transformPath(graph, subject, zeroOrOne.getSubPath(), object));
            default -> throw new ARQNotImplemented(
                    "Property path form not supported: " + path.getClass().getSimpleName() + " in " + path);
        };
    }

    /**
     * Build the transitive closure of a link, following inverse links backwards.
     */
    private static Op closure(Node graph, Node subject, Path innerPath, Node object, int minHops) {
        Node start = subject;
        Node end = object;
        Node predicate;

        switch (innerPath) {
            case P_Link link -> predicate = link.getNode();
            case P_ReverseLink reverseLink -> {
                predicate = reverseLink.getNode();
                start = object;
                end = subject;
            }
            case P_Inverse inverse when inverse.getSubPath() instanceof P_Link link -> {
                predicate = link.getNode();
                start = object;
                end = subject;
            }
            default -> throw new ARQNotImplemented(
                    "Transitive closure is only supported over a single (possibly inverse) property: " + innerPath);
        }

        checkClosureEndpoint(start);
        checkClosureEndpoint(end);
        if (minHops == 0 && !start.isConcrete() && !end.isConcrete()) {
            throw new ARQNotImplemented(
                    "Zero-or-more property path requires at least one concrete endpoint: "
                            + subject + " " + innerPath + "* " + object);
        }

        return new OpTransitiveClosure(graph, start, end, predicate.getURI(), minHops);
    }

    private static void checkClosureEndpoint(Node node) {
        if (node.isConcrete() && !node.isURI()) {
            throw new ARQNotImplemented("Transitive closure endpoints must be URIs or variables: " + node);
        }
    }

    /**
     * Zero-length component of a {@code path?}: binds the variable endpoint to the
     * concrete one.
     */
    private static Op zeroLength(Node graph, Node subject, Node object) {
        if (subject.isURI() && object.isVariable()) {
            return new OpZeroLengthPath(graph, subject, object);
        }
        if (object.isURI() && subject.isVariable()) {
            return new OpZeroLengthPath(graph, object, subject);
        }
        throw new ARQNotImplemented(
                "Zero-or-one property path requires one URI endpoint and one variable endpoint: "
                        + subject + " / " + object);
    }

    private static Op quadPattern(Node graph, Node subject, Node predicate, Node object) {
        BasicPattern pattern = new BasicPattern();
        pattern.add(Triple.create(subject, predicate, object));
        return new OpQuadPattern(graph, pattern);
    }

    private static Var freshVar() {
        return Var.alloc("__path_p" + FRESH_VAR_COUNTER.getAndIncrement());
    }
}
