package fr.vcity.sparqltosql.services;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IQuadImportService {

    String importModelToAdd(String shaParent, List<MultipartFile> files);

    String importModelToRemove(String shaParent, List<MultipartFile> files);

    String importModelToRemoveAndAdd(String shaParent, List<MultipartFile> files);

    void resetDatabase();
}
