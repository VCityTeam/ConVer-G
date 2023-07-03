package fr.vcity.sparqltosql.services;

import org.springframework.web.multipart.MultipartFile;

public interface IQuadImportService {

    void importModelToAdd(String modelString, String lang);

    void importModelToRemove(String modelString, String lang);

    void resetDatabase();

    void importModelToRemoveAndAddFile(MultipartFile[] files);
}
