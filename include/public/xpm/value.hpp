//
// Created by Benjamin Piwowarski on 19/01/2017.
//

#ifndef EXPERIMAESTRO_VALUE_HPP
#define EXPERIMAESTRO_VALUE_HPP

#include <vector>
#include <memory>
#include <cstdint>

#include <xpm/xpm.hpp>
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
  BOOLEAN
};


/**
 * A value
 */
class Value {
 public:
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

#ifndef SWIG
  Value(nlohmann::json const &jsonValue);
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

  /** @} */

  NOSWIG(void updateDigest(Digest &) const);

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

    ~Union();
    Union();
  } _value;
  ValueType _scalarType;
};

inline bool operator==(Value const &a, Value const &b) {
  return a.equals(b);
}


/// TODO: Fuse this with Value
class ScalarParameters : public Parameters {
public:
  /// Constructs from value
  ScalarParameters(Value const & v);

  /// Returns the string
  std::string asString() const;

  /// Returns the string
  bool asBoolean() const;

  /// Returns an integer
  long asInteger() const;

  /// Returns an integer
  double asReal() const;

  /// Returns a path
  Path asPath() const;

  void set(bool value);
  void set(long value);
  void set(std::string const & value, bool typeHint = false);
  void set(YAML::Node const &node);

  /// Returns true if the value is defined
  bool hasValue() const;

  /// Returns true if the value is defined and null
  bool null() const;

  nlohmann::json toJson() const override;
  ValueType valueType() const;

  virtual bool equals(Parameters const &other) const override;
  virtual void outputJson(std::ostream &out, CommandContext & context) const override;
  virtual void updateDigest(Digest & digest) const override;
  
  virtual std::shared_ptr<Parameters> copy() override;

private:
  /// The associated value
  Value _value;

  friend class Parameters;
};




} // endns: xpm

#endif //ANCHOR_JUDGES_VALUE_HPP
