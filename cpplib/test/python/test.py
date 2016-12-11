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
    

if __name__ == '__main__':
    import os.path as osp
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("workdir", type=str, help="Working directory")
    args = parser.parse_args()
    
    # Create from arguments
    set_workdir(osp.realpath(args.workdir))
    
    model = A.create()

    # Create and then run
    for size in  [5, 10]:
        a2 = A2.create(size=size, any=1)
        a2.model = model
        a2.run()
    


