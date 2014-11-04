---
title: Tasks
---

Tasks are simple objects that can be configured and run. Specific behaviours depend on the underlying language that can include different ways to define tasks, but the underlying mechanism, which remains the same, is described in this document.

A task is characterised by the following:

1. It is uniquely identified by a qualified name (URI + name) and a group name;
1. It is configured by a set of XML documents, each associated to a given name and,
        optionally, to a given XML type. A parameter can also be another task, in which case
        the subtask will be run and its output will be used as the parameter.
1. It can be run and its output is valid XML document


# Launcher

  Launchers are used to define how a command is executed. 
  Here is the list of available launchers:
  
  * ShLauncher: runs commands using the `sh` shell
  
  * OARLauncher: runs commands using [OAR](http://oar.imag.fr)


