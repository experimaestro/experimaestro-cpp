from .experimaestro import *

# import copy
import json
import sys
import inspect
import os.path as op
import logging
import pathlib
from pathlib import Path as PPath

logger = logging.getLogger("xpm")

class JSONEncoder(json.JSONEncoder):
    """A JSON encoder for Python objects"""
    def default(self, o):
        if type(o) == TypeName:
            return str(o)

        if isinstance(o, pathlib.PosixPath):
            return {"$type": "path", "$value": str(o.resolve())}

        return json.JSONEncoder.default(self, o)


# Json encoder
JSON_ENCODER = JSONEncoder()

# Flag for simulating
SUBMIT_TASKS = True



class TypeProxy:
    pass

class choices(TypeProxy):
    """Choices"""
    def __init__(self, *choices):
        pass

    @property
    def type(self):
        return cvar.StringType



def value2array(array):
    """Converts an XPM array to a Python array"""
    array = Array.cast(array)
    r = []
    for i in range(len(array)):
        sv = array[i]
        v = VALUECONVERTERS.get(sv.type().toString(), lambda v: v)(sv)
        r.append(v)
    return r


# Converts a value to Python
VALUECONVERTERS = {
    BooleanType.toString(): lambda v: v.value().asBoolean(),
    IntegerType.toString(): lambda v: v.value().asInteger(),
    StringType.toString(): lambda v: v.value().asString(),
    RealType.toString(): lambda v: v.value().asReal(),
    PathType.toString(): lambda v: v.value().asPath(),
    ArrayType.toString(): value2array
}


def structuredValue(value):
    """Transforms a Python value into a structured value"""

    # Simple case: it is already a configuration
    if isinstance(value, StructuredValue):
        return value

    # It is a PyObject: get the associated configuration
    if isinstance(value, PyObject):
        return value.__xpm__.configuration

    # A dictionary: transform
    if isinstance(value, dict):
        return register.build(JSON_ENCODER.encode(value))

    # A list
    if isinstance(value, list):
        newvalue = Array()
        for v in value:
            newvalue.append(structuredValue(v))

        return newvalue

    # For anything else, we try to convert it to a value
    return StructuredValue(Value(value))

def checknullsv(sv):
    """Returns either None or the sv"""
    if sv.value().scalarType() == ValueType_NONE:
        return None
    return sv

class XPMObject(Object):
    """Holds XPM information for a PyObject"""
    def __init__(self, pyobject):
        super().__init__()
        self.pyobject = pyobject
        self.configuration = StructuredValue()
        self.configuration.object(self)
        self.configuration.type(self.pyobject.__class__.__xpmtype__)

    def set(self, k, v):
        logging.info("Called set: %s, %s", k, v)
        self.configuration.set(k, structuredValue(v))
    
    def setValue(self, key, sv):
        """Called by XPM when value has been validated"""
        if sv is None:
            value = None
            svtype = None
        else:
            svtype = sv.type()
            object = sv.object()
            if object:
                value = object.pyobject
            else:
                value = VALUECONVERTERS.get(svtype.toString(), checknullsv)(sv)
        logger.debug("Really setting %s to %s [%s => %s] on %s", key, value,
                     svtype, type(value), type(self.pyobject))
        setattr(self.pyobject, key, value)
    
    def run(self):
        self.pyobject.execute()

class PyObject:
    """Base type for all objects in python interface"""

    def __init__(self, **kwargs):
        assert self.__class__.__xpmtype__, "No XPM type associated with this XPM object"

        # Add configuration
        self.__xpm__ = XPMObject(self)

        # Initialize with arguments
        for k, v in kwargs.items():
            self.__xpm__.set(k, v)

    def _submit(self, *, launcher=None, launcherParameters=None, send=None):
        """Submit this task"""
        logging.info("Submitting")
        if send is None:
            send = SUBMIT_TASKS
        self.__class__.__xpmtask__.submit(self.__xpm__.configuration, 
            send, launcher, launcherParameters)
        return self

    def _prepare(self):
        """Prepare object after creation"""
        pass


PyObject.__xpmtype__ = AnyType



class PythonRegister(Register):
    def __init__(self):
        # Initialize the base class
        Register.__init__(self)

        self.builtins = {
            int: cvar.IntegerType,
            bool: cvar.BooleanType,
            str: cvar.StringType,
            float: cvar.RealType,
            Path: PathType
        }

        self.registered = {}


    def associateType(self, pythonType, xpmType):
        pythonType.__xpmtype__ = xpmType
        self.registered[xpmType.typeName()] = pythonType

    def addType(self, pythonType, typeName, parentType, description=None):
        xpmType = Type(typeName, parentType)
        if description is not None:
            xpmType.description(description)

        self.associateType(pythonType, xpmType)
        super().addType(xpmType)

    def getType(self, key):
        """Returns the Type object corresponding to the given type or None if not found
        """
        if key is None:
            return AnyType

        if key in self.builtins:
            return self.builtins[key]

        if isinstance(key, type):
            return getattr(key, "__xpmtype__", None)
        if isinstance(key, PyObject):
            return key.__class__.__xpmtype__
        return None

    def parse(self, arguments=None):
        if arguments is None:
            arguments = sys.argv[1:]
        return super().parse(StringList(arguments))

    def runTask(self, task, sv):
        logger.info("Running %s", task)
        sv.object().run()

    def createObject(self, sv):
        type = self.registered.get(sv.type().typeName(), None)
        logger.info("Creating object for %s [%s]", sv, type)
        pyobject = type()
        pyobject._prepare()
        return pyobject.__xpm__

