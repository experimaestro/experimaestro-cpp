/**
 * This file contains helpers for c++ code
 */

#ifndef EXPERIMAESTRO_CPP_HPP
#define EXPERIMAESTRO_CPP_HPP

#include "common.hpp"
#include "register.hpp"
#include "value.hpp"
#include "xpm.hpp"

namespace xpm {

/// Register for C++ projects
class CppRegister : public Register {
  /// Run task
  virtual void runTask(std::shared_ptr<Task> const & task, std::shared_ptr<StructuredValue> const & sv) override;

  /// Create object
  virtual std::shared_ptr<Object> createObject(std::shared_ptr<StructuredValue> const & sv) override;
};

std::shared_ptr<Register> currentRegister();
void currentRegister(std::shared_ptr<Register> const &_register);

template <typename T> struct type_of {};

template <typename T> struct ArgumentHolder {
  virtual void setValue(T &self, xpm::StructuredValue::Ptr const &value) = 0;
};

inline void assignValue(StructuredValue::Ptr const &sv, std::string
&s) {
  s = sv->asString();
}
inline void assignValue(StructuredValue::Ptr const &sv, int &x) {
  x = sv->asInteger();
}
inline void assignValue(StructuredValue::Ptr const &sv, long &x) {
  x = sv->asInteger();
}
inline void assignValue(StructuredValue::Ptr const &sv, Path &s) {
  s = Path(sv->asString());
}
inline void assignValue(StructuredValue::Ptr const &sv, Value::Array &s) {
  s = sv->asArray();
}


/// Assigning another object
template <typename T>
inline void assignValue(xpm::StructuredValue::Ptr const &value,
                        std::shared_ptr<T> &p) {
  p = std::dynamic_pointer_cast<T>(value->object());
  if (!p && value) {
    throw xpm::argument_error(std::string("Expected ") +
                              type_of<std::shared_ptr<T>>::value()->toString() +
                              " but got " + value->type()->toString());
  }
}

template <typename T, typename Value>
struct TypedArgumentHolder : public ArgumentHolder<T> {
  Value T::*valuePtr;

  TypedArgumentHolder(Value T::*valuePtr) : valuePtr(valuePtr) {}

