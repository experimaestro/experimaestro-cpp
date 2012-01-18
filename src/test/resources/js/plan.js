
/** Direct task */


// START SNIPPET: main

// Used to store results
var results = [];

// The task
var task = {
    // The id of the task is an XML qualified name 
    id: xpm.qName("a.b.c", "task"),
    
    // One input of type xp:integer
    inputs: <inputs><input type="xp:integer" id="x"/><input type="xp:integer" id="y"/></inputs>,
    
    // One output of type xp:integer
    outputs: <outputs><output type="xp:integer"/></outputs>,
	
    // The function that will be called when the task is run
	run: function(inputs) {
	    // Multiply x by y and put it
        results.push(inputs.x.@xp::value * inputs.y.@xp::value);
        // Returns something
		return <outputs>{inputs.x}{inputs.y}</outputs>;
	}
		
};

// Add the task to the list of available factories
xpm.addTaskFactory(task);

// END SNIPPET: main

/** Run and check */

function test_plan_1() {
    // START SNIPPET: check
    results =  [];
    xpm.experiment(qname("a.b.c", "task"), "x=1,2 * y=5,7");
    xpm.log("The task returned\n%s", results.toSource());
    // END SNIPPET: check
    check([5, 10, 7, 14]);
}


function check(expected) {
    for(var i = 0; i < expected.length; i++)
        if (expected[i] != results[i])
            throw new java.lang.String.format("Expected %s and got %s at %d", expected[i], results[i], i);
}

// Test the plan 1
test_plan_1();