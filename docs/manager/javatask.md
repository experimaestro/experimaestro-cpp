It is possible to define tasks in Java using introspection


```java
@TaskDescription(id = "mg4j:adhoc",
        output = "irc:run",
        description = "Runs an ad-hoc task",
        registry = Registry.class)
public class Adhoc extends AbstractTask {
    final private static Logger LOGGER = LoggerFactory.getLogger(Adhoc.class);

    @JsonArgument(name = "top_k")
    int capacity = 1500;

    @JsonArgument
    RetrievalModel model;

    @Override
    public JsonElement execute(JsonObject r, ProgressListener progress) throws Throwable {
        ...
    }
}
```