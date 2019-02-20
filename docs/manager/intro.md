---
title: Tasks
---

# Manager

The manager is based on the definition of **types** and **tasks**. Tasks are
types that are associated with a way to execute a process.

Types and tasks can be defined in a `YAML` file, or, when bindings exist,
directly in the host language. Available bindings are:

- [Python](python.md)

## Types


A type is defined by:

- A qualified name
- A parent
- A list of arguments 

!!!example
    
    The following `YAML` code defines two types, one

    ```yaml
    my.model.abstract: {}

    my.model:
      parent: "my.model.abstract"
      arguments:
        parameters
          count:
            default: 10
            help: Number of nuggets to include
            type: integer
          $seed:
            defaut: 0
            type: int
            help: Seed for the model parameters
    ```

### Basic types

These types are predefined

- `int`
- `real`
- `string`

### Complex types

- `array`
- `map`


## Tasks

A task extends a type by associating it with 
command to run.


```yaml
tasks:
  indri.index:
    type: indri.index
    command:
      - pathref: indri.PYTHON_PATH
      - pathref: indri.MAIN_PY
      - run
      - indri.index
      - type: parameters
```