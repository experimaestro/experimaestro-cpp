# Import Python modules

import json
import sys
import inspect
import os.path as op
import os
import logging
from pathlib import Path as BasePath, PosixPath

# --- Initialization

logger = logging.getLogger("xpm")
modulepath = BasePath(__file__).parent

# --- Import C bindings

from cffi import FFI
import re

ffi = FFI()
with open(modulepath / "api.h", "r") as fp:
    RE_SKIP = re.compile(r"""^\s*(?:#include|#ifn?def|#endif|#define|extern "C") .*""")
    cdef = ""
    for line in fp:
        if not RE_SKIP.match(line):
            cdef += line

    ffi.cdef(cdef)

print(modulepath / "libexperimaestro.so")
lib = ffi.dlopen(str(modulepath / "libexperimaestro.so"))


# --- Utilities and constants

class JSONEncoder(json.JSONEncoder):
    """A JSON encoder for Python objects"""
    def default(self, o):
        if type(o) == Typename:
            return str(o)

        if isinstance(o, BasePath):
            return {"$type": "path", "$value": str(o.resolve())}

        return json.JSONEncoder.default(self, o)


# Json encoder
JSON_ENCODER = JSONEncoder()

# Flag for simulating
SUBMIT_TASKS = True

# Default launcher
DEFAULT_LAUNCHER = None

# --- From C++ types to Python

def value2python(sv):
    """Converts a value into a Python object"""
    svtype = sv.type()
    object = sv.asMap().object() if sv.isMap() else None
    
    if object:
        return object.pyobject

    if svtype.array():
        sv  = sv.asArray()
        r = []
        for i in range(len(sv)):
            v = value2python(sv[i])
            r.append(v)
        return r

    return VALUECONVERTERS.get(svtype.toString(), checknullsv)(sv)


"""XPM type to Python"""
TYPE2PYTHON = {
    lib.type_tostring(lib.BOOLEAN_TYPE): bool,
    lib.type_tostring(lib.INTEGER_TYPE): int,
    lib.type_tostring(lib.STRING_TYPE): str,
    lib.type_tostring(lib.REAL_TYPE): float,
    lib.type_tostring(lib.PATH_TYPE): BasePath
}


"""Dictionary of converteres"""
VALUECONVERTERS = {
    lib.type_tostring(lib.BOOLEAN_TYPE): lambda v: v.asScalar().asBoolean(),
    lib.type_tostring(lib.INTEGER_TYPE): lambda v: v.asScalar().asInteger(),
    lib.type_tostring(lib.STRING_TYPE): lambda v: v.asScalar().asString(),
    lib.type_tostring(lib.REAL_TYPE): lambda v: v.asScalar().asReal(),
    lib.type_tostring(lib.PATH_TYPE): lambda v: v.asScalar().asPath()
}

# --- From Python to C++ types

def python2value(value):
    """Transforms a Python value into a structured value"""
    # Simple case: it is already a configuration
    if isinstance(value, Value):
        return value

    # It is a PyObject: get the associated configuration
    if isinstance(value, PyObject):
        return value.__xpm__.sv

    # A dictionary: transform
    if isinstance(value, dict):
        v = register.build(JSON_ENCODER.encode(value))
        return v

    # A list
    if isinstance(value, list):
        newvalue = ArrayValue()
        for v in value:
            newvalue.append(python2value(v))

        return newvalue

    # A path
    if isinstance(value, pathlib.Path):
        return ScalarValue(Path(str(value.absolute())))

    # For anything else, we try to convert it to a value
    return ScalarValue(Scalar(value))

def checknullsv(sv):
    """Returns either None or the sv"""
    return None if sv.isScalar() and sv.asScalar().null() else sv



# --- XPM Objects

