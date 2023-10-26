package fr.vcity.sparqltosql.utils;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.sparql.syntax.*;

@Getter
@Slf4j
public class SPARQLtoSQLVisitor2 implements ElementVisitor {

    private static final String GRAPH_NAME_PATTERN = "/GraphName#";
    private static final String VERSION_PATTERN = "/Version#";

    private String generatedSQL = "";

    /**
     * @param el
     */
    @Override
    public void visit(ElementTriplesBlock el) {
        log.info("ElementTriplesBlock {}", el.toString());
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementPathBlock el) {
        log.info("ElementPathBlock {}", el.toString());
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementFilter el) {
        log.info("ElementFilter {}", el.toString());
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementAssign el) {
        log.info("ElementAssign {}", el.toString());
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementBind el) {
        log.info("ElementBind {}", el.toString());
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementData el) {
        log.info("ElementData {}", el.toString());
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementUnion el) {
        log.info("ElementUnion {}", el.toString());
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementOptional el) {
        log.info("ElementOptional {}", el.toString());
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementLateral el) {
        log.info("ElementLateral {}", el.toString());
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementGroup el) {
        String nodeString = el.toString();

        if (nodeString.contains(VERSION_PATTERN)) {
            log.debug("ElementNamedGraph V: {}", getAnchorValueFromURI(nodeString));
        } else if (nodeString.contains(GRAPH_NAME_PATTERN)) {
            log.debug("ElementNamedGraph NG: {}", getAnchorValueFromURI(nodeString));
        }
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementDataset el) {
        log.info("ElementDataset {}", el.toString());
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementNamedGraph el) {
        log.info("ElementNamedGraph {}", el.toString());
        log.info("Element {}", el.getElement().toString());
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementExists el) {
        log.info("ElementExists {}", el.toString());
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementNotExists el) {
        log.info("ElementNotExists {}", el.toString());
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementMinus el) {
        log.info("ElementMinus {}", el.toString());
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementService el) {
        log.info("ElementService {}", el.toString());
    }

    /**
     * @param el
     */
    @Override
    public void visit(ElementSubQuery el) {
        log.info("ElementSubQuery {}", el.toString());
    }

    private static String getAnchorValueFromURI(String uri) {
        return uri.substring(uri.indexOf('#') + 1);
    }
}
