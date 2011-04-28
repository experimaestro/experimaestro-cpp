// START SNIPPET: main

// Declares the alternative
var altName = qname("a.b.c", "alt");
var abc = new Namespace("a.b.c");

xpm.declareAlternative(altName);

/** Alternative 1 */

var configuration_alt_1 = {
	id: xpm.qName("a.b.c", "alt-1"),
	documentation: <p>Configuration of a possible alternative</p>,
	alternative: altName,
	
	inputs: <inputs>
		<input id="size" type="xs:integer" help="The parameter"/>
	</inputs>,
	
	outputs: <outputs>
		<output type="{a.b.c}alt"/>
	</outputs>,

	run: function(inputs) {
		return <alt xmlns="a.b.c">
				{inputs.size}
			</alt>;
	}
};

xpm.addTaskFactory(configuration_alt_1);

/** Task */

var task_factory = {
	id: xpm.qName("a.b.c", "task"),
	version: "1.0",
	inputs: <><input type="{a.b.c}alt" id="p"/></>,
	outputs: <></>,
	
	run: function(inputs) {
		return <outputs>{inputs.p}</outputs>;
	}
};

xpm.addTaskFactory(task_factory);

/** Run and output */

var task = xpm.getTask(task_factory.id);
task.setParameter("p", "{a.b.c}alt-1");
task.setParameter("p.size", "10");
var r = task.run();
xpm.log("Value of p.size is %s", r.abc::alt.xp::value.@value);

// END SNIPPET: main

function test_value() {
    v = r.abc::alt.xp::value.@value;
    if (v == undefined || v != 10)
    	throw new java.lang.String.format("Value [%s] is different from 10", r.abc::alt.xp::value.@value);
}
	
	