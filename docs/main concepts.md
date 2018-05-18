
!!! info "Main concepts"
    The main concepts of experimaestro are

    * Experimental pararameters are structured values, and can thus be nested. This is described in [this document](manager/json.md).
    * Types and tasks are the unit on which experiments are built:
        - Type = configuration
        - Task = a process to be executed
    * Tasks are composed by passing them as parameters to other tasks. Tasks are described further [this document](manager/definitions.md)
    * Computational resources are defined by *connectors* and *launchers*, define a set of computers - how can a file be stored, how can a command line be executed. More information can be found in this [document](scheduler/connectors.md).

