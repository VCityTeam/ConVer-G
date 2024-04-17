package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dao.RDFVersionedNamedGraph;
import fr.vcity.sparqltosql.dao.ResourceOrLiteral;
import fr.vcity.sparqltosql.dao.Version;
import fr.vcity.sparqltosql.model.RDFSavedTriple;
import fr.vcity.sparqltosql.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Iterator;

@Service
@Slf4j
public class QuadImportService implements IQuadImportService {

    ResourceOrLiteral WORKSPACE_IS_IN_VERSION;
    ResourceOrLiteral WORKSPACE_IS_VERSION_OF;
    String WORKSPACE_VERSION_PREFIX = "https://github.com/VCityTeam/SPARQL-to-SQL/Version#";
    String WORKSPACE_VNG_PREFIX = "https://github.com/VCityTeam/SPARQL-to-SQL/Versioned-Named-Graph#";

    IResourceOrLiteralRepository rdfResourceRepository;
    IVersionedQuadRepository rdfVersionedQuadRepository;
    IWorkspaceRepository workspaceRepository;
    IVersionedNamedGraphRepository rdfVersionedNamedGraphRepository;
    VersionedNamedGraphComponent versionedNamedGraphComponent;
    WorkspaceComponent workspaceComponent;
    IVersionRepository versionRepository;
    VersionedQuadComponent versionedQuadComponent;

    public QuadImportService(
            IResourceOrLiteralRepository rdfResourceRepository,
            IVersionedQuadRepository rdfVersionedQuadRepository,
            IWorkspaceRepository workspaceRepository,
            IVersionedNamedGraphRepository rdfVersionedNamedGraphRepository,
            VersionedNamedGraphComponent versionedNamedGraphComponent,
            WorkspaceComponent workspaceComponent,
            IVersionRepository versionRepository,
            VersionedQuadComponent versionedQuadComponent
    ) {
        this.rdfResourceRepository = rdfResourceRepository;
        this.rdfVersionedQuadRepository = rdfVersionedQuadRepository;
        this.workspaceRepository = workspaceRepository;
        this.rdfVersionedNamedGraphRepository = rdfVersionedNamedGraphRepository;
        this.versionedNamedGraphComponent = versionedNamedGraphComponent;
        this.workspaceComponent = workspaceComponent;
        this.versionedQuadComponent = versionedQuadComponent;
        this.versionRepository = versionRepository;

        this.WORKSPACE_IS_IN_VERSION = rdfResourceRepository.save("https://github.com/VCityTeam/SPARQL-to-SQL#is-in-version", null);
        this.WORKSPACE_IS_VERSION_OF = rdfResourceRepository.save("https://github.com/VCityTeam/SPARQL-to-SQL#is-version-of", null);
    }

