/**
 * This file contains helpers for c++ code
 */

#ifndef EXPERIMAESTRO_CPP_HPP
#define EXPERIMAESTRO_CPP_HPP

#include "xpm.hpp"

namespace xpm {

extern std::shared_ptr<Register> CURRENT_REGISTER;

struct _ArgumentHolder {

};

template<typename T>
struct argument_traits;

template<typename T>
struct argument_traits {
  static_assert(std::is_class<T>::value, "Template type T is not class");
  typedef std::shared_ptr<T> Type;
  static T *pointer(Type const &t) {
    return t.get();
  }
};

#define XPM_BASIC_AGUMENT_TRAIT(BasicType) template<>  struct argument_traits<BasicType> { \
    typedef BasicType Type; \
    static BasicType *pointer(BasicType &t) { return &t; }\
  };

XPM_BASIC_AGUMENT_TRAIT(std::string);
XPM_BASIC_AGUMENT_TRAIT(int);
XPM_BASIC_AGUMENT_TRAIT(long);
XPM_BASIC_AGUMENT_TRAIT(float);
XPM_BASIC_AGUMENT_TRAIT(bool);

template<typename T>
struct ArgumentHolder : public _ArgumentHolder {
  std::shared_ptr<Argument> argument;
  typedef typename argument_traits<T>::Type TType;

  TType value;

  ArgumentHolder() : argument(std::make_shared<Argument>()) {
  }

  TType &operator*() {
    return value;
  }

  decltype(argument_traits<T>::pointer(value)) operator->() {
    return argument_traits<T>::pointer(value);
  }

  ArgumentHolder required(bool _required) {
    argument->required(_required);
    return *this;
  }
};

struct TypeBuilder {
  Type &type;

  TypeBuilder(Type &type) : type(type) {
  }

  void add() {}

  template<typename T, typename ...Ts>
  void add(std::string const &name, ArgumentHolder<T> a, Ts... args) {
    a.argument->name(name);
    type.addArgument(a.argument);
    add(args...);
  }
};

template<typename T>
struct RegisterType {
  struct Factory : public ObjectFactory {
    Factory(const std::shared_ptr<Register> &theRegister) : ObjectFactory(theRegister) {
    }

    std::shared_ptr<Object> _create() const override {
      return std::make_shared<T>();
    }
  };

  std::shared_ptr<Type> type;
  RegisterType(std::string const &name, std::shared_ptr<Type> const &parentType = nullptr) : type(std::make_shared<Type>(TypeName(name))) {
    type->parentType(parentType);
  }

  void addType() {
    T t;
    t._fillType(*type);

    type->objectFactory(std::make_shared<Factory>(CURRENT_REGISTER));

    CURRENT_REGISTER->addType(type);
    return type;
  }
};

struct CppObject : public Object {
  void setValue(std::string const &name, std::shared_ptr<Object> const &value) override {

  }
};

template<typename T>
struct TaskBuilder {
  TaskBuilder(std::string const &tname) {
    auto task = std::make_shared<Task>(T::TYPE);
    CURRENT_REGISTER->addTask(task);
  }
};

}

#define XPM_TYPE_CLASS(TYPE) struct TYPE : public CppType<TYPE>
#define XPM_DECLARE_TYPE(NAME, TYPE) \
  typedef xpm::CppType<TYPE> XPM_TYPE; \
  static constexpr const char *TYPENAME = NAME;


#define XPM_ARGUMENTS(...) \
  static std::shared_ptr<Type> TYPE; \
  void _fillType(Type &type) { TypeBuilder(type).add(__VA_ARGS__); }

#define XPM_TYPE(tname, cpptype) std::shared_ptr<Type> cpptype::TYPE = RegisterType<cpptype>::type(tname);
#define XPM_CHILDTYPE(supertype, tname, cpptype) std::shared_ptr<Type> cpptype::TYPE = RegisterType<cpptype>::type(tname, supertype::TYPE);

#define XPM_TASK(tname, type) TaskBuilder<type> TASK ## __LINE__ (tname);
#endif //PROJECT_CPP_HPP
