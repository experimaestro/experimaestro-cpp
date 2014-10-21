package sf.net.experimaestro.manager.java;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.bpiwowar.experimaestro.tasks.AbstractTask;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import sf.net.experimaestro.connectors.Connector;
import sf.net.experimaestro.exceptions.ExperimaestroException;
import sf.net.experimaestro.exceptions.XPMRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.utils.Introspection;
import sf.net.experimaestro.utils.introspection.ClassInfo;
import sf.net.experimaestro.utils.introspection.ClassInfoLoader;
import sf.net.experimaestro.utils.log.Logger;

import javax.xml.namespace.NamespaceContext;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static sf.net.experimaestro.scheduler.Scheduler.getVFSManager;

/**
 *
 */
public class JavaTasksIntrospection {
    final static Logger LOGGER = Logger.getLogger();
    public static final String META_INF_PATH = "META-INF/net.bpiwowar.experimaestro/tasks.json";

    FileObject[] classpath;

    public JavaTasksIntrospection(FileObject[] classpath) {
        this.classpath = classpath;
    }


    /**
     * Json description
     */
    static class Description {
        Map<String, String> namespaces;
        ArrayList<String> packages;
        ArrayList<String> classes;
    }

    public static void addToRepository(Repository repository, Connector connector, String[] paths) throws ExperimaestroException, IOException {
        FileObject[] classpath = Arrays.stream(paths).map(path -> {
            try {
                return connector.getMainConnector().resolveFile(path);
            } catch (FileSystemException e) {
                throw new XPMRuntimeException(e, "Could not resolve path %s", path);
            }
        }).toArray(n -> new FileObject[n]);

        final JavaTasksIntrospection javaTasksIntrospection = new JavaTasksIntrospection(classpath);
        final ClassInfoLoader classLoader = new ClassInfoLoader(classpath, getVFSManager(), JavaTasksIntrospection.class.getClassLoader());
        javaTasksIntrospection.addToRepository(repository, classLoader, connector);
    }

    private void addToRepository(Repository repository, ClassInfoLoader cl, Connector connector) throws ExperimaestroException, IOException {
        BiFunction<ClassInfo, Description, ?> f = (classInfo, description) -> {
            // Creates the task factory
            JavaTaskFactory factory = new JavaTaskFactory(this, connector, repository, classInfo, description.namespaces);
            repository.addFactory(factory);
            return true;
        };

        // FIXME: switch to this one
        forEachClass(cl, classpath, f);

    }

    private static void forEachClass(ClassInfoLoader cl, FileObject[] classpath, BiFunction<ClassInfo, Description, ?> f) throws IOException, ExperimaestroException {

        for (FileObject base : classpath) {
            final FileObject infoFile = base.resolveFile(META_INF_PATH);
            if (!infoFile.exists()) {
                continue;
            }

            Type collectionType = new TypeToken<Description>() {
            }.getType();
            final Description description;
            try {
                final Gson gson = new GsonBuilder()
                        .create();
                final InputStreamReader reader = new InputStreamReader(infoFile.getContent().getInputStream());
                description = gson.fromJson(reader, collectionType);
            } catch (IllegalStateException e) {
                throw new ExperimaestroException(e, "Could not read json file %s", infoFile)
                        .addContext("while inspecting resource %s", base);
            }


            final Consumer<Introspection.ClassFile> action = t -> {
                try {
                    final ClassInfo classInfo = new ClassInfo(cl, t.file);
                    if (classInfo.belongs(AbstractTask.class)) {
                        f.apply(classInfo, description);
                    }
                } catch (IOException e) {
                    throw new XPMRuntimeException(e);
                }
            };

            // Get forEachClass through package inspection
            if (description.packages != null) {
                for (String name : description.packages) {
                    Introspection.findClasses(base, 1, name).forEach(action);
                }
            }

            // Get forEachClass directly
            if (description.classes != null) {
                for (String name : description.classes) {
                    final FileObject fileObject = base.resolveFile(name.replace('.', '/'));
                    action.accept(new Introspection.ClassFile(fileObject, name));
                }
            }
        }
    }


}
