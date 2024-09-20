package fr.vcity.converg.services;

import fr.vcity.converg.dao.ResourceOrLiteral;
import fr.vcity.converg.dao.Version;
import fr.vcity.converg.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class QuadImportService implements IQuadImportService {

    public record TripleValueType(String sValue, String sType, String pValue, String pType, String oValue,
                                  String oType) {
    }

    public record QuadValueType(TripleValueType tripleValueType, String namedGraph, Integer version) {
    }

    ResourceOrLiteral metadataIsInVersion;
    ResourceOrLiteral metadataIsVersionOf;
    ResourceOrLiteral defaultGraphURI;

    IFlatModelQuadRepository flatModelQuadRepository;
    IFlatModelTripleRepository flatModelTripleRepository;
    IResourceOrLiteralRepository rdfResourceRepository;
    IVersionedQuadRepository rdfVersionedQuadRepository;
    IMetadataRepository metadataRepository;
    IVersionedNamedGraphRepository rdfVersionedNamedGraphRepository;
    VersionedNamedGraphComponent versionedNamedGraphComponent;
    MetadataComponent metadataComponent;
    IVersionRepository versionRepository;
    VersionedQuadComponent versionedQuadComponent;

    public QuadImportService(
            IFlatModelQuadRepository flatModelQuadRepository,
            IFlatModelTripleRepository flatModelTripleRepository,
            IResourceOrLiteralRepository rdfResourceRepository,
            IVersionedQuadRepository rdfVersionedQuadRepository,
            IMetadataRepository metadataRepository,
            IVersionedNamedGraphRepository rdfVersionedNamedGraphRepository,
            VersionedNamedGraphComponent versionedNamedGraphComponent,
            MetadataComponent metadataComponent,
            IVersionRepository versionRepository,
            VersionedQuadComponent versionedQuadComponent
    ) {
        this.flatModelQuadRepository = flatModelQuadRepository;
        this.flatModelTripleRepository = flatModelTripleRepository;
        this.rdfResourceRepository = rdfResourceRepository;
        this.rdfVersionedQuadRepository = rdfVersionedQuadRepository;
        this.metadataRepository = metadataRepository;
        this.rdfVersionedNamedGraphRepository = rdfVersionedNamedGraphRepository;
        this.versionedNamedGraphComponent = versionedNamedGraphComponent;
        this.metadataComponent = metadataComponent;
        this.versionedQuadComponent = versionedQuadComponent;
        this.versionRepository = versionRepository;

        this.metadataIsInVersion = rdfResourceRepository.save("https://github.com/VCityTeam/ConVer-G/Version#is-in-version", null);
        this.metadataIsVersionOf = rdfResourceRepository.save("https://github.com/VCityTeam/ConVer-G/Version#is-version-of", null);
        this.defaultGraphURI = rdfResourceRepository.save("https://github.com/VCityTeam/ConVer-G/Named-Graph#default-graph", null);
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
            DatasetGraph datasetGraph =
                    RDFParser.create()
                            .source(inputStream)
                            .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file.getOriginalFilename())))
                            .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                            .toDatasetGraph();

            Long start = System.nanoTime();

            extractAndInsertVersionedNamedGraph(file, datasetGraph, version);
            extractAndInsertQuads(datasetGraph, version);

            Long end = System.nanoTime();
            log.info("[Measure] (Import relational internal): {} ns for file: {};", end - start, file.getOriginalFilename());
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
        flatModelQuadRepository.deleteAll();
        flatModelTripleRepository.deleteAll();
        rdfVersionedQuadRepository.deleteAll();
        rdfVersionedNamedGraphRepository.deleteAll();
        versionRepository.deleteAll();
        rdfResourceRepository.save("https://github.com/VCityTeam/ConVer-G/Version#is-in-version", null);
        rdfResourceRepository.save("https://github.com/VCityTeam/ConVer-G/Version#is-version-of", null);
    }

    /**
     * Import RDF statements represented in language <code>of the file extension</code> to the model as valid in the new metadata.
     *
     * @param file The input file
     */
    @Override
    public void importMetadata(MultipartFile file) throws RiotException {
        log.info("Current file: {}", file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            DatasetGraph datasetGraph =
                    RDFParser.create()
                            .source(inputStream)
                            .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(file.getOriginalFilename())))
                            .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                            .toDatasetGraph();

            Long start = System.nanoTime();

            if (!datasetGraph.getDefaultGraph().isEmpty()) {
                importDefaultModel(datasetGraph.getDefaultGraph());
            }

            for (Iterator<Node> i = datasetGraph.listGraphNodes(); i.hasNext(); ) {
                var graphNode = i.next();
                importDefaultModel(datasetGraph.getGraph(graphNode));
            }

            Long end = System.nanoTime();
            log.info("[Measure] (Import relational internal): {} ns for file: {};", end - start, file.getOriginalFilename());
        } catch (RiotException | IOException e) {
            throw new RiotException(e.getMessage());
        }
    }

    /**
     * Reset the metadata.
     */
    public void removeMetadata() {
        metadataRepository.deleteAll();
    }

    /**
     * Condense the dataset
     */
    @Override
    public void condenseModel() {
        Long start = System.nanoTime();
        rdfResourceRepository.flatModelQuadsSubjectToCatalog();
        rdfResourceRepository.flatModelQuadsPredicateToCatalog();
        rdfResourceRepository.flatModelQuadsObjectToCatalog();
        rdfResourceRepository.flatModelTriplesSubjectToCatalog();
        rdfResourceRepository.flatModelTriplesPredicateToCatalog();
        rdfResourceRepository.flatModelTriplesObjectToCatalog();
        rdfVersionedQuadRepository.condenseModel();
        flatModelQuadRepository.deleteAll();
        Long end = System.nanoTime();
        log.info("[Measure] (Catalog + Condense relational internal): {} ns;", end - start);
    }

    /**
     * Import RDF default model statements
     *
     * @param graph The default graph
     */
    private void importDefaultModel(Graph graph) {
        Set<Node> nodeSet = new HashSet<>();
        List<TripleValueType> tripleValueTypes = new ArrayList<>();

        graph.stream()
                .forEach(triple -> tripleValueTypes.add(getTripleValueType(triple, nodeSet)));

        if (!tripleValueTypes.isEmpty()) {
            metadataComponent.saveTriples(tripleValueTypes);
        }

        rdfResourceRepository.flatModelTriplesSubjectToCatalog();
        rdfResourceRepository.flatModelTriplesPredicateToCatalog();
        rdfResourceRepository.flatModelTriplesObjectToCatalog();
    }

    /**
     * Extract and insert the quads
     *
     * @param datasetGraph The datasetGraph
     * @param version      The version
     */
    private void extractAndInsertQuads(DatasetGraph datasetGraph, Version version) {
        Set<Node> nodeSet = new HashSet<>();
        List<QuadValueType> quadValueTypes = new ArrayList<>();

        datasetGraph.stream()
                .forEach(quad -> {
                    QuadValueType quadValueType = new QuadValueType(getTripleValueType(quad.asTriple(), nodeSet), quad.getGraph().getURI(), version.getIndexVersion() - 1);
                    quadValueTypes.add(quadValueType);
                });

        datasetGraph.getDefaultGraph()
                .stream()
                .forEach(triple -> {
                    QuadValueType quadValueType = new QuadValueType(getTripleValueType(triple, nodeSet), defaultGraphURI.getName(), version.getIndexVersion() - 1);
                    quadValueTypes.add(quadValueType);
                });

        if (!quadValueTypes.isEmpty()) {
            versionedQuadComponent.saveQuads(quadValueTypes);
        }
    }

    /**
     * Extract and insert the versioned named graph
     *
     * @param file         The input file
     * @param datasetGraph The datasetGraph
     * @param version      The version
     */
    private void extractAndInsertVersionedNamedGraph(MultipartFile file, DatasetGraph datasetGraph, Version version) {
        List<String> namedGraphs = new ArrayList<>();

        for (Iterator<Node> i = datasetGraph.listGraphNodes(); i.hasNext(); ) {
            var namedModel = i.next();
            namedGraphs.add(namedModel.getURI());
        }

        if (!datasetGraph.getDefaultGraph().isEmpty()) {
            namedGraphs.add(defaultGraphURI.getName());
        }

        if (!namedGraphs.isEmpty()) {
            versionedNamedGraphComponent.saveVersionedNamedGraph(namedGraphs, file.getOriginalFilename(), version.getIndexVersion());
        }
    }

    /**
     * Get the triple value type
     *
     * @param triple  The triple
     * @param nodeSet The node set
     * @return The triple value type
     */
    private static TripleValueType getTripleValueType(Triple triple, Set<Node> nodeSet) {
        var subject = triple.getSubject();
        var predicate = triple.getPredicate();
        var object = triple.getObject();

        nodeSet.add(subject);
        nodeSet.add(predicate);
        nodeSet.add(object);

        String sValue = subject.isLiteral() ? subject.getLiteral().getValue().toString() : subject.toString();
        String sType = subject.isLiteral() ? subject.getLiteral().getDatatype().getURI() : null;

        String pValue = predicate.isLiteral() ? predicate.getLiteral().getValue().toString() : predicate.toString();
        String pType = predicate.isLiteral() ? predicate.getLiteral().getDatatype().getURI() : null;

        String oValue = object.isLiteral() ? object.getLiteral().getValue().toString() : object.toString();
        String oType = object.isLiteral() ? object.getLiteral().getDatatype().getURI() : null;
        return new TripleValueType(sValue, sType, pValue, pType, oValue, oType);
    }
}
