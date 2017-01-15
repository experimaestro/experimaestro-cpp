//
// Created by Benjamin Piwowarski on 13/01/2017.
//

#include "cpp.hpp"

namespace xpm {

std::shared_ptr<Register> CURRENT_REGISTER = std::make_shared<Register>();
CommandPath EXECUTABLE_PATH = CommandPath(Path("."));

}
