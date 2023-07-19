package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dao.Version;
import fr.vcity.sparqltosql.dao.RDFVersionedNamedGraph;
import fr.vcity.sparqltosql.dao.RDFResourceOrLiteral;
import fr.vcity.sparqltosql.exceptions.FileException;
import fr.vcity.sparqltosql.model.RDFSavedTriple;
import fr.vcity.sparqltosql.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuadImportService implements IQuadImportService {

    IRDFResourceOrLiteralRepository rdfResourceRepository;
    IRDFVersionedQuadRepository rdfVersionedQuadRepository;
    IRDFVersionedNamedGraphRepository rdfVersionedNamedGraphRepository;
    RDFVersionedNamedGraphComponent rdfVersionedNamedGraphComponent;
    IVersionRepository versionRepository;
    RDFVersionedQuadComponent rdfVersionedQuadComponent;

    public QuadImportService(
            IRDFResourceOrLiteralRepository rdfResourceRepository,
            IRDFVersionedQuadRepository rdfVersionedQuadRepository,
            IRDFVersionedNamedGraphRepository rdfVersionedNamedGraphRepository,
            RDFVersionedNamedGraphComponent rdfVersionedNamedGraphComponent,
            IVersionRepository versionRepository,
            RDFVersionedQuadComponent rdfVersionedQuadComponent
    ) {
        this.rdfResourceRepository = rdfResourceRepository;
        this.rdfVersionedQuadRepository = rdfVersionedQuadRepository;
        this.rdfVersionedNamedGraphRepository = rdfVersionedNamedGraphRepository;
        this.rdfVersionedNamedGraphComponent = rdfVersionedNamedGraphComponent;
        this.rdfVersionedQuadComponent = rdfVersionedQuadComponent;
        this.versionRepository = versionRepository;
    }

    /**
     * Import RDF statements represented in language <code>lang</code> to the model as valid in the new version.
     * <br />Predefined values for <code>lang</code> are "TRIG" and "NQUADS"
     *
     * @param files The input files
     */
    @Override
    public void importModelToAdd(List<MultipartFile> files) {
        List<MultipartFile> fileList = files
                .stream()
                .filter(file -> !file.isEmpty())
                .collect(Collectors.toList());
        Version version = versionRepository.save(summarizeImport(fileList, "add"));

        fileList.forEach(file -> {
            log.info("Current file: {}", file.getOriginalFilename());

            try (InputStream inputStream = file.getInputStream()) {
                Dataset dataset =
                        RDFParser.create()
                                .source(inputStream)
                                .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file.getOriginalFilename())))
                                .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                                .toDataset();

                Long start = System.nanoTime();

                if (dataset.getDefaultModel().listStatements().toList().size() > 0) {
                    importDefaultModel(dataset.getDefaultModel(), "add", version.getIndexVersion());
                }

                for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
                    Resource namedModel = i.next();
                    Model model = dataset.getNamedModel(namedModel);
                    log.debug("Name Graph : {}", namedModel.getURI());
                    RDFVersionedNamedGraph savedRDFVersionedNamedGraph = saveRDFNamedGraphOrReturnExisting(namedModel.getURI(), version.getIndexVersion() - 1);

                    model.listStatements().toList().parallelStream().forEach(statement -> {
                        RDFSavedTriple rdfSavedTriple = getRDFSavedTriple(statement);

                        rdfVersionedQuadComponent.saveAdd(
                                rdfSavedTriple.getSavedRDFSubject().getIdResourceOrLiteral(),
                                rdfSavedTriple.getSavedRDFPredicate().getIdResourceOrLiteral(),
                                rdfSavedTriple.getSavedRDFObject().getIdResourceOrLiteral(),
                                savedRDFVersionedNamedGraph.getIdNamedGraph(),
                                version.getIndexVersion() - 1
                        );
                    });
                }

                Long end = System.nanoTime();
                log.info("Time of execution: {} ns for file: {}", end - start, file.getOriginalFilename());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        rdfVersionedQuadRepository.updateValidityVersionedQuad();
        rdfVersionedNamedGraphComponent.updateVersionedNamedGraphValidity();
    }

    /**
     * Import RDF statements represented in language <code>lang</code> to the model as not valid in the new version.
     * <br />Predefined values for <code>lang</code> are "TRIG" and "NQUADS"
     *
     * @param files The input files
     */
    @Override
    public void importModelToRemove(List<MultipartFile> files) {
        List<MultipartFile> fileList = files
                .stream()
                .filter(file -> !file.isEmpty())
                .collect(Collectors.toList());
        Version version = versionRepository.save(summarizeImport(fileList, "remove"));

        fileList.forEach(file -> {
            log.info("Current file: {}", file.getOriginalFilename());

            try (InputStream inputStream = file.getInputStream()) {
                Dataset dataset =
                        RDFParser.create()
                                .source(inputStream)
                                .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file.getOriginalFilename())))
                                .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                                .toDataset();

                if (dataset.getDefaultModel().listStatements().toList().size() > 0) {
                    importDefaultModel(dataset.getDefaultModel(), "remove", version.getIndexVersion());
                }

                for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
                    Resource namedModel = i.next();
                    Model model = dataset.getNamedModel(namedModel);
                    log.debug("Name Graph : {}", namedModel.getURI());
                    RDFVersionedNamedGraph savedRDFVersionedNamedGraph = saveRDFNamedGraphOrReturnExisting(namedModel.getURI(), version.getIndexVersion() - 1);

                    model.listStatements().toList().parallelStream().forEach(statement -> {
                        RDFSavedTriple rdfSavedTriple = getRDFSavedTriple(statement);

                        rdfVersionedQuadComponent.saveRemove(
                                rdfSavedTriple.getSavedRDFSubject().getIdResourceOrLiteral(),
                                rdfSavedTriple.getSavedRDFPredicate().getIdResourceOrLiteral(),
                                rdfSavedTriple.getSavedRDFObject().getIdResourceOrLiteral(),
                                savedRDFVersionedNamedGraph.getIdNamedGraph(),
                                version.getIndexVersion() - 1
                        );
                    });
                }


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        rdfVersionedQuadRepository.updateValidityVersionedQuad();
        rdfVersionedNamedGraphComponent.updateVersionedNamedGraphValidity();
    }

    /**
     * Import RDF statements inside the <code>files</code>
     *
     * @param files The input files
     */
    @Override
    public void importModelToRemoveAndAdd(List<MultipartFile> files) {
        List<MultipartFile> fileList = files
                .stream()
                .filter(file -> !file.isEmpty())
                .sorted(getRemoveAddFileComparator())
                .collect(Collectors.toList());
        Version version = versionRepository.save(summarizeImport(fileList, "remove-add"));

        fileList.forEach(file -> {
            log.info("Current file: {}", file.getOriginalFilename());

            try (InputStream inputStream = file.getInputStream()) {
                Dataset dataset =
                        RDFParser.create()
                                .source(inputStream)
                                .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file.getOriginalFilename())))
                                .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                                .toDataset();
                if (dataset.getDefaultModel().listStatements().toList().size() > 0) {
                    if (file.getOriginalFilename().contains("add")) {
                        importDefaultModel(dataset.getDefaultModel(), "add", version.getIndexVersion());
                    } else if (file.getOriginalFilename().contains("remove")) {
                        importDefaultModel(dataset.getDefaultModel(), "remove", version.getIndexVersion());
                    } else {
                        throw new FileException("The file: " + file.getOriginalFilename() + " doesn't contain 'add' or 'remove'");
                    }
                }

                for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
                    Resource namedModel = i.next();
                    Model model = dataset.getNamedModel(namedModel);
                    log.debug("Name Graph : {}", namedModel.getURI());
                    RDFVersionedNamedGraph savedRDFVersionedNamedGraph = saveRDFNamedGraphOrReturnExisting(namedModel.getURI(), version.getIndexVersion() - 1);

                    model.listStatements().toList().parallelStream().forEach(statement -> {
                        RDFSavedTriple rdfSavedTriple = getRDFSavedTriple(statement);

                        if (file.getOriginalFilename().contains("add")) {
                            rdfVersionedQuadComponent.saveAdd(
                                    rdfSavedTriple.getSavedRDFSubject().getIdResourceOrLiteral(),
                                    rdfSavedTriple.getSavedRDFPredicate().getIdResourceOrLiteral(),
                                    rdfSavedTriple.getSavedRDFObject().getIdResourceOrLiteral(),
                                    savedRDFVersionedNamedGraph.getIdNamedGraph(),
                                    version.getIndexVersion() - 1
                            );
                        } else {
                            rdfVersionedQuadComponent.saveRemove(
                                    rdfSavedTriple.getSavedRDFSubject().getIdResourceOrLiteral(),
                                    rdfSavedTriple.getSavedRDFPredicate().getIdResourceOrLiteral(),
                                    rdfSavedTriple.getSavedRDFObject().getIdResourceOrLiteral(),
                                    savedRDFVersionedNamedGraph.getIdNamedGraph(),
                                    version.getIndexVersion() - 1
                            );
                        }
                    });
                }
            } catch (IOException e) {
                throw new FileException("Failed to store file.", e);
            }
        });

        rdfVersionedQuadRepository.updateValidityVersionedQuad();
        rdfVersionedNamedGraphComponent.updateVersionedNamedGraphValidity();
    }

    /**
     * Deletes all the elements inside the database
     */
    @Override
    public void resetDatabase() {
        rdfVersionedQuadRepository.deleteAll();
        rdfResourceRepository.deleteAll();
        rdfVersionedNamedGraphRepository.deleteAll();
        versionRepository.deleteAll();
    }

    /**
     * Summarize the new version
     *
     * @param fileList   The non empty files
     * @param actionType the action type
     * @return The computed summary of the import
     */
    private String summarizeImport(List<MultipartFile> fileList, String actionType) {
        return String.format("%s: [%s]", actionType, fileList
                .stream()
                .map(multipartFile -> "(" + multipartFile.getOriginalFilename() + ")")
                .collect(Collectors.joining(",")));
    }

    /**
     * Import RDF default model statements
     *
     * @param defaultModel The default graph
     * @param action       The action (add or remove)
     * @param indexBS      The size of the bit string
     */
    private void importDefaultModel(Model defaultModel, String action, Integer indexBS) {
        RDFVersionedNamedGraph savedRDFVersionedNamedGraph = saveRDFNamedGraphOrReturnExisting("default", indexBS - 1);

        defaultModel.listStatements().toList().parallelStream().forEach(statement -> {
            RDFSavedTriple rdfSavedTriple = getRDFSavedTriple(statement);

            if (action.equals("remove")) {
                rdfVersionedQuadComponent.saveRemove(
                        rdfSavedTriple.getSavedRDFSubject().getIdResourceOrLiteral(),
                        rdfSavedTriple.getSavedRDFPredicate().getIdResourceOrLiteral(),
                        rdfSavedTriple.getSavedRDFObject().getIdResourceOrLiteral(),
                        savedRDFVersionedNamedGraph.getIdNamedGraph(),
                        indexBS - 1
                );
            } else {
                rdfVersionedQuadComponent.saveAdd(
                        rdfSavedTriple.getSavedRDFSubject().getIdResourceOrLiteral(),
                        rdfSavedTriple.getSavedRDFPredicate().getIdResourceOrLiteral(),
                        rdfSavedTriple.getSavedRDFObject().getIdResourceOrLiteral(),
                        savedRDFVersionedNamedGraph.getIdNamedGraph(),
                        indexBS - 1
                );
            }
        });
    }

    /**
     * Saves the subject, the property, the object and the named graph inside the database if they exist else returning them
     *
     * @param statement The statement
     * @return The saved or existing Quad
     */
    private RDFSavedTriple getRDFSavedTriple(Statement statement) {
        RDFNode subject = statement.getSubject();
        RDFNode predicate = statement.getPredicate();
        RDFNode object = statement.getObject();

        RDFResourceOrLiteral savedRDFSubject = saveRDFResourceOrLiteralOrReturnExisting(subject, "Subject");
        RDFResourceOrLiteral savedRDFPredicate = saveRDFResourceOrLiteralOrReturnExisting(predicate, "Predicate");
        RDFResourceOrLiteral savedRDFObject = saveRDFResourceOrLiteralOrReturnExisting(object, "Object");

        log.debug("Insert or updated (S: {}, P: {}, O: {})",
                savedRDFSubject.getName(),
                savedRDFPredicate.getName(),
                savedRDFObject.getName()
        );

        return new RDFSavedTriple(savedRDFSubject, savedRDFPredicate, savedRDFObject);
    }

    /**
     * Saves and return the RDF Named Graph inside the database if it doesn't exist, else returns the existing one.
     *
     * @param uri The RDF named graph URI
     * @return The saved or existing RDFNamedGraph element
     */
    private RDFVersionedNamedGraph saveRDFNamedGraphOrReturnExisting(String uri, Integer length) {
        log.debug("Upsert named graph: {}", uri);
        return rdfVersionedNamedGraphComponent.save(uri, length);
    }

    /**
     * Saves and return the RDF node inside the database if it doesn't exist, else returns the existing one.
     *
     * @param spo  The RDF node
     * @param type The RDF node type (logging purpose)
     * @return The saved or existing RDFResourceOrLiteral element
     */
    private RDFResourceOrLiteral saveRDFResourceOrLiteralOrReturnExisting(RDFNode spo, String type) {
        if (spo.isLiteral()) {
            Literal literal = spo.asLiteral();
            String literalValue = literal.getString();

            log.debug("Upsert returning {} literal: {}", type, literalValue);
            return rdfResourceRepository.save(literalValue, spo.asLiteral().getDatatype().getURI());
        } else {

            log.debug("Upsert {} resource: {}", type, spo);
            return rdfResourceRepository.save(spo.toString(), null);
        }
    }

    /**
     * Compares two multipartfile (sorting "remove" first and "add" then)
     *
     * @return The comparison
     */
    private static Comparator<MultipartFile> getRemoveAddFileComparator() {
        return (o1, o2) -> {
            if (o1.getOriginalFilename() == null || o2.getOriginalFilename() == null) {
                throw new FileException("The filename is broken");
            }

            if (o1.getOriginalFilename().contains("remove") && o2.getOriginalFilename().contains("remove")) {
                return 0;
            } else if (o1.getOriginalFilename().contains("add") && o2.getOriginalFilename().contains("add")) {
                return 0;
            } else if (o1.getOriginalFilename().contains("remove") && o2.getOriginalFilename().contains("add")) {
                return 1;
            }
            return -1;
        };
    }
}
