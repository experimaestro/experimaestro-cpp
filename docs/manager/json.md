---
title: Json Input/Output format
---

# Introduction

JSON is used to describe resources. Special names are reserved by experimaestro in order to ensure

We will make use of an example of a message

```json
{
    "alpha": 3,
    "$type": "integer",
}
```

  This document describes the JSON format used by tasks to communicate. Here are the
  key facts:

  1. Any Json type is a valid message
  1. Predefined datatypes are defined in Experimaestro (integers, strings, etc.). Types
     are given by the `$type` tag, or by the type of the value when using
     JSON primitives.
  1. Messages can be compared. This is useful when comparing two resources
  1. Special tags and/or attributes allows to
      * Define resources
      * Define the parameters and their values that were used to generate an output

# Reserved names

Some keys have a special meaning in experimaestro:

- `$value` correspond to the simple value of the JSON
- `$type` correspond to the type of the JSON
- `$tag` correspond to a tag - those can be used to mark special values (e.g. parameters under study)
- `$resource` corresponds to an experimaestro resource

# Predefined types

- `string` for strings
- `integer` for boolean
- `integer` for integers
- `real` for reals
- `path` for anything corresponding to a file or a directory (even on the network)


# Signature

Each JSON can be reduced to a signature that corresponds to the JSON describing the factor
of variation of the outcome of an experiment.

1. Replacing simple values by their value
1. Stripping paths
1. Stripping all keys beginning by `$` except `$type`

```json
{
  "x": { "$type": "integer", "$value": 13 },
  "y": {
    "k": 1
  }
  "path": { "$type": "path", "$value": "/path/to/a/file" },
  "$resource": "/uri/of/resource",
}
```

The signature will be

```json
{
  "x": 13,
  "y": { "k": 1 }
}
```

