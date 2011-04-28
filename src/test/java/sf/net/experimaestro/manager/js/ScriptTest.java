package sf.net.experimaestro.manager.js;

import static java.lang.String.format;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelectInfo;
import org.apache.commons.vfs.FileSelector;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import sf.net.experimaestro.manager.Repository;
import sf.net.experimaestro.server.RPCTaskManager;

public class ScriptTest {
	private static final String JS_SCRIPT_PATH = "/js";

	static public class JavaScriptChecker {

		private FileObject file;
		private String content;
		private Context context;
		private Repository repository;
		private ScriptableObject scope;

		public JavaScriptChecker(FileObject file) throws FileSystemException,
				IOException {
			this.file = file;
			this.content = getFileContent(file);
		}

		@DataProvider
		public Object[][] jsProvider() throws FileSystemException, IOException {
			Pattern pattern = Pattern
					.compile("function\\s+(test_[\\w]+)\\s*\\(");
			Matcher matcher = pattern.matcher(content);
			ArrayList<Object[]> list = new ArrayList<Object[]>();
			list.add(new Object[] { null });

			while (matcher.find()) {
				list.add(new Object[] { matcher.group(1) });
			}

			return list.toArray(new Object[list.size()][]);
		}

		@BeforeTest
		public void enter() {
			context = Context.enter();
			scope = context.initStandardObjects();
			repository = new Repository();

		}

		@AfterTest
		public void exit() {
			Context.exit();
		}

		@Test(dataProvider = "jsProvider")
		public void testScript(String functionName) throws FileSystemException,
				IOException, SecurityException, IllegalAccessException,
				InstantiationException, InvocationTargetException,
				NoSuchMethodException {
			System.err.format("Here function is %s%n", functionName);
			if (functionName == null) {
				Map<String, String> environment = System.getenv();
				XPMObject jsXPM = new XPMObject(context, environment, scope,
						repository, null);
				context.evaluateReader(scope, new StringReader(content),
						file.toString(), 1, null);
			} else {
				Object object = scope.get(functionName, scope);
				assert object instanceof Function : format(
						"%s is not a function", functionName);
				Function function = (Function) object;
				function.call(context, scope, null, new Object[] {});
			}
		}
	}

	@Factory
	public static Object[] jsFactories() throws IOException {
		// Get the JavaScript files
		final URL url = ScriptTest.class.getResource(JS_SCRIPT_PATH);
		FileSystemManager fsManager = VFS.getManager();
		FileObject dir = fsManager.resolveFile(url.toExternalForm());
		FileObject[] files = dir.findFiles(new FileSelector() {
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

		Object[] r = new Object[files.length];
		for (int i = r.length; --i >= 0;)
			r[i] = new JavaScriptChecker(files[i]);

		return r;
	}

	private static String getFileContent(FileObject file)
			throws FileSystemException, IOException {
		InputStreamReader reader = new InputStreamReader(file.getContent()
				.getInputStream());
		char[] cbuf = new char[8192];
		int read = 0;
		StringBuilder builder = new StringBuilder();
		while ((read = reader.read(cbuf, 0, cbuf.length)) > 0)
			builder.append(cbuf, 0, read);
		String s = builder.toString();
		return s;
	}

}
