<head>
<title>XML Input/Output format</title>
</head>

Each input or output can be characterised by a XML qualified name (namespace and local name).
  This allows for a lot of flexibility: input/outputs can be any valid XML document, but can
  also be typed using the same mechanism than in XML Schema. In that case, validation can be used
  to ensure the input or output is as expected.

  We will make use of an example of a message

          <example xmlns="http://my.ns" xmlns:xp="sf.net.experimaestro" xp:resource="file:///home/xpm/output">
              <!-- A simple typed value -->
          	<xp:value id="id" value="abc1" type="xs:string"/>

              <!-- Another value -->
              <alpha xp:type="xs:integer">3</alpha>

              <!-- The format of the resource, ignored when comparing two messages -->
              <format xp:ignore="yes">2</format>

          </example>

  This document describes the XML format used by tasks to communicate. Here are the
  key facts:
  
  1. Any valid XML document is a valid message
  1. Predefined datatypes are defined in Experimaestro (integers, strings, etc.)
  1. Messages can be compared. This is useful when two resources
  1. Special tags and/or attributes allows to
      * Define resources
      * Define the parameters and their values that were used to generate an output


## Comparing XML message

  A message is equal to another one its serialized form is the same. However,
  some parts of the message should be ignored in some cases (e.g. the path to a resource, etc.).
  To do so, use the attribute `xp::ignore`

  This qualified name is important to distinguish the different data types.
  The XML is an output with the name `example` within the namespace `http://my.ns`.
  It is further identified by the `id` of value `abc1`.


## Resources

A resource can be associated to any XML element by using the attribute
     `xp:resource`.

When an output is produced, resources are automatically retrieved.

## Datatypes

  XML schema types: `xs:string`, `xs::integer`, etc.
      

