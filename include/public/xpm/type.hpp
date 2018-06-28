#ifndef EXPERIMAESTRO_TYPE_HPP
#define EXPERIMAESTRO_TYPE_HPP

#include <memory>
#include <unordered_map>

#include <xpm/common.hpp>
#include <xpm/scalar.hpp>

namespace xpm {

#ifdef SWIG
#define SWIG_MUTABLE %mutable;
#define SWIG_IMMUTABLE %immutable;
#else
#define SWIG_MUTABLE
#define SWIG_IMMUTABLE
#endif


class Type;
class Object;
class Argument;
class Value;

SWIG_IMMUTABLE;
extern std::shared_ptr<Type> IntegerType;
extern std::shared_ptr<Type> RealType;
extern std::shared_ptr<Type> StringType;
extern std::shared_ptr<Type> BooleanType;
extern std::shared_ptr<Type> AnyType;
extern std::shared_ptr<Type> PathType;

extern const std::string KEY_TYPE;
extern const std::string KEY_TASK;
extern const std::string KEY_VALUE;
SWIG_MUTABLE;


// ---
// --- Namespace and type names
// ---

/**
 * Qualified name
 */
class Typename {
  std::string name;
 public:
  /** Creates a qualified name
  * @param ns the namespace
  * @param name the local name
  */
  Typename(std::string const &name);

  /** Returns a typename prefix by this */
  Typename operator()(std::string const &localname) const;

  std::string toString() const;

  Typename array() const;

  int hash() const;

  std::string localName() const;

  bool operator==(Typename const &other) const {
    return other.name == name;
  }
  bool operator!=(Typename const &other) const {
    return other.name != name;
  }
};

SWIG_IMMUTABLE;
extern const Typename STRING_TYPE;
extern const Typename BOOLEAN_TYPE;
extern const Typename INTEGER_TYPE;
extern const Typename REAL_TYPE;
extern const Typename ANY_TYPE;
extern const Typename PATH_TYPE;
SWIG_MUTABLE;

}

#ifndef SWIG
namespace std {
/** Hash of type name */
template<>
struct hash<xpm::Typename> {
  inline size_t operator()(xpm::Typename const &typeName) const { return typeName.hash(); }
};
}
#endif

namespace xpm {
  
/**
 * Object type definition
 *
 * An object type is composed of:
 * <ul>
 * <li>A unique type name</li>
 * <li>A list of typed arguments</li>
 * <li>A parent type</li>
 * </ul>
 */
class Type NOSWIG(: public std::enable_shared_from_this<Type>) {
  friend class Register;
 public:
  typedef std::shared_ptr<Type> Ptr;
  
  /// Least common ancestor between two types
  static Type::Ptr lca(Type::Ptr const & a, Type::Ptr const & b);

  /** Type destruction */
  virtual ~Type();

  /**
   * Add new arguments for this type
   * @param argument
   */
  void addArgument(std::shared_ptr<Argument> const &argument);

  /**
   * Get the arguments
   */
  std::unordered_map<std::string, std::shared_ptr<Argument>> &arguments();

  /**
   * Get the arguments
   */
  std::unordered_map<std::string, std::shared_ptr<Argument>> const &arguments() const;

  /// Returns the JSON string corresponding to this type
  std::string toJson() const;

  /// Returns the type name
  Typename const &name() const;

  /// Return the type
  std::string toString() const;

  /// Returns hash code (only based on type name)
  int hash() const;

  /// Predefined types
  bool predefined() const;

  /** Can ignore */
  inline bool canIgnore() { return _canIgnore; }

  /** Get parent type */
  Ptr parentType();

  /** Set parent type */
  void parentType(Ptr const &type);

  /** Get placeholder status */
  bool placeholder() const { return _placeholder; }
  void placeholder(bool placeholder) { _placeholder = placeholder; }

  /** Get placeholder status */
  std::string const & description() const { return _description; }
  void description(std::string const &description) { _description = description; }

  /** Set a property */
  void setProperty(std::string const &name, std::shared_ptr<Value> const &value);
  std::shared_ptr<Value> getProperty(std::string const &name);

  /** Is it a scalar */
  virtual bool scalar() { return false; }

  /** Is it an array */
  virtual bool array() { return false; }

  /// Checks whether another type can be assigned as this type
  bool accepts(Type::Ptr const &other) const;


  /// Base type for any type
  static Type::Ptr const & any();

  /**
   * Creates a new type
   * @param type The typename
   * @param _parent The parent type (or null pointer)
   */
  Type(Typename const &type, std::shared_ptr<Type> _parent = nullptr,
       bool predefined = false, bool canIgnore = false);

 private:
  const Typename _type;
  /**
   * Parent type
   */
  std::shared_ptr<Type> _parent;

  /**
   * Argument of this type.
   */
  std::unordered_map<std::string, std::shared_ptr<Argument>> _arguments;

  /**
   * Properties of this type.
   * They are useful to characterize a type when building experiments
   */
  std::unordered_map<std::string, std::shared_ptr<Value>> _properties;


  std::string _description;
  bool _predefined;
  bool _canIgnore;
  bool _placeholder = false;
};

class SimpleType : public Type {
  ScalarType _valueType;
 public:
  SimpleType(Typename const &tname, ScalarType valueType, bool canIgnore = false);
  inline ScalarType valueType() { return _valueType; }
  virtual bool scalar() override { return true; }
};

class ArrayType : public Type {
  Type::Ptr _componentType;
public:
  ArrayType(Type::Ptr const & componentType);
  virtual bool array() override { return true; }
  Type::Ptr componentType() { return _componentType; }
};

} // namespace xpm

#ifndef SWIG
namespace std {
/** Hash of type */
template<>
struct hash<xpm::Type> {
  inline size_t operator()(xpm::Type const &type) const { return type.name().hash(); }
};
}
#endif



#endif