class XPMObject:
    """Holds XPM information for a PyObject"""
    def __init__(self, pyobject, sv=None):
        self.ptr = ffi.gc(lib.object_new(self.setValue), lib.workspace_free)
        self.pyobject = pyobject

        if sv is None:
            self.sv = MapValue()
        else:
            self.sv = sv.asMap()

        self.sv.object(self)
        self.sv.type(self.pyobject.__class__.__xpmtype__)
        self.setting = False
        self.submitted = False
        self.dependencies = []

    @property
    def job(self):
        job = self.sv.job()
        if job: return job
        raise Exception("No job associated with python2value %s" % self.sv)
        
    def set(self, k, v):
        if self.setting: return

        logger.debug("Called set: %s, %s (%s)", k, v, type(v))
        try:
            self.setting = True
            # Check if the value corresponds to a task; if so,
            # raise an exception if the task was not submitted
            if isinstance(v, PyObject) and hasattr(v.__class__, "__xpmtask__"):
                if not v.__xpm__.submitted:
                    raise Exception("Task for argument '%s' was not submitted" % k)
            pv = python2value(v)
            self.sv.set(k, pv)
        except:
            logger.error("Error while setting %s", k)
            raise
        finally:
            self.setting = False

    @ffi.callback
    def setValue(self, key, sv):
        """Called by XPM when value has been validated"""
        if self.setting: return
        try:
            self.setting = True
            if sv is None:
                value = None
                svtype = None
            else:
                value = value2python(sv)
                svtype = sv.type()

            # Set the value on the object if not setting otherwise
            logger.debug("Really setting %s to %s [%s => %s] on %s", key, value,
                    svtype, type(value), type(self.pyobject))
            setattr(self.pyobject, key, value)
        finally:
            self.setting = False
    
    def run(self):
        self.pyobject.execute()

    def init(self):
        self.pyobject._init()

class PyObject:
    """Base type for all objects in python interface"""

    def __init__(self, **kwargs):
        assert self.__class__.__xpmtype__, "No XPM type associated with this XPM object"

        # Add configuration
        self.__xpm__ = XPMObject(self)

        # Initialize with arguments
        for k, v in kwargs.items():
            self.__xpm__.set(k, v)

    def submit(self, *, workspace=None, launcher=None, send=SUBMIT_TASKS):
        """Submit this task"""
        if self.__xpm__.submitted:
            raise Exception("Task %s was already submitted" % self)
        if send:
            launcher = launcher or DEFAULT_LAUNCHER
            self.__class__.__xpmtask__.submit(workspace, launcher, self.__xpm__.sv, self.__xpm__.dependencies)

        self.__xpm__.submitted = True
        return self

    def __setattr__(self, name, value):
        if not Task.isRunning:
            # If task is not running, we update the structured
            # value
            if name != "__xpm__":
                self.__xpm__.set(name, value)
        super().__setattr__(name, value)

    def _init(self):
        """Prepare object after creation"""
        pass

    
    def _stdout(self):
        return self.__xpm__.job.stdoutPath().localpath()
    def _stderr(self):
        return self.__xpm__.job.stdoutPath().localpath()

    def _adddependency(self, dependency):
        self.__xpm__.dependencies.append(dependency)


    @classmethod
    def clickoption(cls, option_name):
        """Helper class method: adds a click option corresponding to the named argument"""
        import click
        xpmtype = cls.__xpmtype__
        a = cls.__xpmtype__.argument(option_name)
        if a is None:
            raise Exception("No argument with name %s in %s" % (option_name, xpmtype))

        name = "--%s" % a.name().replace("_", "-")
        ptype = TYPE2PYTHON[str(a.type())]
        default = value2python(a.defaultValue()) if a.defaultValue() else None
        return click.option(name, help=a.help, type=ptype)


# Another way to submit if the method is overriden
def submit(*args, **kwargs):
    PyObject.submit(*args, **kwargs)

# Defines a class property
PyObject.__xpmtype__ = lib.ANY_TYPE

class TypeProxy: pass

