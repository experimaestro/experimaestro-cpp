package sf.net.experimaestro.manager.java;

/**
 * Created by bpiwowar on 7/10/14.
 */
public class PathArgument {
    final String jsonName;
    final String relativePath;

    public PathArgument(String jsonName, String relativePath) {
        this.jsonName = jsonName;
        this.relativePath = relativePath;
    }
}
