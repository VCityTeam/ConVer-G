package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.RDFNamedGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IRDFNamedGraphRepository extends JpaRepository<RDFNamedGraph, Integer> {

    Optional<RDFNamedGraph> findByName(
            String name
    );
}
