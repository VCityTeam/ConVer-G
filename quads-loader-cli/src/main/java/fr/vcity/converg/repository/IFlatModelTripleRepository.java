package fr.vcity.converg.repository;

import fr.vcity.converg.dao.FlatModelTriple;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IFlatModelTripleRepository extends CrudRepository<FlatModelTriple, Integer> {

}
