from test import A2 as A2
from experimaestro import register, MergeClass

@MergeClass(A2)
class _A2:
    def execute(self):
        print("Hello world %s" % (self.seed))

register.parse()
