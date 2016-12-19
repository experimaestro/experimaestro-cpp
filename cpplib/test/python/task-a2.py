from test import A2, B
from experimaestro import register, MergeClass, progress
import time

@MergeClass(A2)
class _A2:
    def execute(self):
        print("Hello world %s" % (self.seed))
        for i in range(20):
            time.sleep(1)
            progress((i+1)/20)

@MergeClass(B)
class _B:
    def execute(self):
        print("Hello world %s [%s]" % (self.a.any.zoé, type(self.a.any.zoé)))
        for i in range(20):
            time.sleep(1)
            progress((i+1)/20)

register.parse()
