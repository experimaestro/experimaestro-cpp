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
class Value;
class Register;

enum class ScalarType : int8_t {
  /* Scalar not set */
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
class Scalar {
 public:
  typedef std::shared_ptr<Scalar> Ptr;

  Scalar();
  Scalar(double value);
  Scalar(bool value);
  Scalar(int value);
  Scalar(long value);
  
  Scalar(Path const &value);

  Scalar(std::string const &value);

  /// Build from YAML node
  static Scalar fromYAML(YAML::Node const &node);

  /// Build from string with type hint
  static Scalar fromString(std::string const & string, std::shared_ptr<Type> const & hint);

  inline Scalar(char const *value) : Scalar(std::string(value)) {}

#ifndef SWIG
  Scalar(nlohmann::json const &jsonValue);
#endif

  Scalar(Scalar const &other);
  Scalar &operator=(Scalar const &other);

  virtual ~Scalar();
  // virtual std::shared_ptr<Value> copy();

  ScalarType const scalarType() const;
  std::shared_ptr<Type> type() const;

  /** Is the value defined? */
  bool defined() const;

  /** Is the value null? */
  bool null() const;


  std::string const &getString();

  virtual nlohmann::json toJson() const;

  virtual bool equals(Scalar const &) const;

  /// Cast to other simple type
  Scalar cast(std::shared_ptr<Type> const &type);

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
  static const Scalar NONE;
  
 protected:
  friend struct Helper;

private:
    /// Initialize with a given type (internal use only)
    Scalar(ScalarType vt);

    union Union {
    long integer;
    double real;
    bool boolean;

    std::string string;

    ~Union();
    Union();
  } _value;
  ScalarType _scalarType;
};

inline bool operator==(Scalar const &a, Scalar const &b) {
  return a.equals(b);
}


/// TODO: Fuse this with Scalar
class ScalarValue : public Value {
public:
  virtual ~ScalarValue() = default;
  /// Constructs from value
  ScalarValue(Scalar const & v);

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
  ScalarType valueType() const;

  virtual bool equals(Value const &other) const override;
  NOSWIG(virtual void outputJson(std::ostream &out, CommandContext & context) const override);
  virtual void updateDigest(Digest & digest) const override;
  
  virtual std::shared_ptr<Value> copy() override;
  virtual std::shared_ptr<Type> type() const override;

private:
  /// The associated value
  Scalar _value;

  friend class Value;
};




} // endns: xpm

#endif //ANCHOR_JUDGES_VALUE_HPP
