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

# Reserved JSON keys

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

# Tags

Tags are used to mark parameters that are under study, i.e. parameters that the current experiment are interested in looking at.
When a parameter is tagged, the JSON representation adds a `$tag` entry that gives the name of the parameter
```json
{ "$type": "integer", "$value": 13, "$tag": "x" },
```

This can be achieved by calling the function `tag` from xpm:
[[[{"language": "python", "title": "Python"}
from xpm import tag
tag("x", 1)
[[[{"language": "javascript", "title": "JS"}
tag("x", 1)
]]]

Tags can be retrieved using the `retrieve_tags` function.

This can be achieved by calling the function `tag` from xpm:
[[[{"language": "python", "title": "Python"}
from xpm import retrieve_tags
retrieve_tags(json)
[[[{"language": "javascript", "title": "JS"}
retrieve_tags("x", 1)
]]]

If `json` is
```json
json = {
"x": { "$type": "integer", "$value": 13, "$tag": "x" },
"y": { "$type": "real", "$value": 1.2, "$tag": "y" },
}
```

then the output will be
```json
{
"x": { "$type": "integer", "$value": 13, "$tag": "x" },
"y": { "$type": "real", "$value": 1.2, "$tag": "y" },
"tags": { "x": 1, "y": 1.2 }
}
```

This allows further tasks to use the tags values for building e.g. tables of results.





