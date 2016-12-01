from experimaestro import *
import logging

logging.basicConfig(level=logging.DEBUG)

# ---- XPM PART

# ---- TEST PART

import json
xpmcpp = TypeName("xpmcpplib")
    
@RegisterType(xpmcpp("representation.model"))
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
    
# a = A()
# a = None

# Outputs JSON definitions
logger.info("Outputs to JSON")
logger.info(json.dumps(tojson()))

# Builds from JSON
# logger.info("Build from JSON")
# params = {"$type": xpmcpp("A2"), "size": 1, "model": { "$type": xpmcpp("A") }, "path": ":shares:big:home:yo"}
# print(register.build(params))


# Create from arguments
logger.info("Build from JSON")
model = A.create()

# Configure and run
a2 = A2.execute(size=5, model=model)
print(a2.json())

# Create and then run
a2 = A2.create(size=5)
a2.model = model
a2.run()
print(a2.json())