class ArrayOf(TypeProxy):
    """Array of object"""
    def __init__(self, cls):
        self.cls = cls

    def __call__(self, register):
        type = register.getType(self.cls)
        return ArrayType(type)

class Choice(TypeProxy):
    def __init__(self, *args):
        self.choices = args

    def __call__(self, register):
        return cvar.StringType


class PythonRegister:
    """The register contains a reference"""
    def __init__(self):
        # Initialize the base class
        self.ptr = ffi.gc(lib.register_new(), lib.register_free)

        self.builtins = {
            int: lib.INTEGER_TYPE,
            bool: lib.BOOLEAN_TYPE,
            str: lib.STRING_TYPE,
            float: lib.REAL_TYPE,
            BasePath: lib.PATH_TYPE
        }

        self.registered = {}


    def associateType(self, pythonType, xpmType):
        pythonType.__xpmtype__ = xpmType
        self.registered[xpmType.name()] = pythonType

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

        if isinstance(key, TypeProxy):
            return key(self)
        if isinstance(key, type):
            return getattr(key, "__xpmtype__", None)
        if isinstance(key, PyObject):
            return key.__class__.__xpmtype__
        return None

    def getTask(self, name):
        lib.register_getTask()


    def runTask(self, task, sv):
        logger.info("Running %s", task)
        sv.asMap().object().run()

    def createObject(self, sv):
        type = self.registered.get(sv.type().name(), PyObject)
        logger.debug("Creating object for %s [%s]", sv, type)
        pyobject = type.__new__(type)
        pyobject.__xpm__ = XPMObject(pyobject, sv=sv)
        logger.debug("Preparing object for %s", type)
        return pyobject.__xpm__

    def parse(self, arguments=None, try_parse=False):
        if arguments is None:
            arguments = sys.argv[1:]
        return super().parse(StringList(arguments), try_parse)

    def try_parse(self, arguments=None):
        return self.parse(arguments, True)

register = PythonRegister()


# --- Annotations to define tasks and types

class RegisterType:
    """Annotations for experimaestro types"""
    def __init__(self, qname, description=None, associate=False):
        if type(qname) == Typename:
            self.qname = qname
        else:
            self.qname = Typename(qname)
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
    """Annotation to associate one class with an XPM type"""
    def __init__(self, qname, description=None):
        super().__init__(qname, description=description, associate=True)



class RegisterTask(RegisterType):
    """Register a task"""

    def __init__(self, qname, scriptpath=None, pythonpath=None, prefix_args=[], description=None, associate=None):
        super().__init__(qname, description=description, associate=associate)
        self.pythonpath = sys.executable if pythonpath is None else pythonpath
        self.scriptpath = scriptpath
        self.prefix_args = prefix_args

    def __call__(self, t):
        # Register the type
        t = super().__call__(t)
        
        if not issubclass(t, PyObject):
            raise Exception("Only experimaestro objects (annotated with RegisterType or AssociateType) can be tasks")

        if self.scriptpath is None:
            self.scriptpath = inspect.getfile(t)
        else:
            self.scriptpath = op.join(
                op.dirname(inspect.getfile(t)), self.scriptpath)

        self.scriptpath = BasePath(self.scriptpath).absolute()

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
        for arg in self.prefix_args:
            command.add(CommandString(arg))
        command.add(CommandString("run"))
        command.add(CommandString("--json-file"))
        command.add(CommandParameters())
        command.add(CommandString(Typename.toString(task.name())))
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
    """Defines an argument for an experimaestro type"""
    def __init__(self, name, type=None, default=None, required=None,
                 help=None, ignored=False):
        xpmtype = register.getType(type)
        logger.debug("Registering type argument %s [%s -> %s]", name, type,
                      xpmtype)
        AbstractArgument.__init__(self, name, xpmtype, help=help)
        if default is not None and required is not None and required:
            raise Exception("Argument is required but default value is given")

        self.argument.ignored = ignored
        self.argument.required = (default is
                                  None) if required is None else required
        if default is not None:
            self.argument.defaultValue(python2value(default))


