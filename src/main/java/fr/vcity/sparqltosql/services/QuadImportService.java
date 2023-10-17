package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dao.RDFResourceOrLiteral;
import fr.vcity.sparqltosql.dao.RDFVersionedNamedGraph;
import fr.vcity.sparqltosql.dao.Version;
import fr.vcity.sparqltosql.dao.WorkspaceVersion;
import fr.vcity.sparqltosql.model.RDFSavedTriple;
import fr.vcity.sparqltosql.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

@Service
@Slf4j
public class QuadImportService implements IQuadImportService {

    IRDFResourceOrLiteralRepository rdfResourceRepository;
    IRDFVersionedQuadRepository rdfVersionedQuadRepository;
    IVersionedWorkspaceRepository versionedWorkspaceRepository;
    IRDFVersionedNamedGraphRepository rdfVersionedNamedGraphRepository;
    RDFVersionedNamedGraphComponent rdfVersionedNamedGraphComponent;
    VersionedWorkspaceComponent versionedWorkspaceComponent;
    IVersionRepository versionRepository;
    IWorkspaceVersionRepository workspaceVersionRepository;
    RDFVersionedQuadComponent rdfVersionedQuadComponent;

    public QuadImportService(
            IRDFResourceOrLiteralRepository rdfResourceRepository,
            IRDFVersionedQuadRepository rdfVersionedQuadRepository,
            IVersionedWorkspaceRepository versionedWorkspaceRepository,
            IRDFVersionedNamedGraphRepository rdfVersionedNamedGraphRepository,
            RDFVersionedNamedGraphComponent rdfVersionedNamedGraphComponent,
            VersionedWorkspaceComponent versionedWorkspaceComponent,
            IVersionRepository versionRepository,
            IWorkspaceVersionRepository workspaceVersionRepository,
            RDFVersionedQuadComponent rdfVersionedQuadComponent
    ) {
        this.rdfResourceRepository = rdfResourceRepository;
        this.rdfVersionedQuadRepository = rdfVersionedQuadRepository;
        this.versionedWorkspaceRepository = versionedWorkspaceRepository;
        this.rdfVersionedNamedGraphRepository = rdfVersionedNamedGraphRepository;
        this.rdfVersionedNamedGraphComponent = rdfVersionedNamedGraphComponent;
        this.versionedWorkspaceComponent = versionedWorkspaceComponent;
        this.rdfVersionedQuadComponent = rdfVersionedQuadComponent;
        this.versionRepository = versionRepository;
        this.workspaceVersionRepository = workspaceVersionRepository;
    }

