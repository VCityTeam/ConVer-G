package fr.vcity.sparqltosql.repository;

import fr.vcity.sparqltosql.dao.RDFVersionedNamedGraph;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IVersionedNamedGraphRepository extends CrudRepository<RDFVersionedNamedGraph, Integer> {

}
