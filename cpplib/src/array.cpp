#include <xpm/array.hpp>
#include "private.hpp"

namespace xpm {
Array::~Array() {}

std::array<unsigned char, DIGEST_LENGTH> Array::digest() const {
  Digest d;
  for (auto x: _array) {
    auto xDigest = x->digest();
    d.updateDigest(xDigest);
  }
  return d.get();
}

void Array::add(std::shared_ptr<Object> const &element) {
  _array.push_back(element);
}

std::shared_ptr<Object> Array::copy() {
  return std::make_shared<Array>(*this);
}

nlohmann::json Array::toJson() {
    nlohmann::json array = {};
    for(auto const &v: _array) {
        array.push_back(v->toJson());
    }
    return array;
}

}
