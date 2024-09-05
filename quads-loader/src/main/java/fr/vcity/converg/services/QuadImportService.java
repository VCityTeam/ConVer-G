package fr.vcity.converg.services;

import fr.vcity.converg.dao.ResourceOrLiteral;
import fr.vcity.converg.dao.Version;
import fr.vcity.converg.repository.*;
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
import java.util.*;

@Service
@Slf4j
public class QuadImportService implements IQuadImportService {

    public record Node(String value, String type) {
    }

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
     * @param defaultModel The default graph
     */
    private void importDefaultModel(Model defaultModel) {
        Set<RDFNode> nodeSet = new HashSet<>();
        List<TripleValueType> tripleValueTypes = new ArrayList<>();

        for (StmtIterator stmtIterator = defaultModel.listStatements(); stmtIterator.hasNext(); ) {
            tripleValueTypes.add(getTripleValueType(stmtIterator, nodeSet));
        }

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
     * @param dataset The dataset
     * @param version The version
     */
    private void extractAndInsertQuads(Dataset dataset, Version version) {
        Set<RDFNode> nodeSet = new HashSet<>();
        List<QuadValueType> quadValueTypes = new ArrayList<>();

        for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
            Resource namedModel = i.next();
            Model model = dataset.getNamedModel(namedModel);
            log.info("Name Graph : {}", namedModel.getURI());

            for (StmtIterator stmtIterator = model.listStatements(); stmtIterator.hasNext(); ) {
                QuadValueType quadValueType = new QuadValueType(getTripleValueType(stmtIterator, nodeSet), namedModel.getURI(), version.getIndexVersion() - 1);
                quadValueTypes.add(quadValueType);
            }
        }

        if (!dataset.getDefaultModel().listStatements().toList().isEmpty()) {
            for (StmtIterator stmtIterator = dataset.getDefaultModel().listStatements(); stmtIterator.hasNext(); ) {
                QuadValueType quadValueType = new QuadValueType(getTripleValueType(stmtIterator, nodeSet), defaultGraphURI.getName(), version.getIndexVersion() - 1);
                quadValueTypes.add(quadValueType);
            }
        }

        if (!quadValueTypes.isEmpty()) {
            versionedQuadComponent.saveQuads(quadValueTypes);
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
        List<String> namedGraphs = new ArrayList<>();
        for (Iterator<Resource> i = dataset.listModelNames(); i.hasNext(); ) {
            Resource namedModel = i.next();
            namedGraphs.add(namedModel.getURI());
        }

        if (!dataset.getDefaultModel().listStatements().toList().isEmpty()) {
            namedGraphs.add(defaultGraphURI.getName());
        }

        if (!namedGraphs.isEmpty()) {
            versionedNamedGraphComponent.saveVersionedNamedGraph(namedGraphs, file.getOriginalFilename(), version.getIndexVersion());
        }
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
