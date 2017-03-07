//
// Created by Benjamin Piwowarski on 19/01/2017.
//

#ifndef EXPERIMAESTRO_VALUE_HPP
#define EXPERIMAESTRO_VALUE_HPP

#include <xpm/xpm.hpp>

namespace xpm {

class Type;

/**
 * A value
 */
class Value : public Object {
  union Union {
    long integer;
    double real;
    bool boolean;
    std::string string;

    ~Union();
    Union();
  } _value;
  ValueType _scalarType;
 public:
  typedef std::shared_ptr<Value> Ptr;

  Value();
  Value(double value);
  Value(bool value);
  Value(int value);
  Value(long value);
  Value(std::string const &value);
  Value(Path const &value);
  inline Value(char const *value) : Value(std::string(value)) {}
  Value(Value const &other);
  Value &operator=(Value const &other);

  template<typename T>
  static Ptr create(T const &t) {
    return std::make_shared<Value>(t);
  }

  virtual ~Value();
  virtual std::shared_ptr<Object> copy() override;

  ValueType const scalarType() const;

  /** Is the value defined? */
  bool defined() const;

  /// Get the value
  bool getBoolean() const;
  double getReal() const;
  long getInteger() const;
  Path getPath() const;

  virtual void findDependencies(std::vector<std::shared_ptr<rpc::Dependency>> &dependencies) override;

  std::string const &getString();
  virtual nlohmann::json toJson() override;

  virtual bool equals(Object const &) const override;
  bool equals(Value const &b) const;

  /// Cast to other simple type
  std::shared_ptr<Object> cast(Type::Ptr const &type);

  /// Returns the string
  virtual std::string asString() override;

  /// Returns the string
  virtual bool asBoolean() override;

  /// Returns an integer
  virtual long asInteger() override;

  /// Returns an integer
  virtual double asReal() override;

  /// Returns a path
  virtual Path asPath() const override;

  virtual std::array<unsigned char, DIGEST_LENGTH> digest() const override;

  /// Json value
  nlohmann::json jsonValue() const;

 protected:
  friend struct Helper;
};
inline bool operator==(Value const &a, Value const &b) {
  return a.equals(b);
}

}

#endif //ANCHOR_JUDGES_VALUE_HPP
