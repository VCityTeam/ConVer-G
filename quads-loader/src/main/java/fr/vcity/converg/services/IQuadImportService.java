package fr.vcity.converg.services;

import org.apache.jena.riot.RiotException;
import org.springframework.web.multipart.MultipartFile;

public interface IQuadImportService {

    Integer importModel(MultipartFile file) throws RiotException;

    void flattenVersionedQuads();

    void resetDatabase();

    void importMetadata(MultipartFile file) throws RiotException;

    void removeMetadata();
}
