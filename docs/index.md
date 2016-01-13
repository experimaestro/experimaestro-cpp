---
title: Introduction
layout: default
---

# Presentation

Experimaestro is an experiment manager, and is composed of

1. A [job scheduler](scheduler/index.html) that handles dependencies between jobs and provides locking mechanisms
   The job scheduler can be controlled via command line (`experimaestro` script) or via the web (where
   you can easily monitor jobs in real time)
2. A [modular experiment description framework](manager/index.html), that allows easy description of the various parts of experiments:
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
<include file="src/test/resources/js/plan_composition.js" id="task"/>

Then, we build an experimental plan composing the two tasks. This is done
by first building `plan1` that calls the addition operation, and
then by building `plan2` that takes as input the output of `plan1`.

<include file="src/test/resources/js/plan_composition.js" id="run"/>

# Installation

Experimaestro is implemented using several languages and thus, you
have to install multiple ecosystem with the right version.

## Python 3

You need a working `python3` interpreter on your system along with `pip3`.

To install required python's modules type:

```sh
pip3 install -r server/scripts/requirements.txt
```

## Java (≥ 8)

You need a working *Java* installation (version ≥ 1.8) on your machine. [Gradle](https://gradle.org) is
used for building but is not a requirement.

[[[{"language": "sh", "title": "*nix"}
# From the project's root directory.
./gradlew installDist
[[[{"language": "bat", "title": "windows"}
gradlew.bat installDist
]]]

This will build and install all the dependencies into `server/build/install/experimaestro-server`. The command `experimaestro` is located in `server/build/install/experimaestro-server/bin/experimaestro`.

Note that on a Linux box if you are not root and the alternative link
is not set to the proper version (you can inspect that using
`update-alternatives --display java`), you won't have the permission
to update the link manually using `update-alternatives --config java`.
The work around is to set the environment variable `JAVA_HOME` when
calling maven. For example:

To locate the path of to set to `JAVA_HOME` you can do `locate jdk | less`.

## shell completion

To enable shell completion follow instructions in
[argcomplete](https://pypi.python.org/pypi/argcomplete) documentation.

### ZSH

Add to `.zshrc`:

```sh
  # Register python completion
  if type register-python-argcomplete &> /dev/null
  then
    eval "$(register-python-argcomplete 'experimaestro')"
  fi
```

* Re-launch your shell: `exec zsh`

# Test your installation

To test your installation is working properly you can try to start a
server like this:


```sh
experimaestro --verbose --debug start-server
```

You should get something like:

```
INFO:root:Starting with gradle
INFO:root:Waiting for server to start (PID=37851)...
INFO:root:Server started...
```
