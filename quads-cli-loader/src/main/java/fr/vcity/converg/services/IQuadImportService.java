package fr.vcity.converg.services;

import org.apache.jena.riot.RiotException;

import java.io.File;
import java.io.FileNotFoundException;

public interface IQuadImportService {

    Integer importModel(File file) throws RiotException;

    void flattenVersionedQuads();

    void resetDatabase();

    void importMetadata(File file) throws RiotException, FileNotFoundException;

    void removeMetadata();
}
