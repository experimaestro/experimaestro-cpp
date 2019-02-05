//
// Created by Benjamin Piwowarski on 13/01/2017.
//

#include <xpm/cpp.hpp>


namespace xpm {

ptr<CommandPath> EXECUTABLE_PATH = std::make_shared<CommandPath>(Path("."));

namespace {
  ptr<CppRegister> CURRENTREGISTER;
  void init() {
    static bool initialized = false;
    if (!initialized) {
      CURRENTREGISTER = std::make_shared<CppRegister>();

      initialized = true;
    }
  }
}

ptr<CppRegister> currentRegister() {
  init();
  return CURRENTREGISTER;
}

void currentRegister(ptr<CppRegister> const &_register) {
  CURRENTREGISTER = _register;
}

/// Create object
std::shared_ptr<Object> CppRegister::createObject(std::shared_ptr<Value> const & sv) {
  auto it = constructors.find(sv->type());
  if (it == constructors.end()) {
    return mkptr<DefaultCppObject>();
  }

  return it->second();
}


}
