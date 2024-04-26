package fr.cnrs.liris.jpugetgil.sparqltosql;

import static org.apache.jena.riot.WebContent.charsetUTF8;
import static org.apache.jena.riot.WebContent.contentTypeJSON;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonBuilder;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.ctl.ActionCtl;
import org.apache.jena.fuseki.ctl.JsonDescription;
import org.apache.jena.fuseki.server.DataAccessPointRegistry;
import org.apache.jena.fuseki.server.ServerConst;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.ServletOps;

public class ActionServerStatus extends ActionCtl
{
    public ActionServerStatus() { super(); }

    @Override
    public void validate(HttpAction action) {
        // No validation needed.
    }

    @Override
    public void execGet(HttpAction action) {
        executeLifecycle(action);
    }

    @Override
    public void execPost(HttpAction action) {
        executeLifecycle(action);
    }

    @Override
    public  void execute(HttpAction action) {
        try {
            description(action);
            ServletOps.success(action);
        } catch (IOException e) {
            ServletOps.errorOccurred(e);
        }
    }

    private void description(HttpAction action) throws IOException {
        OutputStream out = action.getResponseOutputStream();
        action.setResponseContentType(contentTypeJSON);
        action.setResponseCharacterEncoding(charsetUTF8);

        JsonBuilder builder = new JsonBuilder();
        builder.startObject();
        describeServer(builder);
        describeDatasets(builder, action.getDataAccessPointRegistry());
        builder.finishObject();

        JsonValue v = builder.build();
        JSON.write(out, v);
        out.write('\n');
        out.flush();
    }

    private void describeServer(JsonBuilder builder) {
        String versionStr = Fuseki.VERSION + "-edited";
        builder
                .pair("version",   versionStr)
                .pair("uptime",    Fuseki.serverUptimeSeconds());
    }

    private void describeDatasets(JsonBuilder builder, DataAccessPointRegistry registry) {
        builder.key(ServerConst.datasets);
        JsonDescription.arrayDatasets(builder, registry);
    }
}
