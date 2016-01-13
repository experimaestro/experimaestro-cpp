---
title: Json Input/Output format
---

# Introduction

JSON is used to describe resources. Special names are reserved by experimaestro in order to ensure

We will make use of an example of a message

```js
{
    alpha: 3,
    "{net.bpiwowar.xpm}type": "{net.bpiwowar.xpm}:integer",
}
```

  This document describes the JSON format used by tasks to communicate. Here are the
  key facts:

  1. Any Json type is a valid message
  1. Predefined datatypes are defined in Experimaestro (integers, strings, etc.)
  1. Messages can be compared. This is useful when comparing two resources
  1. Special tags and/or attributes allows to
      * Define resources
      * Define the parameters and their values that were used to generate an output


# Signature

A signature is computed by:

1. Stripping paths


## Datatypes

  schema types: `xp:string`, `xp:integer`, etc.
