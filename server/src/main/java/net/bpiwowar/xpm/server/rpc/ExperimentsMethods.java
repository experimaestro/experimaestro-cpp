package net.bpiwowar.xpm.server.rpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.bpiwowar.xpm.exceptions.CloseException;
import net.bpiwowar.xpm.exceptions.XPMCommandException;
import net.bpiwowar.xpm.manager.experiments.Experiment;
import net.bpiwowar.xpm.manager.experiments.ExperimentReference;
import net.bpiwowar.xpm.manager.experiments.TaskReference;
import net.bpiwowar.xpm.scheduler.Job;
import net.bpiwowar.xpm.scheduler.Resource;
import net.bpiwowar.xpm.utils.CloseableIterable;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;

/**
 * Methods related to experiments
 */
@JsonRPCMethodsHolder("experiments")
public class ExperimentsMethods {
    /**
     * Return registered classes
     */
    @RPCMethod(help = "Cleanup old experiments")
    JsonObject cleanup(
            @RPCArgument(name = "simulate", help = "If true, don't perform the action")
            boolean simulate
    ) throws SQLException {
        // Select all resources that are part of experiments, but not the latest
        JsonObject response = new JsonObject();
        final Iterator<ExperimentReference> iterator = Experiment.experimentNames().iterator();
        while (iterator.hasNext()) {
            final ExperimentReference reference = iterator.next();
            response.addProperty(reference.identifier, Experiment.deleteOlder(simulate, reference));
        }
        return response;
    }

    @RPCMethod(help = "Find resources by experiment", name = "resources")
    public JsonObject resources(
            @RPCArgument(name = "identifier") String identifier
    ) throws SQLException {
        JsonObject response = new JsonObject();
        JsonArray resources = new JsonArray();
        response.add("resources", resources);
        final Experiment experiment = Experiment.findByIdentifier(identifier);
        response.add("experiment", toJson(experiment));
        if (experiment != null) {
            experiment.resources().forEach(
                    r -> resources.add(toJson(r))
            );
        } else {
            throw new XPMCommandException("No experiment with identifier [" + identifier + "] found");
        }
        return response;
    }

    private JsonElement toJson(Resource r) {
        JsonObject o = new JsonObject();
        o.add("id", new JsonPrimitive(r.getId()));
        o.add("locator", new JsonPrimitive(r.getLocator().toString()));
        o.add("state", new JsonPrimitive(r.getState().toString()));
        if (r instanceof Job) {
            o.add("progress", new JsonPrimitive(((Job) r).getProgress()));
        }
        return o;
    }

    @RPCMethod(name = "latest-names", help = "Get list of experiment names")
    public Stream<ExperimentReference> latestNames() {
        return Experiment.experimentNames();
    }

    @RPCMethod(help = "Get list of experiments")
    public JsonElement experiments() throws SQLException, CloseException {
        try (final CloseableIterable<Experiment> experiments = Experiment.experiments(false)) {
            JsonObject response = new JsonObject();
            final JsonArray nodes = new JsonArray();
            final JsonArray links = new JsonArray();
            response.add("nodes", nodes);
            response.add("links", links);


            for (Experiment experiment : experiments) {
                // Add the nodes
                IdentityHashMap<Object, Integer> map = new IdentityHashMap<>();
                List<TaskReference> tasks = experiment.getTasks();
                for (TaskReference taskReference : tasks) {
                    addNode(nodes, map, taskReference, taskReference.getTaskId().toString());
                    for (Resource resource : taskReference.getResources()) {
                        addNode(nodes, map, resource, resource.getIdentifier());
                    }
                }

                // Add the links
                for (TaskReference taskReference : tasks) {
                    for (TaskReference reference : taskReference.getChildren()) {
                        addLink(links, map, taskReference, reference);
                    }
                    for (Resource resource : taskReference.getResources()) {
                        addLink(links, map, taskReference, resource);
                    }
                }
                break;
            }
            return response;
        }
    }


    @RPCMethod(name = "experiment-list", help = "Get list of experiments")
    public JsonArray experimentList(
            @RPCArgument(name = "latest", help = "Only display latest experiment") boolean latest
    ) throws SQLException, CloseException {
        JsonArray experiments = new JsonArray();
        Experiment.experiments(latest).forEach(e -> {
            final JsonObject experiment = toJson(e);
            experiments.add(experiment);
        });
        return experiments;
    }

    public static JsonObject toJson(Experiment e) {
        final JsonObject experiment = new JsonObject();
        experiment.add("id", new JsonPrimitive(e.getId()));
        experiment.add("name", new JsonPrimitive(e.getName()));
        experiment.add("timestamp", new JsonPrimitive(e.getTimestamp()));
        return experiment;
    }

    private static void addLink(JsonArray links, IdentityHashMap<Object, Integer> map, Object value, Object value2) {
        final JsonObject link = new JsonObject();
        links.add(link);
        link.add("source", new JsonPrimitive(map.get(value)));
        link.add("target", new JsonPrimitive(map.get(value2)));
    }

    private static void addNode(JsonArray nodes, IdentityHashMap<Object, Integer> map, Object object, String string) {
        map.put(object, nodes.size());
        final JsonObject element = new JsonObject();
        nodes.add(element);
        element.add("name", new JsonPrimitive(string));
        element.add("group", new JsonPrimitive(1));
    }
}
