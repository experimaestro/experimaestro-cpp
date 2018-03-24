#ifndef EXPERIMAESTRO_ARRAY_HPP
#define EXPERIMAESTRO_ARRAY_HPP

#include "xpm.hpp"

namespace xpm {

class Array : public Object {
 public:
  typedef std::vector<std::shared_ptr<Object>> Content;
  virtual ~Array();
  Array();

  /// Shallow copy of the array
  virtual std::shared_ptr<Object> copy() override;
  
  /// Computes the hash for the object
  virtual std::array<unsigned char, DIGEST_LENGTH> digest() const override;

  /// Transforms into JSON
  virtual nlohmann::json toJson() override;

  /// Add a new object to the array
  void push_back(std::shared_ptr<Object> const &element);

  /// Returns the size of the array
  size_t size() const;

  /// Access to the new array
  std::shared_ptr<Object> &operator[](const size_t index);

  /// Cast an object to Array type
  static std::shared_ptr<Array> cast(std::shared_ptr<Object> const &);
 private:
  Content _array;
};

}

#endif
