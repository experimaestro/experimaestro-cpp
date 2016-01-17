package net.bpiwowar.xpm.manager.tasks;

import com.google.gson.JsonElement;
import net.bpiwowar.xpm.commands.CommandOutput;
import net.bpiwowar.xpm.connectors.NetworkShare;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.*;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.commands.Commands;
import net.bpiwowar.xpm.scheduler.Dependency;
import net.bpiwowar.xpm.scheduler.DependencyParameters;
import net.bpiwowar.xpm.scheduler.Resource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Exposed
public abstract class ExternalTaskFactory extends TaskFactory {
    protected final Map<String, Input> inputs = new HashMap<>();
    Map<String, JsonElement> constants;
    protected Type output;
    ArrayList<PathArgument> pathArguments;
    Map<String, String> prefixes;

    protected ExternalTaskFactory() {
    }

    public ExternalTaskFactory(Repository repository, TaskInformation information) {
        super(repository);
        this.output = new ValueType(information.output);
        this.pathArguments = information.pathArguments;
        this.prefixes = information.prefixes;
        this.constants = information.constants;
        this.id = information.id;
        if (information.id == null)
            throw new AssertionError("Task ID is not defined");

        // Add inputs
        for (Map.Entry<String, InputInformation> entry : information.inputs.entrySet()) {
            String name = entry.getKey();
            final InputInformation field = entry.getValue();

            Input input = new JsonInput(getType(field));
            input.setDocumentation(field.help);
            input.setOptional(!field.required);
            input.setCopyTo(field.copyTo);
            inputs.put(name, input);
        }

        // Add paths
        for (PathArgument pathArgument : information.pathArguments) {
            Input input = new JsonInput(Type.XP_PATH);
            input.setOptional(true); // We are lying -- we will set it!
            inputs.put(pathArgument.jsonName, input);
        }

    }

    static private Type getType(InputInformation field) {
        final TaskInputType type = field.getType();
        if (type == null) {
            return new ValueType(Constants.XP_ANY);
        } else if (type instanceof TaskInputType.Json) {
            return new ValueType(((TaskInputType.Json) type).value);
        } else if (type instanceof TaskInputType.InputStream) {
            return new InputStreamType(((TaskInputType.InputStream) type).inputstream);
        }

        throw new AssertionError("Unhandled type: " + type);
    }

    @Override
    public Map<String, Input> getInputs() {
        return inputs;
    }

    @Override
    public Type getOutput() {
        return output;
    }

    @Override
    public Task create() {
        final ExternalTask task = new ExternalTask(this);
        task.init();
        return task;
    }

    public Commands commands(HashMap<Object, CommandOutput> streams, Task task, boolean simulate) {
        // Add our command
        Commands commands = build(task);

        // Check dependencies
        if (!simulate) {
            for (Value value : task.getValues().values()) {
                Json element = value.get();
                if (element instanceof JsonObject) {
                    JsonObject object = (JsonObject) element;
                    final Json r = object.get(Constants.XP_RESOURCE.toString());
                    if (r == null) continue;

                    final Object o = r.get();
                    Resource resource;
                    if (o instanceof Resource) {
                        resource = (Resource) o;
                    } else {
                        try {
                            resource = Resource.getByLocator(NetworkShare.uriToPath(o.toString()));
                        } catch (SQLException e) {
                            throw new XPMRuntimeException(e, "Error while searching the resource %s the task %s depends upon",
                                    o.toString(), getId());
                        }
                        if (resource == null) {
                            throw new XPMRuntimeException("Cannot find the resource %s the task %s depends upon",
                                    o.toString(), getId());
                        }
                    }
                    final Dependency lock = resource.createDependency((DependencyParameters) null);
                    commands.addDependency(lock);
                }
            }
        }

        return commands;
    }

    protected abstract Commands build(Task task);

    public abstract void setEnvironment(JsonObject json, Map<String, String> environment);
}
