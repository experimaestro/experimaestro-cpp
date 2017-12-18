# Introduction

Definitions files are in YAML or JSON, and describe the schema of types and tasks.
In this document, the examples are in YAML, since the syntax is lighter.

A definition file is divided in two sections, `types` and `tasks`, which
are two dictionaries mapping a type or a task id to its definition. The definition
is described in

```yaml
types:
    type1:
        ...
    type2:
        ...

tasks:
    task1:
        ...
    task2:
        ...
```

# Types

A type is defined by a dictionary, whose keys can be:

- `arguments` The arguments that define the type
- `type` The type (by defaut, `any`)
- `help` A markdown formatted help message for this argument
- `default` or `generator` provides a default value: `default` is
  an object corresponding to the default value

## Generators

At the moment, only one generator is defined.

```yaml
generator:
    type: path
    name: basename for the generated path
```
# Tasks
