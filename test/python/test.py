from experimaestro import *
import logging
import json

logging.basicConfig(level=logging.INFO)

testns = TypeName("testns")
setLogLevel("local", LogLevel_DEBUG)
setLogLevel("xpm", LogLevel_DEBUG)


@RegisterType(testns("A"))
class A():
    pass

@TypeArgument("model", type=A, required=True, help="object A")
@RegisterType(testns("A1"))
class A1():
    pass

@PathArgument("path", "model.dat", help="The filename where the model is serialized")
@TypeArgument("$seed", default=1, required=False, type=int, help="Random seed")
@TypeArgument("size", type=int, required=True, help="A size")
@TypeArgument("any", type=AnyType, required=True, help="Any type")
@RegisterTask()
@RegisterType(testns("A2"))
class A2(A1): 
    def execute(self): pass

@TypeArgument("a", type=A1)
@RegisterTask()
@RegisterType(testns("B"))
class B(object):
    pass

if __name__ == '__main__':
    import os.path as osp
    import os
    import argparse

    if sys.argv[1] == "run":
        register.parse()
    else:
        parser = argparse.ArgumentParser()
        parser.add_argument("workdir", type=str, help="Working directory")
        args = parser.parse_args()

        pythonpath = ":".join([osp.realpath(x) for x in os.getenv("PYTHONPATH").split(":")])
        print("PYTHON PATH=%s" % pythonpath)

        launcher = DirectLauncher(LocalConnector())
        launcher.environment()["PYTHONPATH"] = pythonpath
        set_launcher(launcher)

        # Default values

        workspace(args.workdir)

        model = A()

        # Create and then run
        for size in  [5, 10]:
            a2 = A2(size=size, any={"zoé": 1})
            a2.model = model
            a2.submit()

        b = B(a=a2)
        submit(b)

        logging.info("Done")
