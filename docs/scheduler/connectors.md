---
title: Connectors and Launchers
---

[Connectors](#connectors) describe a computational resource, as for example a single computer (*single host connector*) or a set of computers. It provides information on how to access the file system and execute commands. At the moment, the following connectors are defined:

- Localhost
- SSH

**Launchers** are used to launch commands:

- Direct launcher: standard command launching
- OAR: uses OAR to launch jobs on a cluster

<a name="connectors"></a>
# Connectors 

A connector defines:

* The main connector used to manage the state of a resource associated with this connector;
* A way to select a single host connector given some computational requirements (e.g. 1G memory);

## Localhost

## SSH


<a name="launchers"></a>
# Launchers 

Connectors are used to connect to a specific computer in order to execute a command or access a file system.
Here is the list of available connectors:

## Direct launcher
* `DirectLauncher`: Executes a command via a shell script

## OAR

* `OARLauncher`: Launches a job through OAR

# Finding and describing resources

In order to allow full flexibility, resources are described by a URI, which in most cases is a URL (e.g. `file://path/to/my/file`). In order to map back a URI to a connector, Experimaestro has a set of handlers associated with protocols (in order to deal with an URL), and custom handlers can be added.

The same resource can be at different paths depending on the host (i.e. via mounts). In order to deal with that,

# Available connectors

## Group

Defines a set of connectors

## Localhost

## SSH


# Launcher

  Launchers are used to define how a command is executed.
  Here is the list of available launchers:

  * ShLauncher: runs commands using the `sh` shell

  * OARLauncher: runs commands using [OAR](http://oar.imag.fr)

