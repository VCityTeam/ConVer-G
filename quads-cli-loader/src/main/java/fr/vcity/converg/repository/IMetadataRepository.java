package fr.vcity.converg.repository;

import fr.vcity.converg.dao.Metadata;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IMetadataRepository extends CrudRepository<Metadata, Integer> {

}
