from .experimaestro import *

import copy
import json
import sys
import inspect
import os.path as op
import logging

logger = logging.getLogger("xpm")

def is_new_style(cls):
    """Returns true if the class is new style"""
    return hasattr(cls, '__class__') \
           and ('__dict__' in dir(cls) or hasattr(cls, '__slots__'))

class JsonEncoder(json.JSONEncoder):
   def default(self, o):
        if type(o) == TypeName:
            return str(o)

        return json.JSONEncoder.default(self, o)

JSON_ENCODER = JsonEncoder()

class PyObject(Object):
    """Base type"""
    def __init__(self):
        Object.__init__(self)
                
    def __setattr__(self, key, value):
        """Called by Python when setting an attribute value"""
        logger.debug("Setting %s to %s" % (key, value))
        super().set(key, value)

    def setValue(self, key, sv):
        """Called by XPM when value has been validated"""
        logger.debug("Really setting %s to %s" % (key, sv))
        dict.__setattr__(self, key, sv.value())

class PythonType(Type):
    def __init__(self, pythonType, *args):
        Type.__init__(self, *args)
        self.pythonType = pythonType

    def create(self):
        o = self.pythonType()
        o.type(self)
        return o

class PythonRegister(Register):
    def __init__(self):
        # Initialize the base class
        Register.__init__(self)

        """Mapping for some built-in types"""
        self.builtins = {
            int: IntegerType,
            bool: BooleanType,
            str: StringType,
            float: RealType
        }

        self.types = {}

    def addType(self, pythonType, typeName, parentType):
        pyType = PythonType(pythonType, typeName, parentType)
        self.types[pythonType] = pyType
        super().addType(pyType)

    def getType(self, key):
        """Returns the Type object corresponding to the given type
        """
        if key is None:
            return AnyType

        if key in self.builtins:
            return self.builtins[key]

        if isinstance(key, Type):
            return key

        if isinstance(key, TypeName):
            return super().getType(key)

        if issubclass(key, PyObject):
            return self.types.get(key, None)

        return super().getType(key)


    def build(self, params):
        _params = StructuredValue.parse(JSON_ENCODER.encode(params))
        return super().build(_params)

register = PythonRegister()

def create(t, args, options):
    logger.debug("Creating %s [%s, %s]", t, args, options)
    xpmType = register.getType(t)
    o = xpmType.create()
    for k, v in options.items():
        setattr(o, k, v)
    return o

class RegisterType:
    """Declares an experimaestro type"""
    def __init__(self, qname):
        if type(qname) == TypeName:
            self.qname = qname
        else:
            self.qname = TypeName(qname)

    def __call__(self, t):
        if not is_new_style(t):
            raise Exception("Error: type %s is an old style Python 2 class" % t)

        # Register type
        if self.qname is not None and register.getType(self.qname) is not None:
            raise Exception("XPM type %s is already declared" % qname)

        # Add the create method
        _dict = dict(t.__dict__)
        def _create(*args, **options):
            return create(t, args, options)
        _dict["create"] = staticmethod(_create)

        # Add XPM object if needed
        bases = t.__bases__ if issubclass(t, PyObject) else (PyObject,) + t.__bases__

        # Re-create a new type
        t = type(t.__name__, bases, _dict) 

        # Find first registered ancestor
        parentinfo = None
        for subtype in t.__mro__[1:]:
            if issubclass(subtype, PyObject) and subtype != PyObject:
                parentinfo = register.getType(subtype)
                if parentinfo is not None:
                    logger.debug("Found super info %s for %s", parentinfo, t)
                    break

        # Register
        register.addType(t, self.qname, parentinfo)

        return t

class RegisterTask():
    """Register a task"""
    def __init__(self, scriptpath=None, pythonpath=None):
        self.pythonpath = sys.executable if pythonpath is None else pythonpath
        self.scriptpath = scriptpath

    def __call__(self, t):
        if not issubclass(t, Object): 
            raise Exception("Only XPM objects can be tasks")
        
        if self.scriptpath is None:
            self.scriptpath = op.abspath(inspect.getfile(t))
            
        logger.debug("Task %s command: %s %s", t, self.pythonpath, self.scriptpath)
        task = Task(register.getType(t))
        register.addTask(task)
        return t

class AbstractArgument:
    def __init__(self, name, type, help=""):
        self.argument = Argument(name)
        self.argument.help = help if help is not None else ""
        self.argument.type = register.getType(type)

    def __call__(self, t):
        xpminfo = register.getType(t)
        if xpminfo is None:
            raise Exception("%s is not an XPM type" % t)

        xpminfo.addArgument(self.argument)
        return t

class TypeArgument(AbstractArgument):
    def __init__(self, name, default=None, choices=None, required=None, type=None, help=None):
        AbstractArgument.__init__(self, name, register.getType(type), help=help)
        self.argument.required = (default is None) if required is None else required
        if default is not None:
            self.argument.defaultValue = Value(default)

class JsonPath(AbstractArgument):
    def __init__(self, name, path, help=""):
        AbstractArgument.__init__(self, name, PathType, help=help)
        self.argument.generator = pathGenerator

def tojson(t=None):
    if t is None:
        types = []
        for k,t in register.types.items():
            types.append(json.loads(t.toJson()))
        return types
    return register.types[t].toJson()

