package fr.vcity.sparqltosql.model;

import fr.vcity.sparqltosql.dao.RDFNamedGraph;
import fr.vcity.sparqltosql.dao.RDFResourceOrLiteral;
import lombok.Data;

@Data
public class RDFSavedQuad {
    private RDFNamedGraph savedRDFNamedGraph;
    private RDFResourceOrLiteral savedRDFSubject;
    private RDFResourceOrLiteral savedRDFPredicate;
    private RDFResourceOrLiteral savedRDFObject;

    public RDFSavedQuad(RDFNamedGraph savedRDFNamedGraph, RDFResourceOrLiteral savedRDFSubject, RDFResourceOrLiteral savedRDFPredicate, RDFResourceOrLiteral savedRDFObject) {
        this.savedRDFNamedGraph = savedRDFNamedGraph;
        this.savedRDFSubject = savedRDFSubject;
        this.savedRDFPredicate = savedRDFPredicate;
        this.savedRDFObject = savedRDFObject;
    }
}
