---
title: Manager
---

# Goals

Tired of bash, python, ... scripts to manage your experiments? Experimaestro manager
gives you a systematic way to write and run experiments where parameters varies.
The goals of the experimaestro manager are to:

* Decompose experiments into a set of tasks
* Standardize the way to run experiments for easier reproduction of experimental results in computing science
* Build a documentation for each task
* Uses the scheduler to run jobs on various computational resources
* Provide multiple ways to write task descriptions. Currently, JavaScript and Python are supported
* Manage data produced by the tasks
* Associate tags to ran tasks

# Main concepts

The main concepts of the manager are:

* All messages between tasks are Json objects. This is described in [this document](json.md).
* Tasks are the unit on which experiments are built, and correspond roughly to either the execution of a long process,
    or to a configuration task. Tasks can be composed in various ways to allow a compact representation of
    experimental plans.  Tasks are described further [this document](tasks.md)
* Computational resources, named `connectors`, define a set of computers - how can a file be stored, how can a
    command line be executed. More information can be found in this [document](../scheduler/connectors.md).
