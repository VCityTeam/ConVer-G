package fr.vcity.sparqltosql.services;

import org.springframework.web.multipart.MultipartFile;

public interface IQuadImportService {

    void importModelToAdd(MultipartFile[] files);

    void importModelToRemove(MultipartFile[] files);

    void resetDatabase();

    void importModelToRemoveAndAdd(MultipartFile[] files);
}
