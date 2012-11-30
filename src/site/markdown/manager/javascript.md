<head>
  <title>Javascript for Manager</title>
</head>

# Example

  Example to illustrate how to create a task and run it.
  
  The following creates a simple task factory
   
<include file="src/test/resources/js/directtask.js" id="task"/>
 
  A task can be run
  
<include file="src/test/resources/js/directtask.js" id="run"/>

  And the output will be
  
    The task returned
    <outputs>
       <value xmlns="http://experimaestro.sf.net" value="10"/>
    </outputs>

# Task factories

  In this section, we give various examples of the different options available while writing a task factory.
  
## Default values

  Default values can be used to set a task parameter to a default value.
  There are two different ways to define defaults values:
  
  1. Using the `default` attribute in the input definition
  1. Using the `default` element

<include file="src/test/resources/js/default.js" id="main"/>

## Alternatives

  Alternatives can be used in order to use different tasks, that can be chosen through setting
  a parameter to the qualified namespace. In the following example, the parameter of type


<include file="src/test/resources/js/alternatives.js" id="main"/>

## Subtasks

<include file="src/test/resources/js/subtasks.js" id="main"/>

## Composing tasks
 
<include file="src/test/resources/js/composing.js" id="main"/>

<include file="src/test/resources/js/composing_2.js" id="main"/>


# Experimental plans

  We use as an example a task that multiply its two inputs `x` and `y`:

<include file="src/test/resources/js/plan.js" id="main"/>

  The following code runs an experimental plan where the values of `x` can be either 1 and 2, and
  the values of `y` are 5 and 7.

<include file="src/test/resources/js/plan.js" id="check"/>

  The values in are `results` the values 5, 10, 7 and 14.

# Environment variables

  * `XPM_DEFAULT_GROUP` defines the default group for the tasks

# Predefined objects
 
   When running a script, the following variables are set:
   
   *  `xp` is a Namespace object that represents the experimaestro namespace  
   *  `xpm` is an object containing useful functions and properties

# Predefined functions

   * `qname(uri, name)` returns a qualified name
   * `script_file()` returns a FileObject corresponding to the current script
   * `include_repository([connector,] path)` includes a repository. The difference with the `include(path)` function is that the repository can be cached. An optional `connector` can be used - by default, the current script connector is used.
   

# XPM object

## Functions

  * `add_task_factory(task)` Adds a task factory to the manager
  * `get_task(qualified_name)` or `get_task(namespace, id)` Get a new instance of task given the namespace and the id
  * `log(format, object, [object ...])` Returns to the caller a message using the format static method from String.
  * `include(path)` includes another javascript file (path is relative to the current file)
  * `xpath(query, xml)` runs an XPath query on an XML document or fragment. See {{XPath handling}} for a description of how XPath are handled within experimaestro.
  * `value(object)` returns an XML experimaestro value
  * `path(file_object)` returns an XML description of the path 
  * `set_default_group(name)` sets the default group for jobs
  * `command_line_job(jobId, commandLine, options)` where `jobId` is a valid task identifier, `commandLine` is
   the command line to be executed, and `options` are the options.
   `commandLine` can be either
      1. An array (command line followed by arguments)
      2. An associative array with the entries "command" (required), "environment" (associative array describing the environment)
  `options` is an associative array:
  - *stdin*: A string or a file object
  - *stdout*: A file object
  - *lock* is an array of couples (resource-id, lock-type) where the lock type can be `READ_ACCESS`, `WRITE_ACCESS`, or `EXCLUSIVE_ACCESS`.

# Logging

A logger object is returned by a call to `xpm.logger(dotname)` where name is a dot separated string. The logger output level is `INFO` by default, and can be changed by a call to `xpm.log_level(dotname, level)`.

The method `log(format, object, [object ...])` Returns to the caller a message using the Java format method where `log` is log level for output.

Log levels can be `trace`, `debug`, `info`, `warn`, `error` and `fatal`.


# XPath handling

  XPath are evaluated with the current node corresponding to the <<root element>> of the XML document.
	
## XPath Functions

  * `parentPath(String)` returns the parent path. For example,
    `xpm.xpath("xp:parentPath(path)", <a><path>/a/b/c</path></a>)` returns `a/b`.
