# Experimaestro

<!-- A [full documentation is available](http://bpiwowar.github.io/experimaestro/) -->

Experimaestro is an experiment manager based on a server that contains a job scheduler (job dependencies, locking mechanisms) and a framework to write the experiments.

- A **job scheduler** that handles dependencies between jobs and provides locking mechanisms

- A **modular experiment description framework**, that allows easy description of the various parts of experiments:
    - Experiments are written in any language (currently supported: Python)
    - Tasks can be composed through imperative programming

Experimaestro is in a **alpha** state currently.

<!-- ![A screenshot of experimaestro running](docs/xpm-screenshot.png) -->
