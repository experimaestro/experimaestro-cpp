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
TYPES_DICT = {}

# Flag for simulating
SUBMIT_TASKS = True

# Only way to get through _SwigPyType
_SwigPyType = type(Object)
class PyObjectType(_SwigPyType):
   """
   Meta class for all objects in python interface

   It handles the creation of objects, and the definition of new attributes
   (blocked by SWIG)
   """
   def __init__(self, name, bases, dct):
      _SwigPyType.__init__(self, name, bases, dct)
      TYPES_DICT[self] = {}
   def __del__(self):
      del TYPES_DICT[self]
   def __setattr__(self, name, value):
      TYPES_DICT[self][name] = value
   def __getattribute__(self, name):
      logging.debug("Searching for %s in %s", name, self)
      if name in TYPES_DICT[self]:
         return TYPES_DICT[self][name]
      return _SwigPyType.__getattribute__(self, name)

   def __call__(cls, *args, **kwds):
      if "create" in TYPES_DICT[cls]:
         return TYPES_DICT[cls]["create"](*args, **kwds)
      return _SwigPyType.__call__(cls, *args, **kwds)

VALUECONVERTERS = {
    BooleanType.toString(): lambda v: v.asBoolean(),
    IntegerType.toString(): lambda v: v.asInteger(),
    StringType.toString(): lambda v: v.asString(),
    RealType.toString(): lambda v: v.asReal(),
    PathType.toString(): lambda v: v.asPath()
}

class PyObject(Object, metaclass=PyObjectType):
    """Base type for all objects in python interface"""
    def __init__(self):
        Object.__init__(self)

    def __getattribute__(self, name):
       d = TYPES_DICT.get(type(self), None)
       if d is not None and name in d:
         from types import MethodType
         return MethodType(d[name], self)
       return super().__getattribute__(name)

    def __setattr__(self, name, value):
        logger.debug("Setting %s to %s [%s]", name, value, type(value))
        if Task.isRunning():
          dict.__setattr__(self, name, value)
        else:
          super().set(name, value)

    def submit(self, *, launcher=None, launcherParameters=None, send=None):
      if send is None:
         send = SUBMIT_TASKS
      super().submit(send, launcher, launcherParameters)
      return self

    def setValue(self, key, sv):
        """Called by XPM when value has been validated"""
        if key.startswith("$"):
            key = key[1:]
        value = VALUECONVERTERS.get(sv.type().toString(), lambda v: v)(sv)
        logger.debug("Really setting %s to %s [%s => %s] on %s", key, value, sv.type(), type(value), type(self))
        dict.__setattr__(self, key, value)

# FIXME: Hack to deal with smart pointers objects released by SWIG
FACTORIES = []
OBJECTS = []

class PythonObjectFactory(ObjectFactory):
    """An experimaestro type in Python"""
    def __init__(self, register, pythonType):
      ObjectFactory.__init__(self, register)
      self.pythonType = pythonType

    def _create(self):
      logger.debug("Created new object of type [%s]", self.pythonType.__mro__)
      # Create and call the PyObject init
      newObject = self.pythonType.__new__(self.pythonType)
      PyObject.__init__(newObject)
      OBJECTS.append(newObject)
      return newObject

class TypeWrapper():
    def __init__(self, xpmtype):
        self.xpmtype = xpmtype

    def __call__(self, *args, **options):
        return create(self.xpmtype, args, options)

    @staticmethod
    def wrap(xpmtype):
        if xpmtype is None:
            return None
        return TypeWrapper(xpmtype)



class PythonRegister(Register):
    def __init__(self):
        # Initialize the base class
        Register.__init__(self)
        # FIXME: when smart pointers are handled...
        self.factory = PythonObjectFactory(self, PyObject)
        self.objectFactory(self.factory)

        """Mapping for some built-in types"""
        self.builtins = {
            int: cvar.IntegerType,
            bool: cvar.BooleanType,
            str: cvar.StringType,
            float: cvar.RealType,
            Path: PathType
        }

        self.types = {}

    def associateType(self, pythonType, xpmType):
        self.types[pythonType] = xpmType
        factory = PythonObjectFactory(self, pythonType)
        # HACK: avoid to be released since shared_ptr are not handled by SWIG yet
        FACTORIES.append(factory)
        xpmType.objectFactory(factory)

    def addType(self, pythonType, typeName, parentType, description=None):
        xpmType = Type(typeName, parentType)
        if description is not None:
          xpmType.description(description)

        self.associateType(pythonType, xpmType)
        super().addType(xpmType)



    def getTask(self, name):
      logger.debug("Getting task %s", name)
      task = super().getTask(name)
      if task is None:
        raise KeyError("Task %s does not exist" % name)
      task.__task__ = task
      task.create = wrap(task, create)
      task.submit_ = wrap(task, create, submit=True)
      task.__call__ = wrap(task, create)
      return task

    def getType(self, key):
        """Returns the Type object corresponding to the given type
        """
        if key is None:
            return AnyType

        if isinstance(key, Task):
          return key.type()

        if key in self.builtins:
            return self.builtins[key]

        if isinstance(key, Type) or isinstance(key, TypeWrapper):
            return key

        if isinstance(key, TypeName):
            return TypeWrapper.wrap(super().getType(key))

        if inspect.isclass(key) and issubclass(key, PyObject):
            return self.types.get(key, None)

        return TypeWrapper.wrap(super().getType(key))


    def parse(self, arguments=None):
        if arguments is None:
            arguments = sys.argv[1:]
        return super().parse(StringList(arguments))


    def createObject(self, params):
        return Value(params)
        return super().build(_params)

