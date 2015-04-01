package sf.net.experimaestro.scheduler;

import javax.persistence.*;

/**
 * Base class for token dependencies
 */
@Entity(name = "tokenrequirements")
@Table(name = "tokenrequirements")
@DiscriminatorColumn(name = "type")
@IdClass(TokenRequirementPK.class)
public class TokenRequirement {
    /** The  token */
    private Token token;

    public TokenRequirement(Token token) {
        this.token = token;
    }

    public TokenRequirement() {
    }

    public Token getToken() {
        return token;
    }

}
