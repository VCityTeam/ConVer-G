package fr.cnrs.liris.jpugetgil.converg.entailment;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpQuadPattern;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;

import java.util.List;

/**
 * Factory for standard RDFS entailment rules.
 * <p>
 * Each rule checks whether a quad pattern triggers inference and produces an Op subtree
 * that, when UNIONed with the original pattern, returns both explicit and inferred triples.
 * Version sets compose naturally: {@code OpJoin} yields intersection (conjunctive premises).
 */
public final class RDFSRules {

    public static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public static final String RDFS_SUBCLASS_OF = "http://www.w3.org/2000/01/rdf-schema#subClassOf";
    public static final String RDFS_SUBPROPERTY_OF = "http://www.w3.org/2000/01/rdf-schema#subPropertyOf";
    public static final String RDFS_DOMAIN = "http://www.w3.org/2000/01/rdf-schema#domain";
    public static final String RDFS_RANGE = "http://www.w3.org/2000/01/rdf-schema#range";

    private static final Node RDF_TYPE_NODE = NodeFactory.createURI(RDF_TYPE);
    private static final Node RDFS_DOMAIN_NODE = NodeFactory.createURI(RDFS_DOMAIN);
    private static final Node RDFS_RANGE_NODE = NodeFactory.createURI(RDFS_RANGE);

    private RDFSRules() {
    }

    /**
     * @return all RDFS entailment rules
     */
    public static List<EntailmentRule> allRules() {
        return List.of(rdfs9(), rdfs7(), rdfs2(), rdfs3());
    }

    /**
     * rdfs9: subClassOf type propagation (with transitive closure).
     * <p>
     * If {@code ?x rdf:type ?C}, also infer {@code ?x rdf:type ?D} for any
     * {@code ?C rdfs:subClassOf+ ?D}. The expansion is:
     * <pre>
     *   OpJoin(
     *     OpQuadPattern(graph, [?x rdf:type ?__ent_sub]),
     *     OpTransitiveClosure(graph, ?__ent_sub, rdfs:subClassOf, ?C)
     *   )
     * </pre>
     * where ?C is the original object (the superclass we're looking for).
     * The JOIN computes version-set intersection: the inferred type only holds in
     * versions where both the type assertion and the subClassOf chain exist.
     */
    public static EntailmentRule rdfs9() {
        return new EntailmentRule() {
            @Override
            public String name() {
                return "rdfs9";
            }

            @Override
            public boolean appliesTo(Quad quad) {
                return quad.getPredicate().isURI()
                        && quad.getPredicate().getURI().equals(RDF_TYPE);
            }

            @Override
            public Op expand(Quad quad, Node graphNode, FreshVariableAllocator allocator) {
                Node subject = quad.getSubject();   // ?x
                Node object = quad.getObject();      // ?C (the class, variable or URI)

                // Fresh variable for the intermediate subclass
                Var subClass = allocator.allocate("sub");

                // Pattern: ?x rdf:type ?__ent_sub (in the same graph)
                BasicPattern typePattern = new BasicPattern();
                typePattern.add(Triple.create(subject, RDF_TYPE_NODE, subClass));
                Op typeOp = new OpQuadPattern(graphNode, typePattern);

                // Transitive closure: ?__ent_sub rdfs:subClassOf+ ?C
                Op closureOp = new OpTransitiveClosure(graphNode, subClass, object, RDFS_SUBCLASS_OF);

                return OpJoin.create(typeOp, closureOp);
            }
        };
    }

