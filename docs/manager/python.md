Python types and tasks can be defined using annotations

# Defining a type

```
from experimaestro import TypeArgument

@TypeArgument("gamma", type=float, required=False)
@RegisterType("my.model")
class MyModel: pass
```


# Running

```
import experimaestro as xpm

# Put your definitions here: either load a YAML or import the definitions

if __name__ == "__main__":
    xpm.logger.setLevel(logging.DEBUG)
    xpm.setLogLevel("xpm", xpm.LogLevel_DEBUG)
    xpm.setLogLevel("rpc", xpm.LogLevel_INFO)
    xpm.register.parse()
```
