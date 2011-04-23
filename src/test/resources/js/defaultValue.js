
var abc = new Namespace("a.b.c");


var task = {
	id: xpm.qName("a.b.c", "task"),
	inputs: <inputs xmlns={xp.uri}>
                <input type="xs:integer" id="a" default="10"/>
                <input id="b">
                    <default><value value="20"/></default>
                </input>
            </inputs>,
	outputs: <outputs><output id="a" type="xs:integer"/><output id="b" type="xs:integer"/></outputs>,
	
	run: function(inputs) {
		return <outputs>{inputs.a}{inputs.b}</outputs>;
	}
		
};

xpm.addTaskFactory(task);

/** Run and check */

var task = xpm.getTask(task.id);
var r = task.run();

a = r.xp::value[0].@value;
if (a == undefined || a != 10)
	throw new java.lang.String.format("Value [%s] is different from 10", a);

b = r.xp::value[1].@value
if (b == undefined || b != 20)
	throw new java.lang.String.format("Value [%s] is different from 20", b);
	
	
