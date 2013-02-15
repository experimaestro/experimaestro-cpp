<head><title>Home</title></head>
Experimaestro is an experiment manager, and is composed of 

1. A job scheduler that handles dependencies between jobs and provides locking mechanisms
2. A modular experiment description framework, that allows easy description of the various parts of an experiments.

Both modules can be used independently even though they were designed to work together.

# Examples

## Simple example

Here is an example of an experiment in JavaScript

<include file="src/site/code/example.js"/>

## Composing experimental plans

Composing experimental plans is easy. 

First, we define two tasks (a multiplication and addition of two numbers)
<include file="src/test/resources/js/plan_composition.js" id="task"/>

Then, we build an experimental plan composing the two tasks
<include file="src/test/resources/js/plan_composition.js" id="run"/>


