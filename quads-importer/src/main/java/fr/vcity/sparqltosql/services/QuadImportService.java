package fr.vcity.sparqltosql.services;

import fr.vcity.sparqltosql.dao.ResourceOrLiteral;
import fr.vcity.sparqltosql.dao.Version;
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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuadImportService implements IQuadImportService {

    private record TripleValueType(String sValue, String sType, String pValue, String pType, String oValue,
                                   String oType) {
    }

    ResourceOrLiteral workspaceIsInVersion;
    ResourceOrLiteral workspaceIsVersionOf;

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

        this.workspaceIsInVersion = rdfResourceRepository.save("https://github.com/VCityTeam/SPARQL-to-SQL#is-in-version", null);
        this.workspaceIsVersionOf = rdfResourceRepository.save("https://github.com/VCityTeam/SPARQL-to-SQL#is-version-of", null);
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

            extractAndInsertVersionedNamedGraph(file, dataset, version);
            extractAndInsertQuads(dataset, version);

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
                importDefaultModel(model);
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
        Set<RDFNode> nodeSet = new HashSet<>();
        StringBuilder triplesQuery = new StringBuilder();

        for (StmtIterator stmtIterator = defaultModel.listStatements(); stmtIterator.hasNext(); ) {
            TripleValueType tripleVT = getTripleValueType(stmtIterator, nodeSet);

            triplesQuery.append("(").append(formatStringToInsert(tripleVT.sValue()))
                    .append(",").append(formatStringToInsert(tripleVT.sType()))
                    .append(",").append(formatStringToInsert(tripleVT.pValue()))
                    .append(",").append(formatStringToInsert(tripleVT.pType()))
                    .append(",").append(formatStringToInsert(tripleVT.oValue()))
                    .append(",").append(formatStringToInsert(tripleVT.oType()))
                    .append(")");
            if (stmtIterator.hasNext()) {
                triplesQuery.append(",\n");
            } else {
                triplesQuery.append("\n");
            }
        }

        String rlQuery = nodeSet.stream().map(node -> {
            String nValue = node.isLiteral() ? node.asLiteral().getString() : node.toString();
            String nType = node.isLiteral() ? node.asLiteral().getDatatype().getURI() : null;
            return "(" + formatStringToInsert(nValue) + "," + formatStringToInsert(nType) + ")";
        }).collect(Collectors.joining(",\n"));

        if (!rlQuery.isBlank()) {
            versionedQuadComponent.saveResourceOrLiteral(rlQuery);
        }

        if (!triplesQuery.toString().isBlank()) {
            workspaceComponent.saveTriples(triplesQuery.toString());
        }
    }

    /**
     * Extract and insert the quads
     *
     * @param dataset The dataset
     * @param version The version
     */
    private void extractAndInsertQuads(Dataset dataset, Version version) {
        Set<RDFNode> nodeSet = new HashSet<>();
        StringBuilder quadsQuery = new StringBuilder();

        for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
            Resource namedModel = i.next();
            Model model = dataset.getNamedModel(namedModel);
            log.info("Name Graph : {}", namedModel.getURI());

            for (StmtIterator stmtIterator = model.listStatements(); stmtIterator.hasNext(); ) {
                TripleValueType tripleVT = getTripleValueType(stmtIterator, nodeSet);

                quadsQuery.append("(").append(formatStringToInsert(tripleVT.sValue()))
                        .append(",").append(formatStringToInsert(tripleVT.sType()))
                        .append(",").append(formatStringToInsert(tripleVT.pValue()))
                        .append(",").append(formatStringToInsert(tripleVT.pType()))
                        .append(",").append(formatStringToInsert(tripleVT.oValue()))
                        .append(",").append(formatStringToInsert(tripleVT.oType()))
                        .append(",").append(formatStringToInsert(namedModel.getURI()))
                        .append(",").append(version.getIndexVersion() - 1)
                        .append(")");
                if (stmtIterator.hasNext()) {
                    quadsQuery.append(",\n");
                } else {
                    quadsQuery.append("\n");
                }
            }
        }

        String rlQuery = nodeSet.stream().map(node -> {
            String nValue = node.isLiteral() ? node.asLiteral().getString() : node.toString();
            String nType = node.isLiteral() ? node.asLiteral().getDatatype().getURI() : null;
            return "(" + formatStringToInsert(nValue) + "," + formatStringToInsert(nType) + ")";
        }).collect(Collectors.joining(",\n"));

        if (!rlQuery.isBlank()) {
            versionedQuadComponent.saveResourceOrLiteral(rlQuery);
        }

        if (!quadsQuery.toString().isBlank()) {
            versionedQuadComponent.saveQuads(quadsQuery.toString());
        }
    }

    /**
     * Extract and insert the versioned named graph
     *
     * @param file    The input file
     * @param dataset The dataset
     * @param version The version
     */
    private void extractAndInsertVersionedNamedGraph(MultipartFile file, Dataset dataset, Version version) {
        StringBuilder ngQuery = new StringBuilder();
        for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
            Resource namedModel = i.next();
            ngQuery.append("(").append(formatStringToInsert(namedModel.getURI()))
                    .append(",").append(formatStringToInsert(file.getOriginalFilename()))
                    .append(",").append(version.getIndexVersion() - 1)
                    .append(")\n");
        }

        if (!ngQuery.toString().isBlank()) {
            versionedNamedGraphComponent.saveVersionedNamedGraph(ngQuery.toString());
        }
    }

    /**
     * Format the string to insert
     *
     * @param value The value
     * @return The formatted string
     */
    private String formatStringToInsert(String value) {
        if (value == null)
            return "null";
        return "'" + value + "'";
    }

    /**
     * Get the triple value type
     *
     * @param stmtIterator The statement iterator
     * @param nodeSet      The node set
     * @return The triple value type
     */
    private static TripleValueType getTripleValueType(StmtIterator stmtIterator, Set<RDFNode> nodeSet) {
        Statement statement = stmtIterator.next();
        RDFNode subject = statement.getSubject();
        RDFNode predicate = statement.getPredicate();
        RDFNode object = statement.getObject();

        nodeSet.add(subject);
        nodeSet.add(predicate);
        nodeSet.add(object);

        String sValue = subject.isLiteral() ? subject.asLiteral().getString() : subject.toString();
        String sType = subject.isLiteral() ? subject.asLiteral().getDatatype().getURI() : null;

        String pValue = predicate.isLiteral() ? predicate.asLiteral().getString() : predicate.toString();
        String pType = predicate.isLiteral() ? predicate.asLiteral().getDatatype().getURI() : null;

        String oValue = object.isLiteral() ? object.asLiteral().getString() : object.toString();
        String oType = object.isLiteral() ? object.asLiteral().getDatatype().getURI() : null;
        return new TripleValueType(sValue, sType, pValue, pType, oValue, oType);
    }
}
