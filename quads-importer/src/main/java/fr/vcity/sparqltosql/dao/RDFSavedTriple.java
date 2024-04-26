package fr.vcity.sparqltosql.dao;

import lombok.Data;

@Data
public class RDFSavedTriple {
    private ResourceOrLiteral savedRDFSubject;
    private ResourceOrLiteral savedRDFPredicate;
    private ResourceOrLiteral savedRDFObject;

    public RDFSavedTriple(ResourceOrLiteral savedRDFSubject, ResourceOrLiteral savedRDFPredicate, ResourceOrLiteral savedRDFObject) {
        this.savedRDFSubject = savedRDFSubject;
        this.savedRDFPredicate = savedRDFPredicate;
        this.savedRDFObject = savedRDFObject;
    }
}
