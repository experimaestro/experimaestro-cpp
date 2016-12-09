import test.A2 as A2

from xpm import parse

@MergeClass(A2)
class _A2:
    def execute():
        print("Hello world")

a2 = register.parse()
a2.execute()