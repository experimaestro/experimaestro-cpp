/**
 * This file contains helpers for c++ code
 */

#ifndef EXPERIMAESTRO_CPP_HPP
#define EXPERIMAESTRO_CPP_HPP

#include <map>

#include <xpm/common.hpp>
#include <xpm/register.hpp>
#include <xpm/scalar.hpp>
#include <xpm/xpm.hpp>
#include <xpm/type.hpp>

namespace xpm {

template<class T> struct CppType;

/// Register for C++ projects
class CppRegister : public Register {
public:
  /// Run task
  virtual void runTask(std::shared_ptr<Task> const & task, std::shared_ptr<Value> const & sv) override;

  /// Create object
  virtual std::shared_ptr<Object> createObject(std::shared_ptr<Value> const & sv) override;

  void addType(ptr<Type> const & type) {
      Register::addType(type);
  }

  template<typename T> void addConstructor(ptr<CppType<T>> const & cppType) {
      constructors[cppType->type] = [] { return mkptr<T>(); };
  }
private:
  /// Maps types to constructors
  std::map<ptr<Type>, std::function<ptr<Object>()>> constructors;
};

std::shared_ptr<CppRegister> currentRegister();
void currentRegister(std::shared_ptr<CppRegister> const &_register);

template <typename T> struct type_of {};

template <typename T> struct ArgumentHolder {
  virtual ~ArgumentHolder() {}
  virtual void setValue(T &self, xpm::Value::Ptr const &value) = 0;
};

inline void assignValue(Value::Ptr const &sv, std::string &s) {
    s = std::dynamic_pointer_cast<ScalarValue>(sv)->asString();
}
inline void assignValue(Value::Ptr const &sv, int &x) {
  x = std::dynamic_pointer_cast<ScalarValue>(sv)->asInteger();
}
inline void assignValue(Value::Ptr const &sv, long &x) {
  x = std::dynamic_pointer_cast<ScalarValue>(sv)->asInteger();
}
inline void assignValue(Value::Ptr const &sv, Path &s) {
  s = std::dynamic_pointer_cast<ScalarValue>(sv)->asPath();
}
// inline void assignValue(Value::Ptr const &sv, Scalar::Array &s) {
//   s = sv->asArray();
// }


/// Assigning another object
template <typename T>
inline void assignValue(xpm::Value::Ptr const &value,
                        std::shared_ptr<T> &p) {
  
  auto object = value->asMap()->object();                        
  p = std::dynamic_pointer_cast<T>(object);

  if (!p && value) {
    std::cerr << demangle(p) << " and " << demangle(object) << std::endl;
    throw xpm::argument_error(std::string("Expected ") +
                              type_of<std::shared_ptr<T>>::value()->toString() +
                              " but got " + value->type()->toString());
  }
}

template <typename T, typename Scalar>
struct TypedArgumentHolder : public ArgumentHolder<T> {
  Scalar T::*valuePtr;

  TypedArgumentHolder(Scalar T::*valuePtr) : valuePtr(valuePtr) {}

  virtual void setValue(T &self,
                        xpm::Value::Ptr const &value) override {
    xpm::assignValue(value, (&self)->*valuePtr);
  }
};

template <typename T> struct CppType {
  std::shared_ptr<Type> type;
  std::unordered_map<std::string, std::unique_ptr<ArgumentHolder<T>>>
      _arguments;

  static std::shared_ptr<CppType<T>> SELF;

  CppType(std::shared_ptr<Type> const & type) : type(type) {}
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
XPM_SIMPLETYPE_OF(std::shared_ptr<Value>, AnyType);
XPM_SIMPLETYPE_OF(Path, PathType);

template<typename Self>
struct BaseCppTypeBuilder {
  std::shared_ptr<Argument> _argument;
  std::shared_ptr<CppRegister> _register = currentRegister();
  std::shared_ptr<Type> type;

  BaseCppTypeBuilder(std::string const &name, ptr<CppRegister> const &_register) {
    if (_register) { this->_register = _register; }

    type = mkptr<Type>(name);
    _register->addType(type);
  }

  virtual ~BaseCppTypeBuilder() = default;


  /// Implicit conversion to CppType
  operator std::shared_ptr<Type>() { return type; }

  template<typename U>
  Self &argument(std::string const &name) {
    _argument = mkptr<Argument>(name);
    type->addArgument(_argument);
    _argument->type(type_of<U>::value());
    return dynamic_cast<Self&>(*this);
  }

  Self &required(bool r) {
    _argument->required(r);
    return dynamic_cast<Self&>(*this);
  }

  Self &generator(std::shared_ptr<Generator> const &g) {
    _argument->generator(g);
    return dynamic_cast<Self&>(*this);
  }

  Self &ignore(bool flag) {
    _argument->ignored(flag);
    return dynamic_cast<Self&>(*this);
  }


  template <typename U> Self &defaultValue(U const &v) {
    _argument->defaultValue(std::make_shared<ScalarValue>(Scalar(v)));
    return dynamic_cast<Self&>(*this);
  }

  template <typename U> Self &constant(U const &v) {
    _argument->constant(std::make_shared<ScalarValue>(Scalar(v)));
    return dynamic_cast<Self&>(*this);
  }

  Self &defaultJson(nlohmann::json const &j) {
    _argument->defaultValue(std::make_shared<Value>(*_register, j));
    return dynamic_cast<Self&>(*this);
  }
};

struct SimpleCppTypeBuilder : public BaseCppTypeBuilder<SimpleCppTypeBuilder> {
  SimpleCppTypeBuilder(std::string const &name, ptr<CppRegister> const &_register) :
    BaseCppTypeBuilder(name, _register) {
  }
};

template <typename T, typename Parent> 
struct CppTypeBuilder : public BaseCppTypeBuilder<CppTypeBuilder<T,Parent>> {

  CppTypeBuilder(std::string const &name)
      : BaseCppTypeBuilder<CppTypeBuilder<T,Parent>>(name, currentRegister()) {

   // Sets the CppType of this object 
   CppType<T>::SELF = mkptr<CppType<T>>(this->type);
   this->_register->addConstructor(CppType<T>::SELF);

   if (!std::is_same<xpm::Object, Parent>::value) {
      this->type->parentType(type_of<std::shared_ptr<Parent>>::value());
    }
  }

  template <typename U>
  CppTypeBuilder &argument(std::string const &name, U T::*u) {
    this->_argument = std::make_shared<Argument>(name);
    this->type->addArgument(this->_argument);
    CppType<T>::SELF->_arguments[name] =
        std::unique_ptr<ArgumentHolder<T>>(new TypedArgumentHolder<T, U>(u));
    this->_argument->type(type_of<U>::value());
    return *this;
  }


  /// Implicit conversion to CppType
  operator std::shared_ptr<CppType<T>>() { return CppType<T>::SELF; }

};

class DefaultCppObject : public Object {
public:
  void setValue(std::string const &name,
                ptr<Value> const &value) override {
  }
};


template <typename T, typename Parent = Object>
class CppObject : public Parent {
  ptr<Value> _sv;
public:
  static std::shared_ptr<CppType<T>> XPM_TYPE;

  ptr<Value> value() { return _sv; }
  void setValue(std::string const &name,
                ptr<Value> const &value) override {
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
        std::make_shared<Task>(Typename(tname), CppType<_Type>::SELF->type);

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

#define DEFAULT_XPM_TYPE(TYPENAME, NAME) \
    struct TYPENAME : public DefaultCppObject {}; \
    XPM_TYPE(NAME, TYPENAME)

#endif // PROJECT_CPP_HPP
