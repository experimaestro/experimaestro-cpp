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
@JsonPath("path", "model.dat", help="The filename where the model is serialized")
@TypeArgument("$seed", default=1, required=False, type=int, help="Random seed")
@TypeArgument("size", type=int, required=True, help="A size")
@TypeArgument("any", type=AnyType, required=True, help="Any type")
@RegisterTask()
@RegisterType(xpmcpp("A2"))
class A2(A1): 
    X = 1
    
    def yo(self): print("yo")


# # --- Run a specific task
#
# register.parse()
#

# --- Build experiments

# Create from arguments
model = A.create()

# Configure and run
for size in  [5, 10]:
    a2 = A2.execute(size=5, model=model)
    
    print(a2.getValue())

# Create and then run
for size in  [5, 10]:
    a2 = A2.create(size=5)
    a2.model = model
    a2.run()
    
