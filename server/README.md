experimaestro
=============

[![Build Status](https://travis-ci.org/bpiwowar/experimaestro.svg?branch=master)](https://travis-ci.org/bpiwowar/experimaestro)

Experimaestro is an experiment manager, and is composed of

- A [job scheduler](scheduler/index.md) that handles dependencies between jobs and provides locking mechanisms
   The job scheduler can be controlled via command line (`experimaestro` script) or via the web (where
   you can easily monitor jobs in real time)
- A [modular experiment description framework](manager/index.md), that allows easy description of the various parts of experiments:
    - Experiments are written in JavaScript
    - Tasks describe the components that can be used, take as input json objects and produce json objets as output
    - Tasks can be composed through the definition of an experimental plan

Both modules can be used independently even though they were designed to work together.

Experimaestro is in a **beta** state - which means that you might experience some bugs
while using it; but as I use it on a daily basis, there number and importance is
going down each day.

# Example

This is an example of how an experimental plan is built.

First, we define two tasks. In the example,
the two tasks are multiplication and addition of two numbers, but in practice
task launch jobs through the scheduler.

<div class="code" href="src/test/resources/js/plan_composition.js"/>

Then, we build an experimental plan composing the two tasks. This is done
by first building `plan1` that calls the addition operation, and
then by building `plan2` that takes as input the output of `plan1`.

<div class="code" href="src/test/resources/js/plan_composition.js#run"></div>