    /**
     * rdfs7: subPropertyOf propagation (with transitive closure).
     * <p>
     * If {@code ?s ?p ?o} where {@code ?p} is a concrete URI, also match
     * {@code ?s ?subp ?o} for any {@code ?subp rdfs:subPropertyOf+ ?p}.
     */
    public static EntailmentRule rdfs7() {
        return new EntailmentRule() {
            @Override
            public String name() {
                return "rdfs7";
            }

            @Override
            public boolean appliesTo(Quad quad) {
                // Only applies when the predicate is a concrete URI (not rdf:type itself,
                // and not an RDFS/OWL schema predicate to prevent recursive expansion)
                return quad.getPredicate().isURI()
                        && !quad.getPredicate().getURI().equals(RDF_TYPE)
                        && !isSchemaProperty(quad.getPredicate().getURI());
            }

            @Override
            public Op expand(Quad quad, Node graphNode, FreshVariableAllocator allocator) {
                Node subject = quad.getSubject();
                Node predicate = quad.getPredicate();  // concrete URI
                Node object = quad.getObject();

                // Fresh variable for the sub-property
                Var subProp = allocator.allocate("subp");

                // Pattern: ?s ?__ent_subp ?o
                BasicPattern dataPattern = new BasicPattern();
                dataPattern.add(Triple.create(subject, subProp, object));
                Op dataOp = new OpQuadPattern(graphNode, dataPattern);

                // Transitive closure: ?__ent_subp rdfs:subPropertyOf+ ?p
                Op closureOp = new OpTransitiveClosure(graphNode, subProp, predicate, RDFS_SUBPROPERTY_OF);

                return OpJoin.create(dataOp, closureOp);
            }
        };
    }

    /**
     * rdfs2: domain inference.
     * <p>
     * If {@code ?x rdf:type ?C}, also infer from {@code ?x ?p ?_ . ?p rdfs:domain ?C}.
     * The expansion joins a data pattern with a domain declaration.
     */
    public static EntailmentRule rdfs2() {
        return new EntailmentRule() {
            @Override
            public String name() {
                return "rdfs2";
            }

            @Override
            public boolean appliesTo(Quad quad) {
                return quad.getPredicate().isURI()
                        && quad.getPredicate().getURI().equals(RDF_TYPE);
            }

            @Override
            public Op expand(Quad quad, Node graphNode, FreshVariableAllocator allocator) {
                Node subject = quad.getSubject();   // ?x
                Node classNode = quad.getObject();   // ?C

                Var freshPred = allocator.allocate("dp");
                Var freshObj = allocator.allocate("do");

                // Pattern: ?x ?__ent_dp ?__ent_do (any predicate applied to ?x)
                BasicPattern dataPattern = new BasicPattern();
                dataPattern.add(Triple.create(subject, freshPred, freshObj));
                Op dataOp = new OpQuadPattern(graphNode, dataPattern);

                // Pattern: ?__ent_dp rdfs:domain ?C
                BasicPattern domainPattern = new BasicPattern();
                domainPattern.add(Triple.create(freshPred, RDFS_DOMAIN_NODE, classNode));
                Op domainOp = new OpQuadPattern(graphNode, domainPattern);

                return OpJoin.create(dataOp, domainOp);
            }
        };
    }

    /**
     * rdfs3: range inference.
     * <p>
     * If {@code ?x rdf:type ?C}, also infer from {@code ?_ ?p ?x . ?p rdfs:range ?C}.
     * The expansion joins a data pattern with a range declaration.
     */
    public static EntailmentRule rdfs3() {
        return new EntailmentRule() {
            @Override
            public String name() {
                return "rdfs3";
            }

            @Override
            public boolean appliesTo(Quad quad) {
                return quad.getPredicate().isURI()
                        && quad.getPredicate().getURI().equals(RDF_TYPE);
            }

            @Override
            public Op expand(Quad quad, Node graphNode, FreshVariableAllocator allocator) {
                Node object = quad.getSubject();     // ?x (the resource whose type we want)
                Node classNode = quad.getObject();   // ?C

                Var freshSubj = allocator.allocate("rs");
                Var freshPred = allocator.allocate("rp");

                // Pattern: ?__ent_rs ?__ent_rp ?x (any triple where ?x is the object)
                BasicPattern dataPattern = new BasicPattern();
                dataPattern.add(Triple.create(freshSubj, freshPred, object));
                Op dataOp = new OpQuadPattern(graphNode, dataPattern);

                // Pattern: ?__ent_rp rdfs:range ?C
                BasicPattern rangePattern = new BasicPattern();
                rangePattern.add(Triple.create(freshPred, RDFS_RANGE_NODE, classNode));
                Op rangeOp = new OpQuadPattern(graphNode, rangePattern);

                return OpJoin.create(dataOp, rangeOp);
            }
        };
    }

    private static boolean isSchemaProperty(String uri) {
        return uri.startsWith("http://www.w3.org/2000/01/rdf-schema#")
                || uri.startsWith("http://www.w3.org/2002/07/owl#")
                || uri.startsWith("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    }
}
