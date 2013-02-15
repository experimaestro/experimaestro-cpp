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


# XPath handling

  XPath are evaluated with the current node corresponding to the <<root element>> of the XML document.
	
## XPath Functions

  * `parentPath(String)` returns the parent path. For example,
    `xpm.xpath("xp:parentPath(path)", <a><path>/a/b/c</path></a>)` returns `a/b`.
