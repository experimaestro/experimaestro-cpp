#ifndef EXPERIMAESTRO_ARRAY_HPP
#define EXPERIMAESTRO_ARRAY_HPP

#include "xpm.hpp"

namespace xpm {

class Array : public Object {
 public:
  typedef std::vector<std::shared_ptr<Object>> Content;
  virtual ~Array();
  Array();

  virtual std::shared_ptr<Object> copy() override;
  void add(std::shared_ptr<Object> const &element);
  virtual std::array<unsigned char, DIGEST_LENGTH> digest() const override;
  virtual nlohmann::json toJson() override;

  size_t size() const;
  std::shared_ptr<Object> &operator[](const size_t index);

  static std::shared_ptr<Array> cast(std::shared_ptr<Object> const &);
 private:
  Content _array;
};

}

#endif
