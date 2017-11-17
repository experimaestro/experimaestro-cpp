#ifndef EXPERIMAESTRO_ARRAY_HPP
#define EXPERIMAESTRO_ARRAY_HPP

#include "xpm.hpp"

namespace xpm {
class Array : public Object {
 public:
  typedef std::vector<std::shared_ptr<Object>> Content;
  virtual ~Array();

  virtual std::shared_ptr<Object> copy() override;
  void add(std::shared_ptr<Object> const &element);
  virtual std::array<unsigned char, DIGEST_LENGTH> digest() const override;
  virtual nlohmann::json toJson() override;
 private:
  Content _array;
};
}

#endif
