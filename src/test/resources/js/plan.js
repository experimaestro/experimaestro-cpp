
/** Direct task */

var results = [];

// START SNIPPET: main
var task = {
    // The id of the task is an XML qualified name 
    id: xpm.qName("a.b.c", "task"),
    
    // One input of type xp:integer
    inputs: <inputs><input type="xp:integer" id="x"/><input type="xp:integer" id="y"/></inputs>,
    
    // One output of type xp:integer
    outputs: <outputs><output type="xp:integer"/></outputs>,
	
    // The function that will be called when the task is run
	run: function(inputs) {
        results.push(inputs.x.@xp::value * inputs.y.@xp::value);
		return <outputs>{inputs.x}{inputs.y}</outputs>;
	}
		
};

// Add the task to the list of available factories
xpm.addTaskFactory(task);

// END SNIPPET: main

/** Run and check */

function test_plan_1() {
    results =  [];
    xpm.experiment(qname("a.b.c", "task"), "x=1,2 * y=5,7");
    xpm.log("The task returned\n%s", results.toSource());
    check();
}


function test_plan_1() {
    results =  [];
    var task = xpm.getTask(qname("a.b.c", "task"));
    task.run_plan("x=1,2 * y=5,7");
    xpm.log("The task returned\n%s", results.toSource());
    check();
}

function check() {    
    var expected = [5, 10, 7, 14];
    for(var i = 0; i < expected.length; i++)
        if (expected[i] != results[i])
            throw new java.lang.String.format("Expected %s and got %s at %d", expected[i], results[i], i);
}