class PathArgument(AbstractArgument):
    """Defines a an argument that will be a relative path (automatically
    set by experimaestro)"""
    def __init__(self, name, path, help=""):
        """
        :param name: The name of argument (in python)
        :param path: The relative path
        """
        AbstractArgument.__init__(self, name, PathType, help=help)
        generator = PathGenerator(path)
        self.argument.generator(generator)

class ConstantArgument(AbstractArgument):
    """
    An constant argument (useful for versionning tasks)
    """
    def __init__(self, name: str, value, help=""):
        value = python2value(value)
        xpmtype = register.getType(value)
        super().__init__(name, xpmtype, help=help)
        self.argument.constant(value)


# --- Export some useful functions

class _Definitions:
    """Allow easy access to XPM tasks with dot notation to tasks or types"""

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


types = _Definitions(register.getType)
tasks = _Definitions(register.getTask)


EXCEPTIONS = {
    lib.ERROR_RUNTIME: RuntimeError
}

def checkexception():
    code = lib.lasterror_code()
    if code != lib.ERROR_NONE:
        raise EXCEPTIONS.get(code, Exception)(ffi.string(lib.lasterror_message()))
def cstr(s):
    return str(s).encode("utf-8")

class Workspace():
    """An experimental workspace"""
    def __init__(self, path):
        # Initialize the base class
        self.ptr = ffi.gc(lib.workspace_new(str(path).encode("utf-8")), lib.workspace_free)

    def current(self):
        """Set this workspace as being the default workspace for all the tasks"""
        lib.workspace_current(self.ptr)

    def experiment(self, name):
        """Sets the current experiment name"""
        lib.workspace_experiment(self.ptr, cstr(name))

    def server(self, port: int):
        lib.workspace_server(self.ptr, port, cstr(modulepath / "htdocs"))
        checkexception()

Workspace.waitUntilTaskCompleted = lib.workspace_waitUntilTaskCompleted
workspace = None

def experiment(path, name):
    """Defines an experiment
    
    :param path: The working directory for the experiment
    :param name: The name of the experiment
    """
    global workspace
    if isinstance(path, BasePath):
        path = path.absolute()
    workspace = Workspace(str(path))
    workspace.current()
    workspace.experiment(name)
    return workspace

def set_launcher(launcher):
    global DEFAULT_LAUNCHER
    DEFAULT_LAUNCHER = launcher

launcher = DirectLauncher(LocalConnector())
if os.getenv("PYTHONPATH"):
    launcher.environment()["PYTHONPATH"] = os.getenv("PYTHONPATH")

set_launcher(launcher)

def tag(name: str, x, object:PyObject=None, context=None):
    """Tag a value"""
    if object:
        if not hasattr(object, "__xpm__"):
            object = sv = python2value(object)
        else:
            sv = object.__xpm__.sv # type: MapValue
        sv.addTag(name, x)
        if context:
            sv.setTagContext(context)
        return object

    value = ScalarValue(x)
    value.tag(name)
    return value

def tags(value):
    """Return the tags associated with a value"""
    if isinstance(value, Value):
        return value.tags()
    return value.__xpm__.sv.tags()

def tagspath(value: PyObject):
    """Return the tags associated with a value"""
    p = BasePath()
    for key, value in value.__xpm__.sv.tags().items():
        p /= "%s=%s" % (key.replace("/","-"), value)
    return p

# --- Handle signals

import atexit
import signal

EXIT_MODE = False

def handleKill():
    EXIT_MODE = True
    logger.warn("Received SIGINT or SIGTERM")
    sys.exit(0)

signal.signal(signal.SIGINT, handleKill)
signal.signal(signal.SIGTERM, handleKill)
signal.signal(signal.SIGQUIT, handleKill)

@atexit.register
def handleExit():
    logger.info("End of script: waiting for jobs to be completed")
    lib.workspace_waitUntilTaskCompleted()

