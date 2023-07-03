package fr.vcity.sparqltosql.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RDFCompleteVersionedQuad {

    private String s;

    private String p;

    private String o;

    private String namedGraph;

    private byte[] validity;

    public RDFCompleteVersionedQuad(
            String s,
            String p,
            String o,
            String namedGraph,
            byte[] validity
    ) {
        this.s = s;
        this.p = p;
        this.o = o;
        this.namedGraph = namedGraph;
        this.validity = validity;
    }

    public RDFCompleteVersionedQuad() {
    }

}