register = PythonRegister()

def create(t, args, options, submit=False):
    logger.debug("Creating %s [%s, %s]", t, args, options)
    xpmType = register.getType(t)

    # Create the type and set the arguments
    o = xpmType.create(register.objectFactory())
    logger.debug("Created object [%s] of type [%s]" % (o, type(o).__mro__))
    if hasattr(t, "__task__"):
        o.task(t.__task__)

    for k, v in options.items():
      if type(v) == dict:
         v = Register.build(register, json.dumps(v))
      logger.debug("Setting attribute [%s] to %s (type %s)", k, v, type(v))
      if isinstance(o, PyObject):
         setattr(o, k, v)
      else:
         PyObject.set(o, k, v)

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
    def __init__(self, qname, description=None, associate=False):
        if type(qname) == TypeName:
            self.qname = qname
        else:
            self.qname = TypeName(qname)
        self.description = description
        self.associate = associate

    def __call__(self, t):
        if not is_new_style(t):
            raise Exception("Error: type %s is an old style Python 2 class" % t)

        # Check if conditions are fullfilled
        xpmType = Register.getType(register, self.qname) if self.qname is not None else None
        if xpmType is not None and not self.associate:
            raise Exception("XPM type %s is already declared" % self.qname)
        if self.associate and xpmType is None:
            raise Exception("XPM type %s is not already declared" % self.qname)

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
        if not issubclass(t, Object):
            raise Exception("Only XPM objects can be tasks")

        if self.scriptpath is None:
            self.scriptpath = inspect.getfile(t)
        else:
            self.scriptpath = op.join(op.dirname(inspect.getfile(t)), self.scriptpath)

        self.scriptpath = op.abspath(self.scriptpath)

        logger.debug("Task %s command: %s %s", t, self.pythonpath, self.scriptpath)
        for mro in t.__mro__:
            pyType = register.getType(mro)
            if pyType is not None:
                break
        if pyType is None:
            raise Exception("Class %s has no associated experimaestro type" %t)
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

        t.submit_ = wrap(t, create, submit=True)
        return t



class AbstractArgument:
    def __init__(self, name, _type, help=""):
        self.argument = Argument(name)
        self.argument.help = help if help is not None else ""
        self.argument.type(_type.xpmtype if type(_type) == TypeWrapper else _type)

    def __call__(self, t):
        xpminfo = register.getType(t)
        if xpminfo is None:
            raise Exception("%s is not an XPM type" % t)

        xpminfo.addArgument(self.argument)
        return t

class TypeArgument(AbstractArgument):
    def __init__(self, name, type=None, default=None, required=None, help=None):
        xpmtype = register.getType(type)
        logging.debug("Registering type argument %s [%s -> %s]", name, type, xpmtype)
        AbstractArgument.__init__(self, name, xpmtype, help=help)
        if default is not None and required is not None and required:
            raise Exception("Argument is required but default value is given")
        self.argument.required = (default is None) if required is None else required
        if default is not None:
            self.argument.defaultValue(Value(default))

class PathArgument(AbstractArgument):
    def __init__(self, name, path, help=""):
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


class TypeProperty:
  def __init__(self, name, value):
    self.name = name
    self.value = value

  def __call__(self, type):
    xpmType = register.getType(type)
    object = register.createObject(self.value)
    xpmType.setProperty(self.name, object)
    return type


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

class MergeClass:
    """Merge class annotation

    class A:
        def x(self): return "x"

    a = A()

    @MergeClass(A)
    class A:
        def y(self): return "y"

    a.x() # prints x
    a.y() # prints y
    """
    def __init__(self, original):
        self.original = original


    def __call__(self, _class):
        for _, method in inspect.getmembers(_class, predicate=inspect.isfunction):
            setattr(self.original, method.__name__, method)

        return None
