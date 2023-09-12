package fr.vcity.sparqltosql.services;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IQuadImportService {

    Integer importModel(List<MultipartFile> files);

    void resetDatabase();
}
