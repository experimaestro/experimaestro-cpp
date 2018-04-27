//
// Created by Benjamin Piwowarski on 13/01/2017.
//

#include <xpm/cpp.hpp>


namespace xpm {

CommandPath EXECUTABLE_PATH = CommandPath(Path("."));

namespace {
  ptr<Register> CURRENTREGISTER;
  void init() {
    static bool initialized = false;
    if (!initialized) {
      CURRENTREGISTER = std::make_shared<Register>();

      initialized = true;
    }
  }
}

ptr<Register> currentRegister() {
  init();
  return CURRENTREGISTER;
}

void currentRegister(ptr<Register> const &_register) {
  CURRENTREGISTER = _register;
}



}
