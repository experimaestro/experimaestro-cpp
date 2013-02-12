/*
 * This file is part of experimaestro.
 * Copyright (c) 2012 B. Piwowarski <benjamin@bpiwowar.net>
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

package sf.net.experimaestro.manager.js;

import com.thaiopensource.relaxng.edit.SchemaCollection;
import com.thaiopensource.relaxng.input.InputFailedException;
import com.thaiopensource.relaxng.input.InputFormat;
import com.thaiopensource.relaxng.input.parse.compact.CompactParseInputFormat;
import com.thaiopensource.relaxng.output.LocalOutputDirectory;
import com.thaiopensource.relaxng.output.OutputDirectory;
import com.thaiopensource.relaxng.output.OutputFailedException;
import com.thaiopensource.relaxng.output.xsd.XsdOutputFormat;
import com.thaiopensource.relaxng.translate.Formats;
import com.thaiopensource.relaxng.translate.util.InvalidParamsException;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.xerces.impl.xs.XSLoaderImpl;
import org.apache.xerces.xs.XSModel;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import sf.net.experimaestro.exceptions.ExperimaestroRuntimeException;
import sf.net.experimaestro.manager.Manager;
import sf.net.experimaestro.manager.Module;
import sf.net.experimaestro.manager.QName;
import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.JSUtils;
import sf.net.experimaestro.utils.log.Logger;

import java.io.*;

public class JSModule extends JSObject {
    final static private Logger LOGGER = Logger.getLogger();
    private final XPMObject xpm;

    Module module;

    /**
     * Creates a new module from a JavaScript description
     *
     * @param jsScope  The scope where the object was created
     * @param jsObject The object itself
     */
    public JSModule(XPMObject xpm, Repository repository, Scriptable jsScope,
                    NativeObject jsObject) {
        this.xpm = xpm;
        module = new Module((QName) JSUtils.get(jsScope, "id", jsObject));

        module.setName(JSUtils.toString(JSUtils.get(jsScope, "name", jsObject)));
        module.setDocumentation(JSUtils
                .toDocument(jsScope, JSUtils.get(jsScope, "description", jsObject),
                        new QName(Manager.EXPERIMAESTRO_NS, "documentation")));

        // Set the parent
        Module parent = getModule(repository, JSUtils.get(jsScope, "parent", jsObject, null));
        if (parent != null)
            module.setParent(parent);

    }

    static public enum Type {
        SCHEMA,
        RELAXNG
    }

    public void add_schema(@JSArgument(name = "path", help = "Path relative to the script") final JSFileObject path) throws IOException, SAXException, InputFailedException, OutputFailedException, InvalidParamsException {
        final String extension = path.getFile().getName().getExtension();
        if (extension.equals("rnc")) {
            add_schema(path, Type.RELAXNG);

        } else add_schema(path, Type.SCHEMA);
    }

    /**
     * Adds an XML Schema declaration
     */
    @JSHelp(value = "Add a schema in the module")
    public void add_schema(
            @JSArgument(name = "path", help = "Path relative to the script") final JSFileObject _file,
            @JSArgument(name = "type") final Type type
    ) throws IOException, InputFailedException, SAXException, InvalidParamsException, OutputFailedException {
        File outFile = null;

        FileObject file = _file.getFile(), origFile = _file.getFile();

        try {
            LOGGER.info("Loading XSD with xpath [%s]", file);

            switch (type) {
                case SCHEMA:
                    break;
                case RELAXNG:
                    CompactParseInputFormat inFormat = new CompactParseInputFormat();

                    File inFile = null;
                    try {
                        inFile = File.createTempFile("xpm", ".rnc");
                        outFile = File.createTempFile("xpm", ".xsd");

                        final InputStream inputStream = file.getContent().getInputStream();
                        final FileOutputStream output = new FileOutputStream(inFile);
                        IOUtils.copy(inputStream, output);
                        output.close();

                        ErrorHandler eh = new MyErrorHandler(xpm.getRootLogger());

                        final InputFormat in = Formats.createInputFormat("rnc");
                        final SchemaCollection sc = in.load(inFile.toURI().toString(), new String[]{}, "xsd", eh, null);

                        XsdOutputFormat of = new XsdOutputFormat();
                        OutputDirectory od = new LocalOutputDirectory(sc.getMainUri(),
                                new File(outFile.toString()),
                                "xsd",
                                DEFAULT_OUTPUT_ENCODING,
                                DEFAULT_LINE_LENGTH,
                                DEFAULT_INDENT);
                        of.output(sc, od, new String[]{}, "rng", eh);
//                        new Driver().run(new String[] {"-I", "rnc", "-O", "xsd", inFile.getAbsolutePath(), outFile.getAbsolutePath()});
                        file = Scheduler.getVFSManager().toFileObject(outFile);
                    } finally {
                        if (inFile != null)
                            inFile.delete();
                    }
                    break;
            }


            // Load & add Add to the repository
            XSLoaderImpl xsLoader = new XSLoaderImpl();
            XSModel xsModel = xsLoader.load(new MyLSInput(file));
            if (xsModel == null)
                throw new ExperimaestroRuntimeException("Error while parsing %s", origFile);
            xpm.getRepository().addSchema(module, xsModel);
        } finally {
            if (outFile != null)
                outFile.delete();

        }

    }

    private static final String DEFAULT_OUTPUT_ENCODING = "UTF-8";
    private static final int DEFAULT_LINE_LENGTH = 72;
    private static final int DEFAULT_INDENT = 2;


    static public Module getModule(Repository repository, Object parent) {
        if (parent == null)
            return null;

        if (parent instanceof Module)
            return (Module) parent;

        if (parent instanceof QName) {
            Module module = repository.getModules().get(parent);
            if (module == null)
                throw new ExperimaestroRuntimeException("No module of name [%s]",
                        parent);
            return module;
        }


        throw new ExperimaestroRuntimeException(
                "Cannot search for module with type %s [%s]",
                parent.getClass(), parent);
    }

    private static class MyLSInput implements LSInput {
        private final FileObject file;

        public MyLSInput(FileObject file) {
            this.file = file;
        }

        @Override
        public Reader getCharacterStream() {
            return null;
        }

        @Override
        public void setCharacterStream(Reader reader) {
            throw new AssertionError("Should not be called");
        }

        @Override
        public InputStream getByteStream() {
            try {
                return file.getContent().getInputStream();
            } catch (Exception e) {
                throw new ExperimaestroRuntimeException(e);
            }
        }

        @Override
        public void setByteStream(InputStream inputStream) {
            throw new AssertionError("Should not be called");
        }

        @Override
        public String getStringData() {
            return null;
        }

        @Override
        public void setStringData(String s) {
            throw new AssertionError("Should not be called");
        }

        @Override
        public String getSystemId() {
            return null;
        }

        @Override
        public void setSystemId(String s) {
            throw new AssertionError("Should not be called");
        }

        @Override
        public String getPublicId() {
            return null;
        }

        @Override
        public void setPublicId(String s) {
            throw new AssertionError("Should not be called");
        }

        @Override
        public String getBaseURI() {
            return null;
        }

        @Override
        public void setBaseURI(String s) {
            throw new AssertionError("Should not be called");
        }

        @Override
        public String getEncoding() {
            return null;
        }

        @Override
        public void setEncoding(String s) {
            throw new AssertionError("Should not be called");
        }

        @Override
        public boolean getCertifiedText() {
            return false;
        }

        @Override
        public void setCertifiedText(boolean b) {
            throw new AssertionError("Should not be called");
        }
    }

    static private class MyErrorHandler implements ErrorHandler {
        Logger logger;

        private MyErrorHandler(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            logger.warn(exception);
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            logger.error(exception);
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            logger.fatal(exception);
        }
    }
}
