package fr.cnrs.liris.jpugetgil.sparqltosql;

import org.apache.jena.atlas.json.JsonBuilder;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.fuseki.ctl.ActionContainerItem;
import org.apache.jena.fuseki.ctl.JsonDescription;
import org.apache.jena.fuseki.server.DataAccessPoint;
import org.apache.jena.fuseki.server.ServerConst;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.ServletOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionDatasets extends ActionContainerItem {

    private static final Logger log = LoggerFactory.getLogger(ActionDatasets.class);
    private static final JsonValue EmptyObject = new JsonObject();

    public ActionDatasets() {
        super();
    }

    @Override
    public void validate(HttpAction action) {
        // No validation needed.
    }

    @Override
    protected JsonValue execGetContainer(HttpAction action) {
        log.info("{} GET datasets", action.id);
        JsonBuilder builder = new JsonBuilder();
        builder.startObject("D");
        builder.key(ServerConst.datasets);
        JsonDescription.arrayDatasets(builder, action.getDataAccessPointRegistry());
        builder.finishObject("D");
        return builder.build();
    }

    @Override
    protected JsonValue execGetItem(HttpAction action) {
        String item = getItemDatasetName(action);
        log.info("{} GET dataset {}", action.id, item);
        JsonBuilder builder = new JsonBuilder();
        DataAccessPoint dsDesc = getItemDataAccessPoint(action, item);
        if (dsDesc == null)
            ServletOps.errorNotFound("Not found: dataset " + item);
        JsonDescription.describe(builder, dsDesc);
        return builder.build();
    }


    @Override
    protected JsonValue execPostContainer(HttpAction action) {
        return EmptyObject;
    }

    @Override
    protected JsonValue execPostItem(HttpAction action) {
        return EmptyObject;
    }
}