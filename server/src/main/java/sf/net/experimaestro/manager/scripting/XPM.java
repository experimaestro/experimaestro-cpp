package sf.net.experimaestro.manager.scripting;

/*
 * This file is part of experimaestro.
 * Copyright (c) 2014 B. Piwowarski <benjamin@bpiwowar.net>
 *
 * experimaestro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * experimaestro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with experimaestro.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.log4j.Level;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import sf.net.experimaestro.connectors.*;
import sf.net.experimaestro.exceptions.ExperimaestroCannotOverwrite;
import sf.net.experimaestro.exceptions.ValueMismatchException;
import sf.net.experimaestro.exceptions.XPMRhinoException;
import sf.net.experimaestro.exceptions.XPMRhinoIllegalArgumentException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.AlternativeType;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.Module;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.manager.Task;
import sf.net.experimaestro.manager.TaskFactory;
import sf.net.experimaestro.manager.experiments.TaskReference;
import sf.net.experimaestro.manager.js.JavaScriptContext;
import sf.net.experimaestro.manager.js.JavaScriptTaskFactory;
import sf.net.experimaestro.manager.json.Json;
import sf.net.experimaestro.manager.json.JsonObject;
import sf.net.experimaestro.manager.json.JsonResource;
import sf.net.experimaestro.scheduler.*;
import sf.net.experimaestro.server.TasksServlet;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.Output;
import sf.net.experimaestro.utils.io.LoggerPrintWriter;
import sf.net.experimaestro.utils.log.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static sf.net.experimaestro.utils.JSUtils.unwrap;

/**
 * XPM Object in scripting languages
 */
@Exposed
public class XPM {
    public static final String COMMAND_LINE_JOB_HELP = "Schedule a command line job.<br>The options are <dl>" +
            "<dt>launcher</dt><dd></dd>" +
            "<dt>stdin</dt><dd></dd>" +
            "<dt>stdout</dt><dd></dd>" +
            "<dt>lock</dt><dd>An array of couples (resource, lock type). The lock depends on the resource" +
            "at hand, but are generally READ, WRITE, EXCLUSIVE.</dd>" +
            "";

//    static public void log(Level level, Context cx, Scriptable thisObj, Object[] args, Function funObj) {
//        if (args.length < 1)
//            throw new XPMRuntimeException("There should be at least one argument for log()");
//
//        String format = Context.toString(args[0]);
//        Object[] objects = new Object[args.length - 1];
//        for (int i = 1; i < args.length; i++)
//            objects[i - 1] = unwrap(args[i]);
//
//        ((XPM) thisObj).xpm.log(format, objects);
//    }

    final static private Logger LOGGER = Logger.getLogger();

    static HashSet<String> COMMAND_LINE_OPTIONS = new HashSet<>(
            ImmutableSet.of("stdin", "stdout", "lock", "connector", "launcher"));

    @Expose()
    @Help("Retrieve (or creates) a token resource with a given xpath")
    static public TokenResource token_resource(
            @Argument(name = "path", help = "The path of the resource") String path
    ) throws ExperimaestroCannotOverwrite, SQLException, URISyntaxException {
        final Resource resource = Resource.getByLocator(NetworkShare.uriToPath(path));
        final TokenResource tokenResource;
        if (resource == null) {
            tokenResource = new TokenResource(NetworkShare.uriToPath(path), 0);
            tokenResource.save();
        } else {
            if (!(resource instanceof TokenResource))
                throw new AssertionError(String.format("Resource %s exists and is not a token", path));
            tokenResource = (TokenResource) resource;
        }

        return tokenResource;
    }

    protected static ScriptContext context() {

        return ScriptContext.get();
    }

    public static java.nio.file.Path getPath(Connector connector, Object stdout) throws IOException {

        if (stdout instanceof String || stdout instanceof ConsString)
            return connector.getMainConnector().resolveFile(stdout.toString());

        if (stdout instanceof ScriptingPath)
            return connector.getMainConnector().resolveFile(stdout.toString());

        if (stdout instanceof java.nio.file.Path)
            return (java.nio.file.Path) stdout;

        throw new XPMRuntimeException("Unsupported stdout type [%s]", stdout.getClass());
    }

    public static Object get(Scriptable scope, final String name) {

        Object object = scope.get(name, scope);
        if (object != null && object == Undefined.instance)
            object = null;
        else if (object instanceof Wrapper)
            object = ((Wrapper) object).unwrap();
        return object;
    }

    static String toString(Object object) {

        if (object instanceof NativeJavaObject)
            return ((NativeJavaObject) object).unwrap().toString();
        return object.toString();
    }


    @Expose("set_property")
    public void setProperty(String name, Object object) {
        context().setProperty(name, object);
    }

