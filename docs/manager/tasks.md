---
title: Tasks
---

Tasks are simple objects that can be configured and run. Specific behaviours depend on the underlying language that can include different ways to define tasks, but the underlying mechanism, which remains the same, is described in this document.

A task is characterised by the following:

1. It is uniquely identified by a qualified name;
1. It is configured by a set of JSON elements, each associated to a given name and,
        optionally, to a given JSON type. A parameter can also be another task, in which case
        the subtask will be run and its output will be used as the parameter.
1. It can be run and its output is JSON


