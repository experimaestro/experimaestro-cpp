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

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.annotations.JSFunction;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.JSUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 26/11/12
 */
public class JSFileObject extends JSObject implements Wrapper {

    public static final String JSCLASSNAME = "FileObject";
    private FileObject file;

    public JSFileObject() {}

    public JSFileObject(FileObject file) {
        this.file = file;
    }

    public void jsConstructor(Object file) throws FileSystemException {
        final Object unwrap = JSUtils.unwrap(file);
        if (unwrap instanceof FileObject) {
            this.file = (FileObject) unwrap;
        } else {
            final String s = JSUtils.toString(unwrap);
            this.file = Scheduler.getVFSManager().resolveFile(s);
        }
    }


    @Override
    public FileObject unwrap() {
        return file;
    }

    @Override
    @JSFunction("toString")
    public String toString() {
        return file == null ? "[null]" : file.toString();
    }

    @JSFunction("toSource")
    public String toSource() {
        return String.format("new FileObject(%s)", file.toString());
    }


    @JSFunction("get_parent")
    @JSHelp(value = "Get the parent file object")
    public static Scriptable getParent(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
        if (args.length != 0)
            throw new IllegalArgumentException("Expected no argument for FileObject.getAncestor - got " + args.length );
        return getAncestor(cx, thisObj, 1);
    }

    @JSFunction("resolve")
    public static Scriptable resolve(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
        if (args.length != 1)
            throw new IllegalArgumentException("Expected one argument for FileObject.resolve- got " + args.length);

        FileObject file = ((JSFileObject)thisObj).file;
        final String path = JSUtils.toString(args[0]);
        return cx.newObject(thisObj, JSCLASSNAME, new Object[] { file.resolveFile(path) } );
    }

    @JSFunction("get_ancestor")
    @JSHelp(value = "Get the n<sup>th</sup> ancestor of this file object",
            arguments = @JSArguments(@JSArgument(type="Integer",name="levels")))
    static public Scriptable getAncestor(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
        if (args.length != 1)
            throw new IllegalArgumentException("Expected one argument for FileObject.getAncestor - got " + args.length);

        int level = JSUtils.getInteger(args[0]);
        if (level < 0)
            throw new IllegalArgumentException("Level is negative (" + level + ")");

        return getAncestor(cx, thisObj, level);
    }

    static private Scriptable getAncestor(Context cx, Scriptable thisObj, int level) throws FileSystemException {
        FileObject file = ((JSFileObject)thisObj).file;
        while (--level >= 0)
            file = file.getParent();

        return cx.newObject(thisObj, JSCLASSNAME, new Object[] { file } );
    }

    @JSFunction("path")
    @JSHelp(value = "Returns a file object corresponding to the path given in the arguments. " +
            "Each name given corresponds to a new path component starting from this file object.",
            arguments = @JSArguments({@JSArgument(type="String", name="name"), @JSArgument(name="...")}))
    static public Scriptable path(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
        final JSFileObject _this = (JSFileObject) thisObj;

        FileObject file = _this.file;
        for (int i = 0; i < args.length; i++) {
            String name = Context.toString(args[i]);
            file = file.resolveFile(name);
            System.err.format("File is now [%s]%n", file);
        }

        return cx.newObject(thisObj, JSCLASSNAME, new Object[] { file } );
    }

    @JSFunction("mkdirs")
    @JSHelp("Creates this folder, if it does not exist.  Also creates any ancestor\n" +
            "folders which do not exist.  This method does nothing if the folder\n" +
            "already exists.")
    public void mkdirs() throws FileSystemException {
        file.createFolder();
    }

    @JSFunction("exists")
    public boolean exists() throws FileSystemException {
        return file.exists();
    }

    @JSFunction("get_size")
    public long get_size() throws FileSystemException {
        return file.getContent().getSize();
    }

    @JSFunction("output_stream")
    @JSArguments({})
    static public PrintWriter output_stream(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
        final JSFileObject _this = (JSFileObject) thisObj;
        final XPMObject xpm = ((XPMObject.JSInstance)thisObj.getParentScope().get("xpm", thisObj.getParentScope())).xpm;
        final OutputStream output = _this.file.getContent().getOutputStream();
        xpm.register(output);
        return new PrintWriter(output);
    }

    @JSFunction("input_stream")
    @JSArguments({})
    static public BufferedReader input_stream(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
        final JSFileObject _this = (JSFileObject) thisObj;
        final XPMObject xpm = ((XPMObject.JSInstance)thisObj.getParentScope().get("xpm", thisObj.getParentScope())).xpm;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(_this.file.getContent().getInputStream()));
        xpm.register(reader);
        return reader;
    }

}
