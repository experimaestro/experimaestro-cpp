package net.bpiwowar.xpm.manager.tasks;

import net.bpiwowar.xpm.connectors.NetworkShare;
import net.bpiwowar.xpm.exceptions.XPMRuntimeException;
import net.bpiwowar.xpm.manager.Constants;
import net.bpiwowar.xpm.manager.Input;
import net.bpiwowar.xpm.manager.JsonInput;
import net.bpiwowar.xpm.manager.Repository;
import net.bpiwowar.xpm.manager.Task;
import net.bpiwowar.xpm.manager.TaskFactory;
import net.bpiwowar.xpm.manager.Type;
import net.bpiwowar.xpm.manager.ValueType;
import net.bpiwowar.xpm.manager.json.Json;
import net.bpiwowar.xpm.manager.json.JsonObject;
import net.bpiwowar.xpm.manager.scripting.Exposed;
import net.bpiwowar.xpm.scheduler.Commands;
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
        this.id = information.id;

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
    }

    static private Type getType(InputInformation field) {
        return new ValueType(field.getValueType());
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

    public Commands commands(JsonObject json, boolean simulate) {
        Commands commands = build(json);


        // Check dependencies
        if (!simulate) {
            for (Json element : json.values()) {
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
                    final Dependency lock = resource.createDependency((DependencyParameters)null);
                    commands.addDependency(lock);
                }
            }
        }

        return commands;
    }

    protected abstract Commands build(JsonObject json);

    public abstract void setEnvironment(JsonObject json, Map<String, String> environment);
}
