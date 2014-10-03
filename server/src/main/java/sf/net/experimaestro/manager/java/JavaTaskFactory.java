package sf.net.experimaestro.manager.java;

import net.bpiwowar.experimaestro.tasks.JsonArgument;
import net.bpiwowar.experimaestro.tasks.TaskDescription;
import net.bpiwowar.experimaestro.tasks.ValueArgument;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.*;
import sf.net.experimaestro.utils.introspection.ClassInfo;
import sf.net.experimaestro.utils.introspection.FieldInfo;

import javax.xml.namespace.NamespaceContext;
import java.util.HashMap;
import java.util.Map;

/**
 * Task factory created from java reflection
 */
public class JavaTaskFactory extends TaskFactory {
    private final Type output;
    final JavaTasks javaTasks;
    final Connector connector;

    Map<String, Input> inputs = new HashMap<>();

    /**
     * Initialise a task
     * @param javaTasks
     * @param connector
     * @param repository The repository
     * @param aClass     The java class from which to build a task factory
     */
    public JavaTaskFactory(JavaTasks javaTasks, Connector connector, Repository repository, ClassInfo aClass, NamespaceContext namespaces) {
        super(repository);
        this.javaTasks = javaTasks;
        this.connector = connector;

        final TaskDescription description = aClass.getAnnotation(TaskDescription.class);
        if (description == null) {
            throw new XPMRuntimeException("The class %s has no TaskDescription annotation", aClass);
        }

        this.id = QName.parse(description.id(), namespaces);
        this.output = getType(namespaces, description.output());

        for (FieldInfo field : aClass.getDeclaredFields()) {
            //final Object jsonArgument = field.getAnnotation(jsonArgumentClass.class);
            final JsonArgument jsonArgument = field.getAnnotation(JsonArgument.class);
            final ValueArgument valueArgument = field.getAnnotation(ValueArgument.class);

            // TODO: add default values, etc.
            if (jsonArgument != null) {
                Input input = new JsonInput(getType(namespaces, jsonArgument.type()));
                input.setDocumentation(jsonArgument.help());
                input.setOptional(jsonArgument.required());
                inputs.put(jsonArgument.name(), input);
            } else if (valueArgument != null) {
                Input input = new JsonInput(getType(namespaces, valueArgument.type()));
                input.setDocumentation(valueArgument.help());
                input.setOptional(valueArgument.required());
                inputs.put(valueArgument.name(), input);
            }

        }
    }

    private Type getType(NamespaceContext namespaces, String output1) {
        return new Type(QName.parse(output1, namespaces));
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
        final JavaTask task = new JavaTask(this);
        task.init();
        return task;
    }
}