    /**
     * Import RDF statements represented in language <code>of the file extension</code> to the model as valid in the new version.
     *
     * @param file The input file
     */
    @Override
    public Integer importModel(MultipartFile file) throws RiotException {
        LocalDateTime startTransactionTime = LocalDateTime.now();
        Version version = versionRepository.save(file.getOriginalFilename(), startTransactionTime);

        log.info("Current file: {}", file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            Dataset dataset =
                    RDFParser.create()
                            .source(inputStream)
                            .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file.getOriginalFilename())))
                            .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                            .toDataset();

            Long start = System.nanoTime();

            for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
                Resource namedModel = i.next();
                Model model = dataset.getNamedModel(namedModel);
                log.info("Name Graph : {}", namedModel.getURI());
                ResourceOrLiteral resourceOrLiteralNG = saveRDFResourceOrLiteralOrReturnExisting(namedModel, "Named Graph");
                ResourceOrLiteral resourceOrLiteralVNG = saveRDFResourceOrLiteralOrReturnExisting(
                        model.createResource(generateVersionedNamedGraph(namedModel.getURI(), file.getOriginalFilename())),
                        "Named Graph"
                );
                ResourceOrLiteral resourceOrLiteralVersion = saveRDFResourceOrLiteralOrReturnExisting(
                        model.createResource(generateVersionURI(file.getOriginalFilename())),
                        "Version"
                );
                saveRDFNamedGraphOrReturnExisting(
                        resourceOrLiteralVNG.getIdResourceOrLiteral(),
                        version.getIndexVersion() - 1,
                        resourceOrLiteralNG.getIdResourceOrLiteral()
                );

                saveVersionedNameGraphInsideWorkspace(
                        resourceOrLiteralVNG.getIdResourceOrLiteral(),
                        resourceOrLiteralVersion.getIdResourceOrLiteral(),
                        resourceOrLiteralNG.getIdResourceOrLiteral()
                );

                model.listStatements().toList().parallelStream().forEach(statement -> {
                    RDFSavedTriple rdfSavedTriple = saveRDFTripleOrReturnExisting(statement);

                    versionedQuadComponent.save(
                            rdfSavedTriple.getSavedRDFSubject().getIdResourceOrLiteral(),
                            rdfSavedTriple.getSavedRDFPredicate().getIdResourceOrLiteral(),
                            rdfSavedTriple.getSavedRDFObject().getIdResourceOrLiteral(),
                            resourceOrLiteralNG.getIdResourceOrLiteral(),
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
        LocalDateTime endTransactionTime = LocalDateTime.now();

        versionRepository.insertEndTransactionTime(version.getIndexVersion(), endTransactionTime);
        return version.getIndexVersion();
    }

    /**
     * Deletes all the elements inside the database
     */
    @Override
    public void resetDatabase() {
        rdfVersionedQuadRepository.deleteAll();
        rdfVersionedNamedGraphRepository.deleteAll();
        versionRepository.deleteAll();
        rdfResourceRepository.save("https://github.com/VCityTeam/SPARQL-to-SQL#is-in-version", null);
        rdfResourceRepository.save("https://github.com/VCityTeam/SPARQL-to-SQL#is-version-of", null);
    }

    /**
     * Import RDF statements represented in language <code>of the file extension</code> to the model as valid in the new workspace.
     *
     * @param file The input file
     */
    @Override
    public void importWorkspace(MultipartFile file) throws RiotException {
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
                importDefaultModel(dataset.getDefaultModel());
            }

            for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
                Resource namedModel = i.next();
                Model model = dataset.getNamedModel(namedModel);
                model.listStatements().toList().parallelStream().forEach(statement -> {
                    RDFSavedTriple rdfSavedTriple = saveRDFTripleOrReturnExisting(statement);

                    try {
                        workspaceComponent.save(
                                rdfSavedTriple.getSavedRDFSubject().getIdResourceOrLiteral(),
                                rdfSavedTriple.getSavedRDFPredicate().getIdResourceOrLiteral(),
                                rdfSavedTriple.getSavedRDFObject().getIdResourceOrLiteral()
                        );
                    } catch (DuplicateKeyException duplicate) {
                        log.info(duplicate.getMessage());
                    }
                });
            }

            Long end = System.nanoTime();
            log.info("Time of execution: {} ns for file: {}", end - start, file.getOriginalFilename());
        } catch (RiotException | IOException e) {
            throw new RiotException(e.getMessage());
        }
    }

    /**
     * Reset the workspace.
     */
    public void removeWorkspace() {
        workspaceRepository.deleteAll();
    }

    /**
     * Import RDF default model statements
     *
     * @param defaultModel The default graph
     */
    private void importDefaultModel(Model defaultModel) {
        defaultModel.listStatements().toList().parallelStream().forEach(statement -> {
            RDFSavedTriple rdfSavedTriple = saveRDFTripleOrReturnExisting(statement);

            workspaceComponent.save(
                    rdfSavedTriple.getSavedRDFSubject().getIdResourceOrLiteral(),
                    rdfSavedTriple.getSavedRDFPredicate().getIdResourceOrLiteral(),
                    rdfSavedTriple.getSavedRDFObject().getIdResourceOrLiteral()
            );
        });
    }

    /**
     * Saves the subject, the property, the object and the named graph inside the database if they exist else returning them
     *
     * @param statement The statement
     * @return The saved or existing Quad
     */
    private RDFSavedTriple saveRDFTripleOrReturnExisting(Statement statement) {
        RDFNode subject = statement.getSubject();
        RDFNode predicate = statement.getPredicate();
        RDFNode object = statement.getObject();

        ResourceOrLiteral savedRDFSubject = saveRDFResourceOrLiteralOrReturnExisting(subject, "Subject");
        ResourceOrLiteral savedRDFPredicate = saveRDFResourceOrLiteralOrReturnExisting(predicate, "Predicate");
        ResourceOrLiteral savedRDFObject = saveRDFResourceOrLiteralOrReturnExisting(object, "Object");

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
     * @param idVersionedNamedGraph The RDF versioned named graph id
     * @param index                 The number of the associated version
     * @param idNamedGraph          The RDF named graph URI id
     * @return The saved or existing RDFNamedGraph element
     */
    private RDFVersionedNamedGraph saveRDFNamedGraphOrReturnExisting(Integer idVersionedNamedGraph, Integer index, Integer idNamedGraph) {
        log.info("Upsert named graph: {} in version {}. versionedId : {}", idNamedGraph, index, idVersionedNamedGraph);
        return versionedNamedGraphComponent.save(idVersionedNamedGraph, index, idNamedGraph);
    }

    /**
     * Saves the RDF Named Graph, Versioned named graph and version inside the workspace
     *
     * @param idVNG        The RDF versioned named graph id
     * @param idURIVersion The RDF version id
     * @param idNG         The RDF named graph id
     */
    private void saveVersionedNameGraphInsideWorkspace(Integer idVNG, Integer idURIVersion, Integer idNG) {
        log.info("Upsert named graph inside workspace: VNG: {}, in version: {}, versionOfNamedGraph : {}", idVNG, idURIVersion, idNG);
        workspaceComponent.save(idVNG, this.WORKSPACE_IS_VERSION_OF.getIdResourceOrLiteral(), idNG);
        workspaceComponent.save(idVNG, this.WORKSPACE_IS_IN_VERSION.getIdResourceOrLiteral(), idURIVersion);
    }

    /**
     * Saves and return the RDF node inside the database if it doesn't exist, else returns the existing one.
     *
     * @param spo  The RDF node
     * @param type The RDF node type (logging purpose)
     * @return The saved or existing RDFResourceOrLiteral element
     */
    private ResourceOrLiteral saveRDFResourceOrLiteralOrReturnExisting(RDFNode spo, String type) {
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
     * Generates the versioned named graph URI
     *
     * @param namedGraphURI The named graph URI
     * @param filename      The filename
     * @return The versioned graph URI
     */
    private String generateVersionedNamedGraph(String namedGraphURI, String filename) {
        return WORKSPACE_VNG_PREFIX +
                DigestUtils.sha256Hex(
                        StringUtils.getBytesUtf8(namedGraphURI + filename)
                );
    }

    /**
     * Generates the version URI
     *
     * @param originalFilename The filename
     * @return The version URI
     */
    private String generateVersionURI(String originalFilename) {
        return WORKSPACE_VERSION_PREFIX + originalFilename;
    }
}
