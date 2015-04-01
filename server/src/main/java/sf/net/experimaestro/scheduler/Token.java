package sf.net.experimaestro.scheduler;

import org.json.simple.JSONObject;

import javax.persistence.*;
import java.io.IOException;

/**
 * Base class for all tokens
 */
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Token {
    /**
     * The token ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;

    @Column(unique=true)
    String identifier;

    protected Token() {}

    public Token(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Get a JSON representation of the object
     *
     * @return
     * @throws IOException
     */
    public JSONObject toJSON() throws IOException {
        JSONObject object = new JSONObject();
        object.put("id", id);
        object.put("publicID", identifier);
        return object;
    }

    abstract protected boolean doUpdateStatus() throws Exception;
}
