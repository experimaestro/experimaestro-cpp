/**
 * Test for groups in inputs
 */

tasks.add("groups", {
   inputs: {
       a: { value: "xp:integer", groups: ["1", "2"] },
       b: { value: "xp:integer", groups: ["1"] }
   },

   run: function(p) {
       x = this.group("2", p);
       if (x.a === undefined) {
           throw "Test error: a should be defined";
       }
       if (x.b !== undefined) {
           throw "Test error: b should not be defined";
       }
       return {};
   }
});

function test_groups() {
    tasks("groups").run({
        a: 1,
        b: 2
    });
}