register = PythonRegister()


class RegisterType:
    """Annotations for experimaestro types"""
    def __init__(self, qname, description=None, associate=False):
        if type(qname) == TypeName:
            self.qname = qname
        else:
            self.qname = TypeName(qname)
        self.description = description
        self.associate = associate

    def __call__(self, t):
        # Check if conditions are fullfilled
        xpmType = None
        if self.qname:
            xpmType = Register.getType(register, self.qname)
            if xpmType is not None and not self.associate:
                raise Exception("XPM type %s is already declared" % self.qname)
            if self.associate and xpmType is None:
                raise Exception("XPM type %s is not already declared" % self.qname)

        # Add XPM object if needed
        if not issubclass(t, PyObject):
            __bases__ = (PyObject, ) + t.__bases__
            t = type(t.__name__, __bases__, dict(t.__dict__))

        # Find first registered ancestor
        parentinfo = None
        for subtype in t.__mro__[1:]:
            if issubclass(subtype, PyObject) and subtype != PyObject:
                parentinfo = register.getType(subtype)
                if parentinfo is not None:
                    logger.debug("Found super info %s for %s", parentinfo, t)
                    break

        # Register
        if self.associate:
            register.associateType(t, xpmType)
        else:
            register.addType(t, self.qname, parentinfo)

        return t


class AssociateType(RegisterType):
    def __init__(self, qname, description=None):
        super().__init__(qname, description=description, associate=True)


class RegisterTask():
    """Register a task"""

    def __init__(self, scriptpath=None, pythonpath=None):
        self.pythonpath = sys.executable if pythonpath is None else pythonpath
        self.scriptpath = scriptpath

    def __call__(self, t):
        if not issubclass(t, PyObject):
            raise Exception("Only experimaestro objects (annotated with RegisterType or AssociateType) can be tasks")

        if self.scriptpath is None:
            self.scriptpath = inspect.getfile(t)
        else:
            self.scriptpath = op.join(
                op.dirname(inspect.getfile(t)), self.scriptpath)

        self.scriptpath = PPath(self.scriptpath).absolute()

        logger.debug("Task %s command: %s %s", t, self.pythonpath,
                     self.scriptpath)
        for mro in t.__mro__:
            pyType = register.getType(mro)
            if pyType is not None:
                break
        if pyType is None:
            raise Exception(
                "Class %s has no associated experimaestro type" % t)
        task = Task(pyType)
        t.__xpmtask__ = task
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

        return t


class AbstractArgument:
    """Abstract class for all arguments (standard, path, etc.)"""

    def __init__(self, name, _type, help=""):
        self.argument = Argument(name)
        self.argument.help = help if help is not None else ""
        self.argument.type(_type) # self.argument.type(_type.xpmtype if type(_type) == TypeWrapper else _type)

    def __call__(self, t):
        xpminfo = register.getType(t)
        if xpminfo is None:
            raise Exception("%s is not an XPM type" % t)

        xpminfo.addArgument(self.argument)
        return t


class TypeArgument(AbstractArgument):
    def __init__(self, name, type=None, default=None, required=None,
                 help=None, ignore=False):
        xpmtype = register.getType(type)
        logger.debug("Registering type argument %s [%s -> %s]", name, type,
                      xpmtype)
        AbstractArgument.__init__(self, name, xpmtype, help=help)
        if default is not None and required is not None and required:
            raise Exception("Argument is required but default value is given")

        self.argument.ignore = ignore
        self.argument.required = (default is
                                  None) if required is None else required
        if default is not None:
            self.argument.defaultValue(StructuredValue(Value(default)))


class PathArgument(AbstractArgument):
    def __init__(self, name, path, help=""):
        """
        :param name: The name of argument (in python)
        :param path: The relative path
        """
        AbstractArgument.__init__(self, name, PathType, help=help)
        generator = PathGenerator(path)
        self.argument.generator(generator)


def tojson(t=None):
    if t is None:
        types = []
        for k, t in register.types.items():
            types.append(json.loads(t.toJson()))
        return types
    return register.types[t].toJson()


class Definitions:
    """Allow easy access to XPM tasks"""

    def __init__(self, retriever, path=None):
        self.__retriever = retriever
        self.__path = path

    def __getattr__(self, name):
        if name.startswith("__"):
            return object.__getattr__(self, name)
        if self.__path is None:
            return Definitions(self.__retriever, name)
        return Definitions(self.__retriever, "%s.%s" % (self.__path, name))

    def __call__(self, *args, **options):
        definition = self.__retriever(self.__path)
        if definition is None:
            raise AttributeError("Task/Type %s not found" % self.__path)
        return definition.__call__(*args, **options)


types = Definitions(register.getType)
tasks = Definitions(register.getTask)


def typename(object):
    """Returns the type name of the object"""
    return object.type().typeName()


# # --- Export some useful functions

from experimaestro.rpc import Functions

for name in ["set_experiment"]:
    locals()[name] = getattr(Functions, name)
