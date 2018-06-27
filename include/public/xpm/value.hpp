//
// Created by Benjamin Piwowarski on 19/01/2017.
//

#ifndef EXPERIMAESTRO_VALUE_HPP
#define EXPERIMAESTRO_VALUE_HPP

#include <vector>
#include <memory>
#include <cstdint>

#include <xpm/json.hpp>
#include <xpm/common.hpp>
#include <xpm/filesystem.hpp>

namespace YAML {
  class Node;
}

namespace xpm {

struct Digest;
class Type;
class Parameters;
class Register;

enum class ValueType : int8_t {
  /* Value not set */
  UNSET, 

  /* Set to None */
  NONE, 

  INTEGER, 
  REAL, 
  STRING, 
  PATH, 
  BOOLEAN, 
  ARRAY
};


/**
 * A value
 */
class Value {
 public:
  typedef std::vector<std::shared_ptr<Parameters>> Array;
  typedef std::shared_ptr<Value> Ptr;

  Value();
  Value(double value);
  Value(bool value);
  Value(int value);
  Value(long value);
  
  Value(Path const &value);

  Value(std::string const &value);

  /// Build from YAML node
  static Value fromYAML(YAML::Node const &node);

  /// Build from string with type hint
  static Value fromString(std::string const & string, std::shared_ptr<Type> const & hint);

  inline Value(char const *value) : Value(std::string(value)) {}

  Value(Array const & array);
  Value(Array && array);

#ifndef SWIG
  Value(Register & xpmRegister, nlohmann::json const &jsonValue);
#endif

  Value(Value const &other);
  Value &operator=(Value const &other);

  virtual ~Value();
  // virtual std::shared_ptr<Parameters> copy();

  ValueType const scalarType() const;
  std::shared_ptr<Type> type() const;

  /** Is the value defined? */
  bool defined() const;

  /** Is the value null? */
  bool null() const;


  std::string const &getString();

  virtual nlohmann::json toJson() const;

  virtual bool equals(Value const &) const;

  /// Cast to other simple type
  Value cast(std::shared_ptr<Type> const &type);

  /** @defgroup content Access to value content
   *  @{
   */

  /// Get the value
  bool getBoolean() const;
  double getReal() const;
  long getInteger() const;
  Path getPath() const;

  /// Returns the string
  virtual std::string asString() const;

  /// Returns the string
  virtual bool asBoolean() const;

  /// Returns an integer
  virtual long asInteger() const;

  /// Returns an integer
  virtual double asReal() const;

  /// Returns a path
  virtual Path asPath() const;

  /// Returns as array
  virtual Array asArray() const;

  /** @} */

  NOSWIG(void updateDigest(Digest &) const);

  // Array methods (throw an exception if the value is not an array)

  /// Add a new object to the array
  void push_back(std::shared_ptr<Parameters> const &element);

  /// Returns the size of the array
  size_t size() const;

  /// Access to the new array
  std::shared_ptr<Parameters> &operator[](const size_t index);

  /// A constant
  static const Value NONE;
  
 protected:
  friend struct Helper;

private:
    /// Initialize with a given type (internal use only)
    Value(ValueType vt);

    union Union {
    long integer;
    double real;
    bool boolean;

    std::string string;
    Array array;

    ~Union();
    Union();
  } _value;
  ValueType _scalarType;
};

inline bool operator==(Value const &a, Value const &b) {
  return a.equals(b);
}

}

#endif //ANCHOR_JUDGES_VALUE_HPP
