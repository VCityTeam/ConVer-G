package fr.vcity.sparqltosql.services;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IQuadImportService {

    void importModelToAdd(List<MultipartFile> files);

    void importModelToRemove(List<MultipartFile> files);

    void resetDatabase();

    void importModelToRemoveAndAdd(List<MultipartFile> files);
}