    @Expose("set_default_lock")
    @Help("Adds a new resource to lock for all jobs to be started")
    public void setDefaultLock(
            @Argument(name = "resource", help = "The resource to be locked")
            Resource resource,
            @Argument(name = "parameters", help = "The parameters to be given at lock time")
            Object parameters) {

        context().addDefaultLock(resource, parameters);
    }

    @Expose("logger")
    public ScriptingLogger getLogger(String name) {

        return new ScriptingLogger(name);
    }

    @Expose("log_level")
    @Help(value = "Sets the logger debug level")
    public void setLogLevel(
            @Argument(name = "name") String name,
            @Argument(name = "level") String level
    ) {

        context().getLogger(name).setLevel(Level.toLevel(level));
    }

    @Expose("get_script_path")
    public String getScriptPath() {

        return context().getCurrentScriptPath().toString();
    }

    @Expose("get_script_file")
    public ScriptingPath getScriptFile() throws FileSystemException {

        return new ScriptingPath(context().getCurrentScriptPath());
    }

    /**
     * Add a module
     */
    @Expose("add_module")
    public Module addModule(QName qname) {
        final ScriptContext scriptContext = context();
        Module module = new Module(qname);
        LOGGER.debug("Adding module [%s]", module.getId());
        scriptContext.getRepository().addModule(module);
        return module;
    }

    @Expose(value = "add_module", context = true)
    public Module addModule(LanguageContext cx, Map description) {
        final ScriptContext scriptContext = context();
        Module module = new Module(cx.qname(description.get("id")));
        module.setName(description.get("name").toString());
//        module.setDocumentation(description.get("description").toString());

        // Set the parent
        final Object parentString = description.get("parent");
        if (parentString != null) {
            QName parent = cx.qname(parentString);
            final Module parentModule = scriptContext.getRepository().getModules().get(parent);
            if (parentModule != null) {
                module.setParent(parentModule);
            }
        }

        // Add the module
        scriptContext.getRepository().addModule(module);
        return module;
    }

    /**
     * Add an experimentId
     *
     * @param object
     * @return
     */
    @Expose(value = "add_task_factory", context = true)
    public JavaScriptTaskFactory add_task_factory(JavaScriptContext jcx, @NoJavaization NativeObject object) throws ValueMismatchException {
        ScriptContext scx = context();

        final QName id = jcx.qname(JSUtils.get(jcx.scope(), "id", object));
        final JavaScriptTaskFactory factory = new JavaScriptTaskFactory(id, jcx.scope(), object, scx.getRepository());
        scx.getRepository().addFactory(factory);
        return factory;
    }

    @Expose("get_task")
    public Task getTask(QName name) {
        return ScriptContext.get().getTask(name);
    }

    @Expose("get_task")
    public Task getTask(
            String namespaceURI,
            String localName) {
        return ScriptContext.get().getTask(new QName(namespaceURI, localName));
    }

    @Expose("file")
    @Help(value = "Returns a file relative to the current connector")
    public Path file(@Argument(name = "filepath") String filepath) throws FileSystemException {
        return context().getCurrentScriptPath().resolve(filepath);
    }


    @Expose(value = "command_line_job", optional = 1)
    @Help(value = COMMAND_LINE_JOB_HELP)
    public Resource commandlineJob(@Argument(name = "jobId") Object path,
                                   @Argument(type = "Array", name = "command") List<?> jsargs,
                                   @Argument(type = "Map", name = "options") Map<String, Object> options) throws Exception {

        Commands commands = new Commands(Command.getCommand(jsargs));
        return commandlineJob(path, commands, options);
    }


    @Expose(value = "command_line_job", optional = 1)
    @Help(value = COMMAND_LINE_JOB_HELP)
    public Resource commandlineJob(@Argument(name = "jobId") Object path,
                                   @Argument(type = "Array", name = "command") AbstractCommand command,
                                   @Argument(type = "Map", name = "options") Map<String, Object> options) throws Exception {

        Commands commands = new Commands(command);
        return commandlineJob(path, commands, options);
    }


    @Expose(value = "command_line_job", optional = 1)
    @Help(value = COMMAND_LINE_JOB_HELP)
    public Resource commandlineJob(
            @Argument(name = "json") JsonObject json,
            @Argument(name = "jobId") Object jobId,
            @Argument(name = "commands") Object commands,
            @Argument(type = "Map", name = "options") Map<String, Object> jsOptions) throws Exception {

        Commands _commands;
        if (commands instanceof Commands) {
            _commands = (Commands) commands;
        } else if (commands instanceof AbstractCommand) {
            _commands = new Commands((AbstractCommand) commands);
        } else if (commands instanceof List) {
            _commands = new Commands(Command.getCommand((List) commands));
        } else {
            throw new XPMRhinoIllegalArgumentException("2nd argument of command_line_job must be a command");
        }

        Resource resource = commandlineJob(jobId, _commands, jsOptions);

        // Update the json
        json.put(Manager.XP_RESOURCE.toString(), new JsonResource(resource));
        return resource;
    }


