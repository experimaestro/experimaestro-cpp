---
title: Manager
---

# Goals

Tired of bash, python, ... scripts to manage your experiments? Experimaestro manager
gives you a systematic way to write and run experiments where parameters varies.
The goals of the experimaestro manager are to:

* Decompose experiments into a set of parametrized tasks
* Standardize the way to run experiments for easier reproduction of experimental results in computing science
* Build a documentation for each task
* Uses the scheduler to run jobs on various computational resources
* Provide multiple way to write tasks. Currently, only JavaScript is supported but this could be extend to Python.
* Handle easy to read & write experimental plans. For example,
    `{"collection.id": ["trec.1/adhoc","trec.2/adhoc"], "qir.weighting-scheme": ["tf-idf","tf"]}`
    will run four experiments where the different parameters (id and weighting schemes) are varied
* (_Not implemented_) Manage data produced by the tasks
  
# Main concepts

The main concepts of the manager are:

* All messages between tasks are Json objects. This is described in [this document](json.html).
* Tasks are the unit on which experiments are built, and correspond roughly to either the execution of a long process,
    or to a configuration task. Tasks can be composed in various ways to allow a compact representation of
    experimental plans.  Tasks are described further [this document](tasks.html)
* Computational resources, named `connectors`, define a set of computers - how can a file be stored, how can a
    command line be executed. More information can be found in this [document](connectors.html).
