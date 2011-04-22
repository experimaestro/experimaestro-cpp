// Declares the alternative
var altName = xpm.qName("a.b.c", "alt");
var abc = new Namespace("a.b.c");

xpm.declareAlternative(altName);



/** Direct task */

var task = {
	id: xpm.qName("a.b.c", "task"),
	inputs: <inputs><input type="xs:integer" id="p"/></inputs>,
	outputs: <outputs><output type="xs:integer"/></outputs>,
	
	run: function(inputs) {
		return <outputs>{inputs.p}</outputs>;
	}
		
};

xpm.addTaskFactory(task);

/** Run and check */

var task = xpm.getTask(task.id);
task.setParameter("p", "10");
var r = task.run();

v = r.xp::value.@value;
if (v == undefined || v != 10)
	throw new java.lang.String.format("Value [%s] is different from 10", r.abc::alt.xp::value.@value);
	
	
