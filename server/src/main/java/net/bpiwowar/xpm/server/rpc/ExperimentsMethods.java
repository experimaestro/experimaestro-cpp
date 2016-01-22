package net.bpiwowar.xpm.server.rpc;

import net.bpiwowar.xpm.manager.experiments.Experiment;
import net.bpiwowar.xpm.manager.scripting.ClassDescription;
import net.bpiwowar.xpm.manager.scripting.Scripting;
import net.bpiwowar.xpm.utils.StreamUtils;

import java.util.stream.Stream;

/**
 * Methods related to experiments
 */
@JsonRPCMethodsHolder("experiments")
public class ExperimentsMethods {
    /** Return registered classes */
    @RPCMethod(help = "Cleanup old experiments")
    void cleanup(
            @RPCArgument(name = "simulate", help = "If true, don't perform the action")
            boolean simulate,
            @RPCArgument(name = "identifier", help = "Identifier of the experiment")
            String identifier
    ) {

        // Select all resources that are part of the last experiment, but not the other
        // ones
//        StreamUtils.stream(Experiment.findAllByIdentifier(identifier))
//                .skip(1);

    }
}
