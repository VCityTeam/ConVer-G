package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.RDFVersionedQuad;
import fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IRDFVersionedQuadRepository extends JpaRepository<RDFVersionedQuad, Integer> {

    @Transactional
    @Query(value = "update versioned_quad v set validity = v.validity || '0' returning length(v.validity)", nativeQuery = true)
    Integer createNewVersion();

    @Query(value = "select v.validity from RDFVersionedQuad v")
    List<String> getVersions();

    Optional<RDFVersionedQuad> findByIdSubjectAndIdPropertyAndIdObjectAndIdNamedGraph(
            Integer idSubject,
            Integer idProperty,
            Integer idObject,
            Integer idNamedGraph
    );

    List<RDFVersionedQuad> findAllByValidity(String validity);

    @Query("""
            SELECT new fr.vcity.sparqltosql.dto.RDFCompleteVersionedQuad(rls.name, rlp.name, rlo.name, ng.name, v.validity)
            FROM RDFVersionedQuad v LEFT JOIN RDFResourceOrLiteral rls ON rls.id = v.idSubject
            LEFT JOIN RDFResourceOrLiteral rlp ON rlp.id = v.idProperty
            LEFT JOIN RDFResourceOrLiteral rlo ON rlo.id = v.idObject
            LEFT JOIN RDFNamedGraph ng ON ng.id = v.idNamedGraph
            WHERE v.validity = :validity""")
    List<RDFCompleteVersionedQuad> findAllByValidityAndJoin(String validity);
}
