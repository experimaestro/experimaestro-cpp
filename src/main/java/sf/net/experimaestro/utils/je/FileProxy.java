package sf.net.experimaestro.utils.je;

import java.io.File;

import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PersistentProxy;

@Persistent(proxyFor = File.class)
public class FileProxy implements PersistentProxy<File> {
	String absolutePath;
	
	@Override
	public void initializeProxy(File object) {
		absolutePath = object.getAbsolutePath();
	}

	@Override
	public File convertProxy() {
		return new File(absolutePath);
	}

}
