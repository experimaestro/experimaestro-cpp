<head><title>Home</title></head>

# Presentation

Experimaestro is an experiment manager, and is composed of 

1. A [job scheduler](scheduler/index.md) that handles dependencies between jobs and provides locking mechanisms
   The job scheduler can be controlled via command line (`experimaestro` script) or via the web (where
   you can easily monitor jobs in real time)
2. A [modular experiment description framework](manager/index.md), that allows easy description of the various parts of experiments:
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

## Python

You need a working `python3` interpreter on your system along with `pip3`.

To install required python's modules type:

```sh
pip3 install -r requirements.txt
```

## Java

You need a working *Java 1.8* installation on your machine and
*Maven 3* or higher.

```sh
mvn compile
mvn install
```

Note that on a Linux box if you are not root and the alternative link
is not set to the proper version (you can inspect that using
`update-alternatives --display java`), you won't have the permission
to update the link manually using `update-alternatives --config java`.
The work around is to set the environment variable `JAVA_HOME` when
calling maven. For example:

```sh
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn compile
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn install
```

To locate the path of to set to `JAVA_HOME` you can do `locate jdk | less`.

## shell completion

To enable shell completion follow instructions in
[argcomplete](https://pypi.python.org/pypi/argcomplete) documentation.

Here is just what I did for *zsh*:
* Add something like this to my `.zshrc`:
  ```sh
  # Register python completion
  if type register-python-argcomplete &> /dev/null
  then
    eval "$(register-python-argcomplete 'experimaestro')"
  fi
  ```
* Re-launch your shell: `exec zsh`
