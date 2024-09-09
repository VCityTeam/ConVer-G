package fr.vcity.converg.repository;

import fr.vcity.converg.dao.FlatModelQuad;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IFlatModelQuadRepository extends CrudRepository<FlatModelQuad, Integer> {

}
