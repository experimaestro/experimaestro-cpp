package sf.net.experimaestro.connectors;

import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;

/**
 * Created by bpiwowar on 24/10/14.
 */
public interface AgentRepositoryFactory {
    IdentityRepository create(JSch jsch);
}
