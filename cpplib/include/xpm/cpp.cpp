//
// Created by Benjamin Piwowarski on 13/01/2017.
//

#include "cpp.hpp"

namespace xpm {

CommandPath EXECUTABLE_PATH = CommandPath(Path("."));

namespace {
  std::shared_ptr<Register> CURRENTREGISTER;
  void init() {
    static bool initialized = false;
    if (!initialized) {
      CURRENTREGISTER = std::make_shared<Register>();

      initialized = true;
    }
  }
}

std::shared_ptr<Register> currentRegister() {
  init();
  return CURRENTREGISTER;
}

void currentRegister(std::shared_ptr<Register> const &_register) {
  CURRENTREGISTER = _register;
}



}
