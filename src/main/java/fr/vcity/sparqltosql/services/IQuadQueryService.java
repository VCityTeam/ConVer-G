package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad;

import java.util.List;

public interface IQuadQueryService {
    List<RDFCompleteVersionedQuad> query(String queryString);

    void query2(String queryString);

    void importModel(String modelString, String lang);

    void resetDatabase();
}
