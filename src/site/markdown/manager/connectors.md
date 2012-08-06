<head>
  <title>Connectors</title>
</head>

Connectors describe a computational resource, as for example a single computer or a cluster. It provides
information on:

  * How to access the filesystem
  * How to execute processes

# Accessing a filesystem

  Standard `Commons VFS` filesystem can be accessed.
  
# Executing a command

Connectors are used to connect to a specific computer in order to execute a command or access a file system.
Here is the list of available connectors:

* `ShellLauncher`: Executes a command via a shell script
* `OARLauncher`: Launches a job through OAR
