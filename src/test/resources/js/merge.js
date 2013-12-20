var y = new PlanInput([{v: 3}, {v: 4}])

var a = tasks.copy("a", {
   x: [1, 2],
   y: y
});

var plan = tasks.merge("xp:any", { a: a.group_by(y) } , { v: y.select("v") } );
var s = plan.run()[0];

assert(_(s.v) == 3, "s.v is not 3 but %d", _(s.v));
assert(s.a.length == 2, "s.a is not an array of length 2 but %d", s.a.length);
