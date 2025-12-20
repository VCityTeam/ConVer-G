package fr.vcity.converg.services;

import fr.vcity.converg.dao.ResourceOrLiteral;
import fr.vcity.converg.dao.Version;
import fr.vcity.converg.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.jena.sparql.core.Quad;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    ResourceOrLiteral metadataLocation;
    ResourceOrLiteral metadataIsVersionOf;
    ResourceOrLiteral defaultGraphURI;
    ResourceOrLiteral rdfTypeURI;

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

    List<QuadValueType> quadValueTypes = new ArrayList<>();
    Set<String> namedGraphs = new HashSet<>();
    List<TripleValueType> tripleValueTypes = new ArrayList<>();

    final String PROVO_PREFIX = "http://www.w3.org/ns/prov#";
    final String RDF_PREFIX = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    final String NAMED_GRAPH_PREFIX = "https://github.com/VCityTeam/ConVer-G/Named-Graph#";
    final String VERSION_PREFIX = "https://github.com/VCityTeam/ConVer-G/Version#";

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

        this.metadataIsInVersion = rdfResourceRepository.save(PROVO_PREFIX + "atLocation", null);
        this.metadataLocation = rdfResourceRepository.save(PROVO_PREFIX + "Location", null);
        this.metadataIsVersionOf = rdfResourceRepository.save(PROVO_PREFIX + "specializationOf", null);
        this.metadataIsVersionOf = rdfResourceRepository.save(PROVO_PREFIX + "Entity", null);
        this.defaultGraphURI = rdfResourceRepository.save(NAMED_GRAPH_PREFIX + "default-graph", null);
        this.rdfTypeURI = rdfResourceRepository.save(RDF_PREFIX + "type", null);
    }

    /**
     * Import RDF statements represented in language <code>of the file extension</code> to the model as valid in the new version.
     *
     * @param file The input file
     */
    @Override
    public Integer importModel(MultipartFile file) throws RiotException {
        LocalDateTime startTransactionTime = LocalDateTime.now();
        String filename = file.getOriginalFilename();
        Version version = versionRepository.save(VERSION_PREFIX + filename, startTransactionTime);

        log.info("Current file: {}", filename);

        try (InputStream inputStream = file.getInputStream()) {
            Long start = System.nanoTime();

            Long startBatching = System.nanoTime();
            getQuadsStreamRDF(inputStream, filename, version.getIndexVersion())
                    .finish();
            Long endBatching = System.nanoTime();
            log.info("[Measure] (Batching): {} ns for file: {};", endBatching - startBatching, file.getOriginalFilename());

            log.info("Saving quads to catalog");
            Long startCatalog = System.nanoTime();
            rdfResourceRepository.flatModelQuadsToCatalog();
            Long endCatalog = System.nanoTime();
            log.info("[Measure] (Catalog): {} ns for file: {};", endCatalog - startCatalog, file.getOriginalFilename());

            log.info("Condensing quads to catalog");
            Long startCondensing = System.nanoTime();
            String newBitMask = "0".repeat(version.getIndexVersion() - 1) + "1";
            rdfVersionedQuadRepository.condenseModel(newBitMask);
            rdfVersionedQuadRepository.updateValidityVersionedQuad(version.getIndexVersion() - 1);
            Long endCondensing = System.nanoTime();
            log.info("[Measure] (Condensing): {} ns for file: {};", endCondensing - startCondensing, file.getOriginalFilename());

            flatModelQuadRepository.deleteAll();

            Long end = System.nanoTime();
            log.info("[Measure] (Import relational internal): {} ns for file: {};", end - start, file.getOriginalFilename());
        } catch (RiotException | IOException e) {
            versionRepository.delete(version);
            throw new RiotException(e.getMessage());
        }

        LocalDateTime endTransactionTime = LocalDateTime.now();
        versionRepository.insertEndTransactionTime(version.getIndexVersion(), endTransactionTime);

        return version.getIndexVersion();
    }


    @Override
    public void flattenVersionedQuads() {
        log.info("Flattening versioned quads");

        Long start = System.nanoTime();
        versionedQuadComponent.flattenVersionedQuads();

        rdfVersionedQuadRepository.deleteAll();
        Long end = System.nanoTime();

        log.info("[Measure] (Flattening and delete): {} ns", end - start);
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
            Long start = System.nanoTime();

            getTriplesStreamRDF(inputStream, file.getOriginalFilename())
                    .finish();

            log.info("Saving triples to catalog");
            rdfResourceRepository.flatModelTriplesToCatalog();
            metadataRepository.insertMetadataTriples();
            flatModelTripleRepository.deleteAll();

            Long end = System.nanoTime();
            log.info("[Measure] (Import relational internal): {} ns for file: {};", end - start, file.getOriginalFilename());
        } catch (RiotException | IOException e) {
            throw new RiotException(e.getMessage());
        }
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
        rdfResourceRepository.save(PROVO_PREFIX + "atLocation", null);
        rdfResourceRepository.save(PROVO_PREFIX + "specializationOf", null);
        rdfResourceRepository.save("https://github.com/VCityTeam/ConVer-G/Named-Graph#default-graph", null);
        rdfResourceRepository.save(RDF_PREFIX + "type", null);
    }

    /**
     * Reset the metadata.
     */
    public void removeMetadata() {
        metadataRepository.deleteAll();
    }

    /**
     * Save the triples in batch
     */
    private void saveBatchTriples() {
        metadataComponent.saveTriples(tripleValueTypes);
        tripleValueTypes.clear();
    }

    /**
     * Save the quads in batch
     */
    private void saveBatchQuads() {
        versionedQuadComponent.saveQuads(quadValueTypes);
        quadValueTypes.clear();
    }

    /**
     * Save the versioned named graph in batch
     *
     * @param filename The input filename
     * @param version  The version number
     */
    private void saveBatchVersionedNamedGraph(String filename, Integer version) {
        versionedNamedGraphComponent.saveVersionedNamedGraph(namedGraphs.stream().toList(), filename, version);
        namedGraphs.clear();
    }

    /**
     * Converts a stream into a stream of quads
     *
     * @param in The input stream of the dataset
     * @return A stream of quads
     */
    private StreamRDF getQuadsStreamRDF(InputStream in, String filename, Integer version) {
        StreamRDF quadStreamRDF = new StreamRDFBase() {
            @Override
            public void quad(Quad quad) {
                namedGraphs.add(quad.getGraph().getURI());
                quadValueTypes.add(new QuadValueType(
                        getTripleValueType(quad.asTriple()),
                        quad.getGraph().getURI(),
                        version - 1
                ));

                if (namedGraphs.size() == 50000) {
                    log.info("50000 named graph records found. Executing batch save named graph");
                    saveBatchVersionedNamedGraph(filename, version);
                }

                if (quadValueTypes.size() == 50000) {
                    log.info("50000 quads records found. Executing batch save quads");
                    saveBatchQuads();
                }
            }

            @Override
            public void triple(Triple triple) {
                namedGraphs.add(defaultGraphURI.getName());
                quadValueTypes.add(new QuadValueType(
                        getTripleValueType(triple),
                        defaultGraphURI.getName(),
                        version - 1
                ));

                if (namedGraphs.size() == 50000) {
                    log.info("50000 named graph records found. Executing batch save named graph");
                    saveBatchVersionedNamedGraph(filename, version);
                }

                if (quadValueTypes.size() == 50000) {
                    log.info("50000 quads records found. Executing batch save quads");
                    saveBatchQuads();
                }
            }
        };

        RDFParser.create()
                .source(in)
                .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(filename)))
                .parse(quadStreamRDF);

        saveBatchVersionedNamedGraph(filename, version);
        saveBatchQuads();

        return quadStreamRDF;
    }

    /**
     * Converts a stream into a stream of quads
     *
     * @param in The input stream of the dataset
     * @return A stream of quads
     */
    private StreamRDF getTriplesStreamRDF(InputStream in, String filename) {
        StreamRDF tripleStreamRDF = new StreamRDFBase() {
            @Override
            public void triple(Triple triple) {
                tripleValueTypes.add(getTripleValueType(triple));

                if (tripleValueTypes.size() == 50000) {
                    log.info("50000 triples records found. Executing batch save quads");
                    saveBatchTriples();
                }
            }
        };

        RDFParser.create()
                .source(in)
                .lang(RDFLanguages.nameToLang(FilenameUtils.getExtension(filename)))
                .parse(tripleStreamRDF);

        saveBatchTriples();

        return tripleStreamRDF;
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
