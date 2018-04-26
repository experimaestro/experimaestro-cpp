//
// Created by Benjamin Piwowarski on 19/01/2017.
//

#ifndef EXPERIMAESTRO_VALUE_HPP
#define EXPERIMAESTRO_VALUE_HPP

#include <xpm/xpm.hpp>

namespace xpm {

class Type;
class StructuredValue;
class Register;

enum class ValueType : int8_t {
  NONE, INTEGER, REAL, STRING, PATH, BOOLEAN, ARRAY
};


/**
 * A value
 */
class Value {
 public:
  typedef std::vector<std::shared_ptr<StructuredValue>> Array;
  typedef std::shared_ptr<Value> Ptr;

  Value();
  Value(double value);
  Value(bool value);
  Value(int value);
  Value(long value);
  
  Value(Path const &value);

  Value(std::string const &value);
  inline Value(char const *value) : Value(std::string(value)) {}

  Value(Array const & array);
  Value(Array && array);

#ifndef SWIG
  Value(Register & xpmRegister, nlohmann::json const &jsonValue);
#endif

  Value(Value const &other);
  Value &operator=(Value const &other);

  virtual ~Value();
  // virtual std::shared_ptr<StructuredValue> copy();

  ValueType const scalarType() const;
  std::shared_ptr<Type> type() const;

  /** Is the value defined? */
  bool defined() const;


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
  virtual std::string asString();

  /// Returns the string
  virtual bool asBoolean();

  /// Returns an integer
  virtual long asInteger();

  /// Returns an integer
  virtual double asReal();

  /// Returns a path
  virtual Path asPath() const;

  /// Returns as array
  virtual Array asArray() const;

  /** @} */

  virtual std::array<unsigned char, DIGEST_LENGTH> digest() const;


  // Array methods (throw an exception if the value is not an array)

  /// Add a new object to the array
  void push_back(std::shared_ptr<StructuredValue> const &element);

  /// Returns the size of the array
  size_t size() const;

  /// Access to the new array
  std::shared_ptr<StructuredValue> &operator[](const size_t index);

 protected:
  friend struct Helper;

  private:
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

// FIXME: Remove - just for reference
// #ifndef EXPERIMAESTRO_ARRAY_HPP
// #define EXPERIMAESTRO_ARRAY_HPP

// #include "xpm.hpp"

// namespace xpm {

// class Array : public Object {
//  public:
//   typedef std::vector<std::shared_ptr<StructuredValue>> Content;
//   virtual ~Array();
//   Array();

//   /// Shallow copy of the array
//   virtual std::shared_ptr<StructuredValue> copy() override;
  
//   /// Computes the hash for the object
//   virtual std::array<unsigned char, DIGEST_LENGTH> digest() const override;

//   /// Transforms into JSON
//   virtual nlohmann::json toJson() override;


//   /// Cast an object to Array type
//   static std::shared_ptr<Array> cast(std::shared_ptr<StructuredValue> const &);
//  private:
//   Content _array;
// };

// }

// #endif
