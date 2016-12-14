from experimaestro import *
import logging
import json

logging.basicConfig(level=logging.DEBUG)

xpmcpp = TypeName("xpmcpplib")

@RegisterType(xpmcpp("A"))
class A():
    pass

@TypeArgument("model", type=A, required=True, help="object A")
@RegisterType(xpmcpp("A1"))
class A1():
    pass

# @JsonInputStream("samples", mg4j("position-sample"), help="Samples")
@PathArgument("path", "model.dat", help="The filename where the model is serialized")
@TypeArgument("$seed", default=1, required=False, type=int, help="Random seed")
@TypeArgument("size", type=int, required=True, help="A size")
@TypeArgument("any", type=AnyType, required=True, help="Any type")
@RegisterTask("task-a2.py")
@RegisterType(xpmcpp("A2"))
class A2(A1): pass

@TypeArgument("a", type=A1)
@RegisterTask("task-a2.py")
@RegisterType(xpmcpp("B"))
class B(object):
    pass

if __name__ == '__main__':
    import os.path as osp
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("workdir", type=str, help="Working directory")
    args = parser.parse_args()

    # Default values
    set_workdir(osp.realpath(args.workdir))
    rpc.Functions.set_experiment("cpp.test", True)

    connector = rpc.Functions.get_localhost_connector()
    launcher = connector.default_launcher()
    launcher.set_notification_url("http://localhost:12346/notification") #rpc.Functions.notification_url())
    rpc.Functions.set_default_launcher(launcher)

    model = A.create()

    # Create and then run
    for size in  [5, 10]:
        a2 = A2.create(size=size, any=1)
        print(a2)
        # a2.yoas()
        a2.model = model
        a2.submit()
        print("a2 is", a2.getValue().toJsonString())
        
        b = B.create(a=a2)
        print("b is", b.getValue().toJsonString())
        print(b.a.size)
        b.submits()



