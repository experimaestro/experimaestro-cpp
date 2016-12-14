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

# Used as a metaclass for C++ classes that can be extended
_SwigPyType = type(Object)
class PyObjectType(_SwigPyType):
    pass

VALUECONVERTERS = {
    ValueType_NONE: lambda v: v,
    ValueType_BOOLEAN: lambda v: v.value().getBoolean(),
    ValueType_INTEGER: lambda v: v.value().getInteger(),
    ValueType_STRING: lambda v: v.value().getString(),
    ValueType_REAL: lambda v: v.value().getReal(),
    ValueType_OBJECT: lambda v: v.value().getObject(),
    ValueType_ARRAY: lambda v: [VALUECONVERTERS[x.value().scalarType()](x) for x in v.value().getArray()],
}

class PyObject(Object, metaclass=PyObjectType):
    """Base type for all objects"""

    def __init__(self):
        Object.__init__(self)

    def __setattr__(self, name, value):
        logger.debug("Setting %s to %s", name, value)
        super().set(name, value)

    def setValue(self, key, sv):
        """Called by XPM when value has been validated"""
        if key.startswith("$"):
            key = key[1:]        
        value = VALUECONVERTERS[sv.value().scalarType()](sv)
        dict.__setattr__(self, key, value)

__StructuredValue = StructuredValue
class StructuredValue(__StructuredValue):
    def __init__(self, *args, **argv):
        __StructuredValue.__init__(self, *args, **argv)

# FIXME: Hack to deal with smart pointers objects released by SWIG
FACTORIES = []
OBJECTS = []

class PythonObjectFactory(ObjectFactory):
    """An experimaestro type in Python"""
    def __init__(self, pythonType):
      ObjectFactory.__init__(self)
      self.pythonType = pythonType

    def create(self):
      newObject = self.pythonType()
      OBJECTS.append(newObject)      
      return newObject

class PythonRegister(Register):
    def __init__(self):
        # Initialize the base class
        Register.__init__(self)

        """Mapping for some built-in types"""
        self.builtins = {
            int: cvar.IntegerType,
            bool: cvar.BooleanType,
            str: cvar.StringType,
            float: cvar.RealType
        }

        self.types = {}

    def addType(self, pythonType, typeName, parentType):
        pyType = self.types[pythonType] = Type(typeName, parentType)
        factory = PythonObjectFactory(pythonType)
        FACTORIES.append(factory)
        pyType.objectFactory(factory)
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

    def parse(self, arguments=None):
        if arguments is None:
            arguments = sys.argv[1:]
        return super().parse(StringList(arguments))


    def build(self, params):
        _params = StructuredValue.parse(JSON_ENCODER.encode(params))
        return super().build(_params)

register = PythonRegister()

def create(t, args, options, submit=False):
    logger.debug("Creating %s [%s, %s]", t, args, options)
    xpmType = register.getType(t)

    # Create the type and set the arguments
    o = xpmType.create()
    if hasattr(t, "__task__"):
        o.task(t.__task__)
    logger.debug("Created object [%s] of type [%s]" % (o, type(o).__mro__))
    for k, v in options.items():
        logger.debug("Setting attribute [%s] to %s", k, v)
        setattr(o, k, v)

    if hasattr(t, "__task__"):
        o.task(t.__task__)

    if submit:
        logger.debug("Submitting task to experimaestro")
        o.submit()
    return o

def wrap(self, function, **options):
    def _wrapped(*args, **opts):
        return function(self, args, opts, **options)
    return _wrapped


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
            raise Exception("XPM type %s is already declared" % self.qname)

        # Add XPM object if needed
        bases = t.__bases__ if issubclass(t, PyObject) else (PyObject,) + t.__bases__

        # Re-create a new type
        t = type(t.__name__, bases, dict(t.__dict__))

        # Add the create method
        t.create = wrap(t, create)

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
            self.scriptpath = inspect.getfile(t)
        else:
            self.scriptpath = op.join(op.dirname(inspect.getfile(t)), self.scriptpath)

        self.scriptpath = op.abspath(self.scriptpath)

        logger.debug("Task %s command: %s %s", t, self.pythonpath, self.scriptpath)
        pyType = register.getType(t)
        task = Task(pyType)
        t.__task__ = task
        task.objectFactory(pyType.objectFactory())
        register.addTask(task)

        command = Command()
        command.add(CommandPath(op.realpath(self.pythonpath)))
        command.add(CommandPath(op.realpath(self.scriptpath)))
        command.add(CommandString("run"))
        command.add(CommandString(task.typeName().toString()))
        command.add(CommandParameters())
        commandLine = CommandLine()
        commandLine.add(command)
        task.commandline(commandLine)

        t.autoSubmit = wrap(t, create, submit=True)
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

class PathArgument(AbstractArgument):
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


class MergeClass:
    """Merge class annotation

    class A:
        def x(self): print("x")

    a = A()

    @MergeClass(A)
    class A:
        def y(self): print("y")

    a.x() # prints x
    a.y() # prints y

    """
    def __init__(self, original):
        self.original = original


    def __call__(self, _class):
        for _, method in inspect.getmembers(_class, predicate=inspect.isfunction):
            setattr(self.original, method.__name__, method)

        return None