---
title: Introduction
layout: default
---

# Introduction

!!! warning "Warning"

    This documentation is outdated: experimaestro is now "serverless", which means it is much easier
    to run experiments.

Tired to **manually** manage your experiments? Experimaestro manager
gives you a systematic way to write and run experiments where parameters varies.

The goals of the experimaestro manager are to:

* Decompose experiments into a set of tasks
* Standardize the way to run experiments for easier reproduction of experimental results in computing science
* Uses the scheduler to run jobs on various computational resources
* Provide multiple ways to write task descriptions. Currently, only *Python 3* is supported
* Manage data produced by the tasks
* Associate tags to ran tasks

Experimaestro is an experiment manager, and is composed of

1. A [job scheduler](scheduler/index.html) that handles dependencies between jobs and provides locking mechanisms
   The job scheduler can be controlled via command line (`experimaestro` script) or via the web (where
   you can easily monitor jobs in real time)
2. A [modular experiment description framework](manager/index.html), that allows easy description of the various parts of experiments:
    - Experiments are written in JavaScript or Python
    - Tasks describe the components that can be used

Experimaestro is in an **alpha** state.


!!! info "Main concepts"
    The main concepts of experimaestro are

    * All messages between tasks are structured values. This is described in [this document](manager/json.md).
    * Types and tasks are the unit on which experiments are built, and correspond roughly to either the execution of a long process, or to a configuration. Tasks can be composed in various ways to allow a compact representation of
        experimental plans.  Tasks are described further [this document](manager/definitions.md)
    * Computational resources, named `connectors`, define a set of computers - how can a file be stored, how can a
        command line be executed. More information can be found in this [document](scheduler/connectors.md).
