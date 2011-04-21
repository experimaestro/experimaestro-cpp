package sf.net.experimaestro.manager.js;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelectInfo;
import org.apache.commons.vfs.FileSelector;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.server.RPCTaskManager;

public class ScriptTest {
	private static final String JS_SCRIPT_PATH = "/js";

	@DataProvider(name = "jsProvider")
	public static Object[][] jsProvider() throws FileSystemException {
		final URL url = ScriptTest.class.getResource(JS_SCRIPT_PATH);
		FileSystemManager fsManager = VFS.getManager();
		FileObject file = fsManager.resolveFile(url.toExternalForm());
		FileObject[] files = file.findFiles(new FileSelector() {
			@Override
			public boolean traverseDescendents(FileSelectInfo info)
					throws Exception {
				return true;
			}

			@Override
			public boolean includeFile(FileSelectInfo file) throws Exception {
				return file.getFile().getName().getExtension().equals("js");
			}
		});
		Object[][] r = new Object[files.length][];
		for (int i = r.length; --i >= 0;)
			r[i] = new Object[] { files[i] };

		return r;
	}

	@Test(dataProvider = "jsProvider")
	public void testScript(FileObject file) throws FileSystemException, IOException {
		RPCTaskManager rpc = new RPCTaskManager();
		Repository repository = new Repository();
		rpc.setTaskServer(null, repository);

		InputStreamReader reader = new InputStreamReader(file.getContent().getInputStream());
		char[] cbuf = new char[8192];
		int read = 0;
		StringBuilder builder = new StringBuilder();
		while ((read  = reader.read(cbuf , 0, cbuf.length)) > 0)
			builder.append(cbuf, 0, read);
		
		Map<String, String> environment = System.getenv();
		ArrayList<Object> r = rpc.runJSScript(false, builder.toString(), environment);
		if ((Integer)r.get(0) != 0)
			throw new AssertionError("JavaScript failed");
	}
}