    /**
     * Import RDF statements represented in language <code>of the file extension</code> to the model as valid in the new version.
     *
     * @param file The input file
     */
    @Override
    public Integer importModel(MultipartFile file) throws RiotException {
        Version version = versionRepository.save(file.getOriginalFilename());

        log.info("Current file: {}", file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            Dataset dataset =
                    RDFParser.create()
                            .source(inputStream)
                            .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file.getOriginalFilename())))
                            .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                            .toDataset();

            Long start = System.nanoTime();

            if (!dataset.getDefaultModel().listStatements().toList().isEmpty()) {
                importDefaultModel(dataset.getDefaultModel(), version.getIndexVersion(), true);
            }

            for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
                Resource namedModel = i.next();
                Model model = dataset.getNamedModel(namedModel);
                log.debug("Name Graph : {}", namedModel.getURI());
                RDFVersionedNamedGraph savedRDFVersionedNamedGraph = saveRDFNamedGraphOrReturnExisting(namedModel.getURI(), version.getIndexVersion() - 1);

                model.listStatements().toList().parallelStream().forEach(statement -> {
                    RDFSavedTriple rdfSavedTriple = getRDFSavedTriple(statement);

                    rdfVersionedQuadComponent.save(
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
        } catch (RiotException | IOException e) {
            versionRepository.delete(version);
            throw new RiotException(e.getMessage());
        }

        rdfVersionedQuadRepository.updateValidityVersionedQuad();
        rdfVersionedNamedGraphComponent.updateVersionedNamedGraphValidity();

        return version.getIndexVersion();
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
        versionedWorkspaceRepository.deleteAll();
        workspaceVersionRepository.deleteAll();
    }

    /**
     * Import RDF statements represented in language <code>of the file extension</code> to the model as valid in the new workspace.
     *
     * @param file The input file
     */
    @Override
    public Integer importWorkspace(MultipartFile file) throws RiotException {

        WorkspaceVersion workspaceVersion = workspaceVersionRepository.save(file.getOriginalFilename());

        log.info("Current file: {}", file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            Dataset dataset =
                    RDFParser.create()
                            .source(inputStream)
                            .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file.getOriginalFilename())))
                            .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                            .toDataset();

            Long start = System.nanoTime();

            if (!dataset.getDefaultModel().listStatements().toList().isEmpty()) {
                importDefaultModel(dataset.getDefaultModel(), workspaceVersion.getIndexWorkspaceVersion(), false);
            }

            for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
                Resource namedModel = i.next();
                Model model = dataset.getNamedModel(namedModel);
                log.debug("Name Graph : {}", namedModel.getURI());
                saveRDFNamedGraphOrReturnExisting(namedModel.getURI(), workspaceVersion.getIndexWorkspaceVersion() - 1);

                model.listStatements().toList().parallelStream().forEach(statement -> {
                    RDFSavedTriple rdfSavedTriple = getRDFSavedTriple(statement);

                    versionedWorkspaceComponent.save(
                            rdfSavedTriple.getSavedRDFSubject().getIdResourceOrLiteral(),
                            rdfSavedTriple.getSavedRDFPredicate().getIdResourceOrLiteral(),
                            rdfSavedTriple.getSavedRDFObject().getIdResourceOrLiteral(),
                            workspaceVersion.getIndexWorkspaceVersion() - 1
                    );
                });
            }

            Long end = System.nanoTime();
            log.info("Time of execution: {} ns for file: {}", end - start, file.getOriginalFilename());
        } catch (RiotException | IOException e) {
            workspaceVersionRepository.delete(workspaceVersion);
            throw new RiotException(e.getMessage());
        }

        versionedWorkspaceRepository.updateValidityVersionedWorkspace();

        return workspaceVersion.getIndexWorkspaceVersion();
    }

    /**
     * Reset the workspace.
     */
    public void removeWorkspace() {
        versionedWorkspaceRepository.deleteAll();
        workspaceVersionRepository.deleteAll();
    }

    /**
     * Import RDF default model statements
     *
     * @param defaultModel The default graph
     * @param indexBS      The size of the bit string
     */
    private void importDefaultModel(Model defaultModel, Integer indexBS, boolean isVersion) {
        RDFVersionedNamedGraph savedRDFVersionedNamedGraph = saveRDFNamedGraphOrReturnExisting("default", indexBS - 1);

        defaultModel.listStatements().toList().parallelStream().forEach(statement -> {
            RDFSavedTriple rdfSavedTriple = getRDFSavedTriple(statement);

            if (isVersion) {
                rdfVersionedQuadComponent.save(
                        rdfSavedTriple.getSavedRDFSubject().getIdResourceOrLiteral(),
                        rdfSavedTriple.getSavedRDFPredicate().getIdResourceOrLiteral(),
                        rdfSavedTriple.getSavedRDFObject().getIdResourceOrLiteral(),
                        savedRDFVersionedNamedGraph.getIdNamedGraph(),
                        indexBS - 1
                );
            } else {
                versionedWorkspaceComponent.save(
                        rdfSavedTriple.getSavedRDFSubject().getIdResourceOrLiteral(),
                        rdfSavedTriple.getSavedRDFPredicate().getIdResourceOrLiteral(),
                        rdfSavedTriple.getSavedRDFObject().getIdResourceOrLiteral(),
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
}
