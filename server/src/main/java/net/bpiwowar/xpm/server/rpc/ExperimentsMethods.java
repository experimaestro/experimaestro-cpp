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
import net.bpiwowar.xpm.scheduler.ResourceState;
import net.bpiwowar.xpm.scheduler.Scheduler;
import net.bpiwowar.xpm.utils.CloseableIterable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Methods related to experiments
 */
@JsonRPCMethodsHolder("experiments")
public class ExperimentsMethods extends BaseJsonRPCMethods {
    final static private Logger LOGGER = LogManager.getFormatterLogger();

    public ExperimentsMethods(JSONRPCRequest mos) {
        super(mos);
    }

    /**
     * Delete obsolete experiments
     */
    @RPCMethod(name = "clean-experiments", help = "Cleanup old experiments",
            returns = "A map between experiment names and deletion counts")
    static class CleanupExperiments implements JsonCallable {
        @RPCArgument(name = "simulate", required = false, help = "If true, don't perform the action")
        boolean simulate = true;

        @RPCArgument(name = "remove-resources")
        boolean removeResources;

        @Override
        public JsonObject call() throws Throwable {
            // Select all resources that are part of experiments, but not the latest
            JsonObject response = new JsonObject();
            JsonObject experiments = new JsonObject();
            response.add("experiments", experiments);
            final Iterator<ExperimentReference> iterator = Experiment.experimentNames().iterator();
            while (iterator.hasNext()) {
                final ExperimentReference reference = iterator.next();
                response.addProperty(reference.identifier, Experiment.deleteOlder(simulate, reference));
            }

            if (removeResources) {
                final ProcessObsolete cleanResources = new ProcessObsolete();
                cleanResources.command = ProcessCommand.delete;
                response.add("resources", cleanResources.call());
            }

            return response;
        }
    }

    public enum ProcessCommand {
        list, kill, delete
    }

    @RPCMethod(name = "process-obsolete", help = "Removes resources not belonging to any experiment")
    static class ProcessObsolete implements JsonCallable {
        @RPCArgument(name = "command", required = false, help = "Command to apply")
        ProcessCommand command = ProcessCommand.list;


        @Override
        public JsonArray call() throws Throwable {
            JsonArray array = new JsonArray();

            final Set<Long> set = Experiment.listObsoleteResources();

            for (Long rid : set) {
                final Resource resource = Resource.getById(rid);
                array.add(new JsonPrimitive(resource.getIdentifier()));
                switch (command) {
                    case kill:
                        if (resource instanceof Job) {
                            ((Job) resource).stop();
                        }
                        break;

                    case delete:
                        if (resource instanceof Job && ((Job) resource).checkProcess()) {
                            ((Job) resource).stop();
                        }
                        resource.delete(false);
                        break;
                }
            }

            return array;
        }
    }

    @RPCMethod(name = "task-references")
    static class ListTaskReferences implements JsonCallable {
        @RPCArgument
        String identifier;

        @RPCArgument
        long timestamp;

        @Override
        public JsonObject call() throws Throwable {
            return null;
        }
    }


    @RPCMethod(help = "Find resources by experiment", name = "resources")
    static class FindResources implements JsonCallable {
        @RPCArgument(name = "identifier")
        String identifier;

        @RPCArgument(name = "timestamp", required = false)
        Long timestamp;

        @Override
        public Object call() throws Throwable {
            JsonObject response = new JsonObject();

            final Experiment experiment = timestamp != null && timestamp > 0 ?
                    Experiment.find(identifier, timestamp) : Experiment.findByIdentifier(identifier);
            response.add("experiment", ExperimentsMethods.toJson(experiment));

            JsonObject tasks = new JsonObject();
            response.add("tasks", tasks);

            JsonArray resources = new JsonArray();
            response.add("resources", resources);
            if (experiment != null) {
                experiment.resources().forEach(rt -> {
                    final String taskIdString = String.valueOf(rt.taskId);
                    if (!tasks.has(taskIdString)) {
                        tasks.addProperty(taskIdString, rt.taskIdentifier);
                    }
                    final JsonObject json = toJson(rt.resource);
                    json.addProperty("taskid", rt.taskId);
                    resources.add(json);
                });

            } else {
                throw new XPMCommandException("No experiment with identifier [" + identifier + "] found");
            }
            return response;
        }

        static private JsonObject toJson(Resource r) {
            JsonObject o = new JsonObject();
            o.add("id", new JsonPrimitive(r.getId()));
            o.add("locator", new JsonPrimitive(r.getLocator().toString()));
            o.add("state", new JsonPrimitive(r.getState().toString()));
            if (r instanceof Job) {
                o.add("progress", new JsonPrimitive(((Job) r).getProgress()));
            }
            return o;
        }

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
                    addNode(nodes, map, taskReference, taskReference.getTaskId());
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

    @RPCMethod(name = "delete", help = "Delete an experiment")
    public static int delete(@RPCArgument(name = "identifier") String identifier) throws SQLException {
        int count = 0;
        for (Experiment experiment : Experiment.findAllByIdentifier(identifier)) {
            count += experiment.delete();
        }
        return count;
    }

    @RPCMethod(name = "kill", help = "Kill all jobs from an experiment")
    public class Kill implements JsonCallable {
        @RPCArgument
        String identifier;

        @RPCArgument
        EnumSet<ResourceState> states = ResourceState.RUNNABLE_STATES;

        @Override
        public Object call() throws Throwable {
            int n = 0;
            for (Resource resource : Experiment.resourcesByIdentifier(identifier, states)) {
                try {
                    if (resource instanceof Job) {
                        if (((Job) resource).stop()) {
                            n++;
                        }
                    }
                } catch (Throwable throwable) {
                    LOGGER.error("Error while killing job [%s]", resource);
                }
            }
            return n;
        }
    }


    @RPCMethod(name = "invalidate", help = "Restart all jobs from an experiment")
    public class Invalidate implements JsonCallable {
        @RPCArgument(name = "experimentId")
        String experimentId;

        @RPCArgument
        EnumSet<ResourceState> states = ResourceState.RUNNABLE_STATES;

        @RPCArgument(name = "recursive", required = false, help = "Whether we should invalidate dependent results when the job was done")
        boolean recursive = true;

        @RPCArgument(name = "restart", help = "Whether we should restart the job (false = error)")
        boolean restart = true;

        @Override
        public Object call() throws Throwable {
            int nbUpdated = 0;

            for (Resource resource : Experiment.resourcesByIdentifier(experimentId, states)) {

                try {
                    LOGGER.info("Invalidating resource %s", resource);

                    final ResourceState rsrcState = resource.getState();

                    // Stop running jobs
                    if (rsrcState == ResourceState.RUNNING) {
                        ((Job) resource).stop();
                    }

                    // The job is active, so we have nothing to do
                    if (rsrcState.isActive() && rsrcState != ResourceState.RUNNING) {
                        // Just notify in case
                        Scheduler.notifyRunners();
                        continue;
                    }

                    if (restart || !resource.getState().isFinished()) {
                        ((Job) resource).invalidate(restart);
                        nbUpdated++;

                        // If the job was done, we need to invalidate the dependences
                        if (recursive && rsrcState == ResourceState.DONE) {
                            nbUpdated += JsonRPCMethods.invalidate(resource, restart);
                        }
                    }
                } catch (Throwable t) {
                    LOGGER.error("Could not invalidate job %s: %s", resource, t);
                }
            }

            return nbUpdated;
        }

    }
}
