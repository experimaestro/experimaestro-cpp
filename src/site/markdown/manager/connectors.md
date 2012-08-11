<head>
  <title>Connectors</title>
</head>

Connectors describe a computational resource, as for example a single computer (*single host connector*) or a cluster. It provides
information on how to access the file system and launch processes.

# General description

A connector defines:

* The main connector used to manage the state of a resource associated with this connector;
* A way to select a single host connector given some computational requirements (e.g. 1G memory);
* A way to get a local path given a URL. For example, you could map a path /a/b/c` to `/d/c`

# Executing a command

Connectors are used to connect to a specific computer in order to execute a command or access a file system.
Here is the list of available connectors:

# Finding and describing resources

In order to allow full flexibility, resources are described by a URI, which in most cases is a URL (e.g. `file://path/to/my/file`). In order to map back a URI to a connector, Experimaestro has a set of handlers associated with protocols (in order to deal with an URL), and custom handlers can be added.

The same resource can be at different paths depending on the host (i.e. via mounts). In order to deal with that, 

# Available connectors

## Single host connector

### Local machine

The Localhost connector is the simplest since it stores files and launch processes locally.

### SSH

## Group

Groups define a set of connectors that can be used to launch processes. A main connector has to be defined in order to hold the state of the launched processes or created resources.

# Launchers

Launchers are used to launch a process on a given single host connector.

## Sh launcher

This is the default launcher on UNIX, linux and OS X computers.

## OAR launcher

