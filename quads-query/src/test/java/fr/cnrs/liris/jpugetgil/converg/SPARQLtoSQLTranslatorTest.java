package fr.cnrs.liris.jpugetgil.converg;

import fr.cnrs.liris.jpugetgil.converg.sql.SQLQuery;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SPARQLtoSQLTranslatorTest {
    SPARQLtoSQLTranslator condensedSPARQLtoSQLTranslator = new SPARQLtoSQLTranslator(true);
    SPARQLtoSQLTranslator flatSPARQLtoSQLTranslator = new SPARQLtoSQLTranslator(false);

    @Test
    void testBuildSPARQLContextCondensed() {
        Query query = QueryFactory.create("SELECT ?g ?s ?p ?o WHERE { GRAPH ?g { ?s ?p ?o } }");
        SQLQuery sqlQuery = getSqlQuery(query, condensedSPARQLtoSQLTranslator);
        assertNotNull(sqlQuery.getSql());
    }

    @Test
    void testBuildSPARQLContextFlat() {
        Query query = QueryFactory.create("SELECT ?g ?s ?p ?o WHERE { GRAPH ?g { ?s ?p ?o } }");
        SQLQuery sqlQuery = getSqlQuery(query, flatSPARQLtoSQLTranslator);
        assertNotNull(sqlQuery.getSql());
    }

    private static SQLQuery getSqlQuery(Query query, SPARQLtoSQLTranslator condensedSPARQLtoSQLTranslator) {
        Op op = Algebra.compile(query);

        // Transform the op to a quad form
        Op quadOp = Algebra.toQuadForm(op);

        SQLQuery sqlQuery = condensedSPARQLtoSQLTranslator.buildSPARQLContext(quadOp);
        return sqlQuery;
    }
}