    /**
     * Declare an alternative
     *
     * @param qname A qualified name
     */
    @Expose("declare_alternative")
    @Help(value = "Declare a qualified name as an alternative input")
    public void declareAlternative(Object qname) {

        AlternativeType type = new AlternativeType((QName) qname);
        getRepository().addType(type);
    }


    @Expose("publish")
    @Help("Publish the repository on the web server")
    public void publish() throws InterruptedException {

        final ScriptContext scriptContext = context();
        TasksServlet.updateRepository(context().getCurrentScriptPath().toString(), scriptContext.getRepository());
    }


    @Expose
    @Help("Set the simulate flag: When true, the jobs are not submitted but just output")
    public boolean simulate(boolean simulate) {
        final boolean old = RunningContext.get().simulate();
        RunningContext.get().simulate(simulate);
        return old;
    }

    @Expose
    public boolean simulate() {
        return RunningContext.get().simulate();
    }

    @Expose(context = true)
    public String evaluate(LanguageContext lc, List<Object> command) throws Exception {
        return evaluate(lc, command, ImmutableMap.of());
    }

    /**
     * Simple evaluation of shell commands (does not createSSHAgentIdentityRepository a job)
     *
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @Expose(context = true)
    public String evaluate(LanguageContext lc, List<Object> jsargs, Map options) throws Exception {
        ScriptContext sc = context();
        Command command = Command.getCommand(jsargs);

        // Get the launcher
        final Launcher launcher;
        if (options.containsKey("launcher")) {
            launcher = (Launcher) options.get("launcher");
        } else {
            launcher = sc.getDefaultLauncher();
        }

        // Run the process and captures the output

        AbstractProcessBuilder builder = launcher.processBuilder();

        SingleHostConnector commandConnector = launcher.getMainConnector();
        try (CommandContext commandEnv = new CommandContext.Temporary(launcher)) {
            // Transform the list
            builder.command(Lists.newArrayList(Iterables.transform(command.list(), argument -> {
                try {
                    return argument.toString(commandEnv);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })));

            if (options.containsKey("stdout")) {
                java.nio.file.Path stdout = getPath(commandConnector, unwrap(options.get("stdout")));
                builder.redirectOutput(AbstractCommandBuilder.Redirect.to(stdout));
            } else {
                builder.redirectOutput(AbstractCommandBuilder.Redirect.PIPE);
            }

            return builder.execute(sc.getMainLogger());
        }
    }


    @Expose(value = "command_line_job", optional = 1)
    @Help(value = COMMAND_LINE_JOB_HELP)
    public Resource commandlineJob(@Argument(name = "jobId") Object path,
                                   Commands commands,
                                   @Argument(type = "Map", name = "options") Map<String, Object> options) throws Exception {
        final ScriptContext scriptContext = context();
        final Logger rootLogger = scriptContext.getLogger("xpm");


        CommandLineTask job = null;
        // --- XPMProcess arguments: convert the javascript array into a Java array
        // of String
        LOGGER.debug("Adding command line job");

        // --- Create the task


        final Connector connector = (Connector) options.get("connector");

        // Resolve the path for the given connector
        if (!(path instanceof java.nio.file.Path)) {
            path = NetworkShare.uriToPath(path.toString());
        }

        job = new CommandLineTask((java.nio.file.Path) path);

        job.setCommands(commands);
        RunningContext rc = RunningContext.get();
        if (rc.getSubmittedJobs().containsKey(path)) {
            rootLogger.info("Not submitting %s [duplicate]", path);
            if (simulate()) {
                return job;
            }

            return Resource.getByLocator(connector.resolve((java.nio.file.Path) path));
        }


        // --- Environment
        ArrayList<Dependency> dependencies = new ArrayList<>();

        // --- Set defaults
        scriptContext.prepare(job);

        // --- Options
        if (options != null) {

            final ArrayList unmatched = new ArrayList(Sets.difference(options.keySet(), COMMAND_LINE_OPTIONS));
            if (!unmatched.isEmpty()) {
                throw new IllegalArgumentException(format("Some options are not allowed: %s",
                        Output.toString(", ", unmatched)));
            }


            // --- XPMProcess launcher
            if (options.containsKey("launcher")) {
                final Object launcher = options.get("launcher");
                if (launcher != null)
                    job.setLauncher((Launcher) launcher);

            }

            // --- Redirect standard output
            if (options.containsKey("stdin")) {
                final Object stdin = options.get("stdin");
                if (stdin instanceof String) {
                    job.setInput((String) stdin);
                } else if (stdin instanceof java.nio.file.Path) {
                    job.setInput((java.nio.file.Path) stdin);
                } else
                    throw new XPMRuntimeException("Unsupported stdin type [%s]", stdin.getClass());
            }

            // --- Redirect standard output
            if (options.containsKey("stdout")) {
                java.nio.file.Path fileObject = XPM.getPath(connector, options.get("stdout"));
                job.setOutput(fileObject);
            }

            // --- Redirect standard error
            if (options.containsKey("stderr")) {
                java.nio.file.Path fileObject = XPM.getPath(connector, options.get("stderr"));
                job.setError(fileObject);
            }


            // --- Resources to lock
            if (options.containsKey("lock")) {
                List locks = (List) options.get("lock");
                for (int i = locks.size(); --i >= 0; ) {
                    Object lock_i = locks.get(i);
                    Dependency dependency = null;

                    if (lock_i instanceof Dependency) {
                        dependency = (Dependency) lock_i;
                    } else if (lock_i instanceof List) {
                        List array = (List) lock_i;
                        if (array.size() != 2) {
                            throw new XPMRhinoException(new IllegalArgumentException("Wrong number of arguments for lock"));
                        }

                        final Object depObject = array.get(0);
                        Resource resource = null;
                        if (depObject instanceof Resource) {
                            resource = (Resource) depObject;
                        } else {
                            final String rsrcPath = Context.toString(depObject);
                            resource = Resource.getByLocator(rsrcPath);
                            if (resource == null)
                                if (simulate()) {
                                    if (!rc.getSubmittedJobs().containsKey(rsrcPath))
                                        LOGGER.error("The dependency [%s] cannot be found", rsrcPath);
                                } else {
                                    throw new XPMRuntimeException("Resource [%s] was not found", rsrcPath);
                                }
                        }

                        final Object lockType = array.get(1);
                        LOGGER.debug("Adding dependency on [%s] of type [%s]", resource, lockType);

                        if (!simulate()) {
                            dependency = resource.createDependency(lockType);
                        }
                    } else {
                        throw new XPMRuntimeException("Element %d for option 'lock' is not a dependency but %s",
                                i, lock_i.getClass());
                    }

                    if (!simulate()) {
                        dependencies.add(dependency);
                    }
                }

            }


        }


        job.setState(ResourceState.WAITING);
        if (simulate()) {
            PrintWriter pw = new LoggerPrintWriter(rootLogger, Level.INFO);
            pw.format("[SIMULATE] Starting job: %s%n", job.toString());
            pw.format("Command: %s%n", job.getCommands().toString());
            pw.format("Locator: %s", path.toString());
            pw.flush();
        } else {

            // Add dependencies
            dependencies.forEach(job::addDependency);

            // Register within an experimentId
            if (scriptContext.getExperiment() != null) {
                TaskReference reference = scriptContext.getTaskReference();
                reference.add(job);
            }

            final Resource old = Resource.getByLocator(job.getLocator());

            job.updateStatus();

            // Replace old if necessary
            if (old != null) {
                if (!old.canBeReplaced()) {
                    rootLogger.log(old.getState() == ResourceState.DONE ? Level.DEBUG : Level.INFO,
                            "Cannot overwrite task %s [%d]", old.getLocator(), old.getId());
                    return old;
                } else {
                    rootLogger.info("Replacing resource %s [%d]", old.getLocator(), old.getId());
                    old.replaceBy(job);
                }
            } else {
                // Store in scheduler
                job.save();
            }

        }

        rc.getSubmittedJobs().put(job.getLocator().toString(), job);

        return job;
    }

    /**
     * Get the information about a given task
     *
     * @param namespace The namespace
     * @param id        The ID within the namespace
     * @return
     */
    public TaskFactory getTaskFactory(String namespace, String id) {
        return ScriptContext.get().getFactory(new QName(namespace, id));
    }


    /**
     * Get experimaestro namespace
     */
    @Expose
    public String ns() {
        return Manager.EXPERIMAESTRO_NS;
    }


    Repository getRepository() {
        return context().getRepository();
    }

    public Scheduler getScheduler() {

        return context().getScheduler();
    }

    /**
     * Creates a unique (up to the collision probability) ID based on the hash
     *
     * @param basedir    The base directory
     * @param prefix     The prefix for the directory
     * @param id         The task ID or any other QName
     * @param jsonValues the JSON object from which the hash is computed
     * @return A unique directory
     */
    public Path uniqueDirectory(Scriptable scope, java.nio.file.Path basedir, String prefix, QName id, Object jsonValues) throws IOException, NoSuchAlgorithmException {

        if (basedir == null) {
            if ((basedir = ScriptContext.get().getWorkingDirectory()) == null) {
                throw new XPMRuntimeException("Working directory was not set before unique_directory() is called");
            }
        }
        final Json json = JSUtils.toJSON(scope, jsonValues);
        return Manager.uniquePath(basedir, prefix, id, json, true);
    }


}
