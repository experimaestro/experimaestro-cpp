<head>
  <title>Connectors</title>
</head>

Connectors describe a computational resource, as for example a single computer or a cluster. It provides
information on:

1. How to access the file system
1. How to execute processes
1. How to describe and locate a resource

# Accessing a file system

In the simplest case, standard `Commons VFS` files system can be accessed.
  
# Executing a command

Connectors are used to connect to a specific computer in order to execute a command or access a file system.
Here is the list of available connectors:

* `ShellLauncher`: Executes a command via a shell script
* `OARLauncher`: Launches a job through OAR

# Finding and describing resources

In order to allow full flexibility, resources are described by a URI, which in most cases is a URL (e.g. `file://path/to/my/file`). In order to map back a URI to a connector, Experimaestro has a set of handlers associated with protocols (in order to deal with an URL), and custom handlers can be added.

The same resource can be at different paths depending on the host (i.e. via mounts). In order to deal with that, 