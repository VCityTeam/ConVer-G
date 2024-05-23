package fr.cnrs.liris.jpugetgil.sparqltosql;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.sys.FusekiModule;
import org.apache.jena.fuseki.validation.DataValidator;
import org.apache.jena.fuseki.validation.IRIValidator;
import org.apache.jena.fuseki.validation.QueryValidator;
import org.apache.jena.fuseki.validation.UpdateValidator;
import org.apache.jena.rdf.model.Model;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.URLResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class FusekiUI implements FusekiModule {

    private static final Logger log = LoggerFactory.getLogger(FusekiUI.class);

    @Override
    public String name() {
        return "Fuseki-UI";
    }

    @Override
    public void prepare(FusekiServer.Builder builder, Set<String> datasetNames, Model configModel) {
        Resource uiApp = new URLResourceFactory().newClassLoaderResource("webapp");
        log.info("Fuseki UI loaded from: {}", uiApp);
        builder
                .staticFileBase(uiApp.getURI().toString())
                .addServlet("/$/datasets", new ActionDatasets())
                .addServlet("/$/server", new ActionServerStatus())
                .addServlet("/$/validate/query", new QueryValidator())
                .addServlet("/$/validate/update", new UpdateValidator())
                .addServlet("/$/validate/iri", new IRIValidator())
                .addServlet("/$/validate/data", new DataValidator())
                .enablePing(true)
                .enableStats(true)
                .enableCompact(true)
                .enableMetrics(true)
                .enableTasks(true);

        log.info("Fuseki UI loaded");
    }
}