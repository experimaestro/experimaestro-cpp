---
title: Introduction
layout: default
---

# Introduction

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

Experimaestro is an experiment manager, and is composed of

1. A [job scheduler](scheduler/index.html) that handles dependencies between jobs and provides locking mechanisms
   The job scheduler can be controlled via command line (`experimaestro` script) or via the web (where
   you can easily monitor jobs in real time)
2. A [modular experiment description framework](manager/index.html), that allows easy description of the various parts of experiments:
    - Experiments are written in JavaScript or Python
    - Tasks describe the components that can be used
    - Composition: tasks take as input json and output json
    - Tasks can be composed through the definition of an experimental plan

Both modules can be used independently even though they were designed to work together.

Experimaestro is in a **beta** state - which means that you might experience some bugs
while using it; but as I use it on a daily basis, there number and importance is
going down each day.


!!! info "Main concepts"
    The main concepts of experimaestro are

    * All messages between tasks are **JSON** objects. This is described in [this document](json.md).
    * Tasks are the unit on which experiments are built, and correspond roughly to either the execution of a long process,
        or to a configuration task. Tasks can be composed in various ways to allow a compact representation of
        experimental plans.  Tasks are described further [this document](tasks.md)
    * Computational resources, named `connectors`, define a set of computers - how can a file be stored, how can a
        command line be executed. More information can be found in this [document](../scheduler/connectors.md).
