package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.Version;
import fr.vcity.sparqltosql.dto.VersionAncestry;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IVersionRepository extends CrudRepository<Version, Integer> {

    @Query("""
    INSERT INTO version VALUES (DEFAULT, DEFAULT, DEFAULT, :message, DEFAULT, DEFAULT)
    RETURNING *
    """)
    Version save(String message);

    @Query("""
    UPDATE version SET sha_version = (
        SELECT
            md5(
                CAST(:parent AS text) ||
                CAST((array_agg(
                CAST(vq.id_named_graph AS text) ||
                CAST(vq.id_subject AS text) ||
                CAST(vq.id_property AS text) ||
                CAST(vq.id_object AS text)
                ORDER BY id_named_graph, id_subject, id_property, id_object)) AS text))
        FROM
            versioned_quad vq
        WHERE get_bit(vq.validity, :index - 1) = 1
    ), sha_version_parent = :parentNullable
    WHERE index_version = :index
    RETURNING *
    """)
    Version updateVersionSha(String parent, String parentNullable, Integer index);

    @Query("""
        WITH RECURSIVE parents AS
           (SELECT sha_version,
                   sha_version_parent,
                   ARRAY ['']         AS ancestry
            FROM version
            WHERE sha_version_parent IS NULL
            UNION
            SELECT child.sha_version                                       AS id,
                   coalesce(child.sha_version_parent, p.sha_version_parent),
                   array_append(p.ancestry, child.sha_version_parent)             AS ancestry
            FROM version child
                     INNER JOIN parents p ON p.sha_version = child.sha_version_parent)
        SELECT sha_version,
        sha_version_parent,
               ancestry
        FROM parents
    """)
    List<VersionAncestry> getGraphVersion();

    @Query("""
        SELECT sha_version FROM version WHERE index_version = :indexVersion
    """)
    String getHashVersionByIndexVersion(Integer indexVersion);
}
