package fr.vcity.converg.repository;

import fr.vcity.converg.dao.RDFVersionedNamedGraph;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IVersionedNamedGraphRepository extends CrudRepository<RDFVersionedNamedGraph, Integer> {

}
