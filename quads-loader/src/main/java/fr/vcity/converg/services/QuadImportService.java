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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
@Slf4j
public class QuadImportService implements IQuadImportService {

    /**
     * Triple Value Type
     *
     * @param sValue the subject value
     * @param sType  the subject type
     * @param pValue the predicate value
     * @param pType  the predicate type
     * @param oValue the object value
     * @param oType  the object type
     */
    public record TripleValueType(String sValue, String sType, String pValue, String pType, String oValue,
                                  String oType) {
    }

    /**
     * Quad Value Type
     *
     * @param tripleValueType the triple (subject, predicate, object) and their types
     * @param namedGraph      the named graph
     * @param version         the version
     */
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

            log.info("Saving subjects quads to catalog");
            rdfResourceRepository.flatModelQuadsSubjectToCatalog();
            log.info("Saving predicates quads to catalog");
            rdfResourceRepository.flatModelQuadsPredicateToCatalog();
            log.info("Saving objects quads to catalog");
            rdfResourceRepository.flatModelQuadsObjectToCatalog();
            log.info("Condensing quads to catalog");
            rdfVersionedQuadRepository.condenseModel();
            flatModelQuadRepository.deleteAll();

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

            log.info("Saving subjects triples to catalog");
            rdfResourceRepository.flatModelTriplesSubjectToCatalog();
            log.info("Saving predicates triples to catalog");
            rdfResourceRepository.flatModelTriplesPredicateToCatalog();
            log.info("Saving object triples to catalog");
            rdfResourceRepository.flatModelTriplesObjectToCatalog();
            flatModelTripleRepository.deleteAll();

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
     * Import RDF default model statements
     *
     * @param graph The default graph
     */
    private void importDefaultModel(Graph graph) {
        List<TripleValueType> tripleValueTypes = new ArrayList<>();

        graph.stream()
                .forEach(triple -> {
                    tripleValueTypes.add(getTripleValueType(triple));

                    if (tripleValueTypes.size() == 50000) {
                        log.info("50000 records found. Executing batch save triples");
                        metadataComponent.saveTriples(tripleValueTypes);
                        tripleValueTypes.clear();
                    }
                });

        if (!tripleValueTypes.isEmpty()) {
            metadataComponent.saveTriples(tripleValueTypes);
        }
    }

    /**
     * Extract and insert the quads
     *
     * @param datasetGraph The datasetGraph
     * @param version      The version
     */
    private void extractAndInsertQuads(DatasetGraph datasetGraph, Version version) {
        List<QuadValueType> quadValueTypes = new ArrayList<>();

        datasetGraph.stream()
                .forEach(quad -> {
                    QuadValueType quadValueType = new QuadValueType(
                            getTripleValueType(
                                    quad.asTriple()
                            ),
                            quad.getGraph().getURI(),
                            version.getIndexVersion() - 1
                    );
                    quadValueTypes.add(quadValueType);

                    if (quadValueTypes.size() == 50000) {
                        log.info("50000 records found. Executing batch save quads");
                        versionedQuadComponent.saveQuads(quadValueTypes);
                        quadValueTypes.clear();
                    }
                });

        datasetGraph.getDefaultGraph()
                .stream()
                .forEach(triple -> {
                    QuadValueType quadValueType = new QuadValueType(getTripleValueType(triple), defaultGraphURI.getName(), version.getIndexVersion() - 1);
                    quadValueTypes.add(quadValueType);

                    if (quadValueTypes.size() == 50000) {
                        log.info("50000 records found. Executing batch save quads");
                        versionedQuadComponent.saveQuads(quadValueTypes);
                        quadValueTypes.clear();
                    }
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
     * @param triple The triple
     * @return The triple value type
     */
    private static TripleValueType getTripleValueType(Triple triple) {
        var subject = triple.getSubject();
        var predicate = triple.getPredicate();
        var object = triple.getObject();

        String sValue = subject.isLiteral() ? subject.getLiteral().getValue().toString() : subject.toString();
        String sType = subject.isLiteral() ? subject.getLiteral().getDatatype().getURI() : null;

        String pValue = predicate.isLiteral() ? predicate.getLiteral().getValue().toString() : predicate.toString();
        String pType = predicate.isLiteral() ? predicate.getLiteral().getDatatype().getURI() : null;

        String oValue = object.isLiteral() ? object.getLiteral().getValue().toString() : object.toString();
        String oType = object.isLiteral() ? object.getLiteral().getDatatype().getURI() : null;
        return new TripleValueType(sValue, sType, pValue, pType, oValue, oType);
    }
}
