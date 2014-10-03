<head>
<title>Json Input/Output format</title>
</head>

Each input or output can be characterised by a XML qualified name (namespace and local name).
This allows for a lot of flexibility: input/outputs can be any valid XML document, but can also be typed using the same mechanism than in XML Schema. In that case, validation can be used to ensure the input or output is as expected.

  We will make use of an example of a message

          {
              alpha: 3,
              "{sf.net.experimaestro}type": "{sf.net.experimaestro}:integer",
          }

  This document describes the XML format used by tasks to communicate. Here are the
  key facts:
  
  1. Any Json type is a valid message
  1. Predefined datatypes are defined in Experimaestro (integers, strings, etc.)
  1. Messages can be compared. This is useful when comparing two resources
  1. Special tags and/or attributes allows to
      * Define resources
      * Define the parameters and their values that were used to generate an output



## Datatypes

  XML schema types: `xp:string`, `xp:integer`, etc.
      

