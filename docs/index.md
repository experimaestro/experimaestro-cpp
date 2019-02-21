---
title: Introduction
layout: default
---

The overall goals of the experimaestro manager are to:

* Decompose experiments into a set of parameterizable tasks
* Schedule tasks and handle dependencies between tasks
* Avoids to re-run the same task two times by computing unique task IDs dependending on the parameters

## Example

This example shows how to define two tasks:

1. `say` just outputs the `word` argument
1. `concat` concats the outputs of an array of `say` outputs: the `concat` task will wait that the two `say` command finishes successfully before
starting.


```python
{!./languages/python/test/helloworld.py!}
```

To run the experiment, just type

```python
python3 helloworld.py xp workdir
```

Three folders are created, each one corresponding to the execution of a task:

- `workdir/jobs/helloworld.say/287e28c715315ec75b6eb2598d4e565f023c8871` (outputs `hello`)
- `workdir/jobs/helloworld.say/1574e0eae32d4d476be52793edc6ba1e613d5a80` (outputs `world`)
- `workdir/jobs/helloworld.concat/92833d4723ad88eff282faa927c963602ba4b038` (outputs `hello world`)

Notice that the path is made of the task name (prefixed by the namespace), and a hash value
that is unique for a given set of *experimental parameters*. This ensures that the task
is not run two times if a new experiment is run.
