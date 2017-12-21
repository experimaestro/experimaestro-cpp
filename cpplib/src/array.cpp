#include <xpm/array.hpp>
#include "private.hpp"

namespace xpm {

Array::Array() {
  type(ArrayType);
}

Array::~Array() {}

std::array<unsigned char, DIGEST_LENGTH> Array::digest() const {
  Digest d;
  for (auto x: _array) {
    auto xDigest = x->digest();
    d.updateDigest(xDigest);
  }
  return d.get();
}

void Array::push_back(std::shared_ptr<Object> const &element) {
  _array.push_back(element);
}

std::shared_ptr<Object> Array::copy() {
  return std::make_shared<Array>(*this);
}

std::shared_ptr<Array> Array::cast(std::shared_ptr<Object> const & object) {
  return std::dynamic_pointer_cast<Array>(object);
}

size_t Array::size() const {
  return _array.size();
}

std::shared_ptr<Object> &Array::operator[](const size_t index) {
  return _array[index];
}


nlohmann::json Array::toJson() {
    nlohmann::json array = {};
    for(auto const &v: _array) {
        array.push_back(v->toJson());
    }
    return array;
}

}