  virtual void setValue(T &self,
                        xpm::StructuredValue::Ptr const &value) override {
    xpm::assignValue(value, (&self)->*valuePtr);
  }
};

template <typename T> struct CppType {
  std::shared_ptr<Type> type;
  std::unordered_map<std::string, std::unique_ptr<ArgumentHolder<T>>>
      _arguments;

  static std::shared_ptr<CppType<T>> SELF;

  CppType(std::string const &name)
      : type(std::make_shared<Type>(TypeName(name))) {}
};
template <typename T> std::shared_ptr<CppType<T>> CppType<T>::SELF;

/// Type of a variable
template <typename T> struct type_of<std::shared_ptr<T>> {
  static std::shared_ptr<Type> value() { return CppType<T>::SELF->type; }
};

#define XPM_SIMPLETYPE_OF(CPPTYPE, XPMTYPE)                                    \
  template <> struct type_of<CPPTYPE> {                                        \
    static std::shared_ptr<Type> value() { return XPMTYPE; }                   \
  };

XPM_SIMPLETYPE_OF(std::string, StringType);
XPM_SIMPLETYPE_OF(bool, BooleanType);
XPM_SIMPLETYPE_OF(long, IntegerType);
XPM_SIMPLETYPE_OF(int, IntegerType);
XPM_SIMPLETYPE_OF(float, RealType);
XPM_SIMPLETYPE_OF(double, RealType);
XPM_SIMPLETYPE_OF(std::shared_ptr<StructuredValue>, AnyType);
XPM_SIMPLETYPE_OF(Path, PathType);

template <typename T, typename Parent> struct CppTypeBuilder {
  std::shared_ptr<CppType<T>> type;
  std::shared_ptr<Argument> _argument;
  std::shared_ptr<Register> _register = currentRegister();

  CppTypeBuilder(std::string const &name)
      : type(std::make_shared<CppType<T>>(name)) {
    // Add the type to the register
    currentRegister()->addType(type->type);
    CppType<T>::SELF = type;

    std::cerr << "Adding " << type->type->typeName().toString() << " to the register" << std::endl;

    if (!std::is_same<xpm::Object, Parent>::value) {
      type->type->parentType(type_of<std::shared_ptr<Parent>>::value());
    }
  }

  template <typename U>
  CppTypeBuilder &argument(std::string const &name, U T::*u) {
    _argument = std::make_shared<Argument>(name);
    type->type->addArgument(_argument);
    type->_arguments[name] =
        std::unique_ptr<ArgumentHolder<T>>(new TypedArgumentHolder<T, U>(u));
    _argument->type(type_of<U>::value());
    return *this;
  }
  CppTypeBuilder &required(bool r) {
    _argument->required(r);
    return *this;
  }

  CppTypeBuilder &generator(std::shared_ptr<Generator> const &g) {
    _argument->generator(g);
    return *this;
  }

  template <typename U> CppTypeBuilder &defaultValue(U const &v) {
    _argument->defaultValue(std::make_shared<StructuredValue>(Value(v)));
    return *this;
  }

  CppTypeBuilder &defaultJson(nlohmann::json const &j) {
    _argument->defaultValue(std::make_shared<StructuredValue>(*_register, j));
    return *this;
  }
  operator std::shared_ptr<CppType<T>>() { return type; }
};

template <typename T, typename Parent = Object>
class CppObject : public Parent {
  ptr<StructuredValue> _sv;
public:
  static std::shared_ptr<CppType<T>> XPM_TYPE;

  ptr<StructuredValue> value() { return _sv; }
  void setValue(std::string const &name,
                ptr<StructuredValue> const &value) override {
    auto it = XPM_TYPE->_arguments.find(name);
    if (it != XPM_TYPE->_arguments.end()) {
      T &t = dynamic_cast<T &>(*this);
      it->second->setValue(t, value);
    } else {
      Parent::setValue(name, value);
    }
  }
};

extern std::shared_ptr<CommandPath> EXECUTABLE_PATH;

template <typename _Type, typename _Task> struct TaskBuilder {

  static_assert(std::is_base_of<_Type, _Task>::value,
                "Task should be a subclass of type");
  TaskBuilder(std::string const &tname) {
    auto task =
        std::make_shared<Task>(TypeName(tname), CppType<_Type>::SELF->type);

    auto commandLine = std::make_shared<CommandLine>();
    auto command = std::make_shared<Command>();
    command->add(EXECUTABLE_PATH);
    command->add(std::make_shared<CommandString>("run"));
    command->add(std::make_shared<CommandString>(tname));
    command->add(std::make_shared<CommandParameters>());
    commandLine->add(command);
    task->commandline(commandLine);
    currentRegister()->addTask(task);
  }
};

} // namespace xpm

#define XPM_SIMPLETASK(tname, TYPE)                                            \
  xpm::TaskBuilder<TYPE, TYPE> TASK##__LINE__(tname);
#define XPM_TASK(tname, TYPE, TASK)                                            \
  xpm::TaskBuilder<TYPE, TASK> TASK##__LINE__(tname);

#define XPM_SUBTYPE(NAME, TYPE, PARENT)                                        \
  template <>                                                                  \
  std::shared_ptr<xpm::CppType<TYPE>> xpm::CppObject<TYPE, PARENT>::XPM_TYPE = \
      xpm::CppTypeBuilder<TYPE, PARENT>(NAME)

#define XPM_TYPE(NAME, TYPE) XPM_SUBTYPE(NAME, TYPE, xpm::Object)

#endif // PROJECT_CPP_HPP
