
/** Direct task */

// START SNIPPET: task
var task = {
    // The id of the task is an XML qualified name 
    id: xpm.qName("a.b.c", "task"),
    
    // One input of type xp:integer
    inputs: <inputs><input type="xp:integer" id="x"/></inputs>,
    
    // One output of type xp:integer
    outputs: <outputs><output type="xp:integer"/></outputs>,
	
    // The function that will be called when the task is run
	run: function(inputs) {
		return <outputs>{inputs.x}</outputs>;
	}
		
};

// Add the task to the list of available factories
xpm.addTaskFactory(task);
// END SNIPPET: task


/** Run and check */

// START SNIPPET: run
var task = xpm.getTask("a.b.c", "task");
task.setParameter("x", "10");
var r = task.run();
xpm.log("The task returned\n%s", r);

// END SNIPPET: run

var abc = Namespace("a.b.c");
v = r.xp::value.@value;
if (v == undefined || v != 10)
	throw new java.lang.String.format("Value [%s] is different from 10", r.abc::alt.xp::value.@value);
	
	
