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
import org.mozilla.javascript.*;
import org.mozilla.javascript.annotations.JSFunction;
import sf.net.experimaestro.scheduler.Scheduler;
import sf.net.experimaestro.utils.JSUtils;

/**
 * @author B. Piwowarski <benjamin@bpiwowar.net>
 * @date 26/11/12
 */
public class JSResource extends JSObject implements Wrapper {

    public static final String JSCLASSNAME = "FileObject";
    private FileObject file;



    public JSResource() {}

    public JSResource(FileObject file) {
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
    public static Scriptable getParent(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
        if (args.length != 0)
            throw new IllegalArgumentException("Expected no argument for FileObject.getAncestor - got " + args.length );
        return getAncestor(cx, thisObj, 1);
    }

    @JSFunction("get_ancestor")
    static public Scriptable getAncestor(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
        if (args.length != 1)
            throw new IllegalArgumentException("Expected one argument for FileObject.getAncestor - got " + args.length);

        int level = JSUtils.getInteger(args[0]);
        if (level < 0)
            throw new IllegalArgumentException("Level is negative (" + level + ")");

        return getAncestor(cx, thisObj, level);
    }

    static private Scriptable getAncestor(Context cx, Scriptable thisObj, int level) throws FileSystemException {
        FileObject file = ((JSResource)thisObj).file;
        while (--level >= 0)
            file = file.getParent();

        return cx.newObject(thisObj, JSCLASSNAME, new Object[] { file } );
    }

    @JSFunction("path")
    static public Scriptable path(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws FileSystemException {
        final JSResource _this = (JSResource) thisObj;

        FileObject file = _this.file;
        for (int i = 0; i < args.length; i++) {
            String name = Context.toString(args[i]);
            file = file.resolveFile(name);
            System.err.format("File is now [%s]%n", file);
        }

        return cx.newObject(thisObj, JSCLASSNAME, new Object[] { file } );
    }

    @JSFunction("mkdirs")
    public void mkdirs() throws FileSystemException {
        file.createFolder();
    }
}
