package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.RDFResourceOrLiteral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IRDFResourceOrLiteralRepository extends JpaRepository<RDFResourceOrLiteral, Integer> {
    Optional<RDFResourceOrLiteral> findByNameAndType(String name, String type);
}
