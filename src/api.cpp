#include <stdexcept>
#include <memory>

#include <xpm/common.hpp>
#include <xpm/type.hpp>
#include <xpm/connectors/local.hpp>
#include <xpm/launchers/launchers.hpp>
#include <xpm/xpm.hpp>
#include <xpm/logging.hpp>
#include <xpm/register.hpp>
#include <xpm/workspace.hpp>
#include <__xpm/common.hpp>
extern "C" {
    #include <xpm/api.h>
}

DEFINE_LOGGER("api");

using xpm::mkptr;
namespace {
    std::string lasterror_string;
    bool STOPPING = false;
}

template<typename T> struct C_API_HELPER {};

#define DECLARE_XPM_COBJECT(NAME, CLASS, CPPCLASS) \
    template<> struct C_API_HELPER<CPPCLASS> { \
        typedef CLASS CObject; \
    }; \
    template<> struct C_API_HELPER<CLASS> { \
        typedef CPPCLASS Object; \
    }; \
    extern "C" void NAME ## _free(CLASS * c_ptr) { freecptr(c_ptr); } \

#define XPM_CUSTOM_COBJECT(NAME, CLASS, CPPCLASS, ...) \
    DECLARE_XPM_COBJECT(NAME, CLASS, CPPCLASS) \
    extern "C" CLASS * NAME ## _new (__VA_ARGS__) 


#define XPM_COBJECT(NAME, CLASS, ...) XPM_CUSTOM_COBJECT(NAME, CLASS, xpm::CLASS, __VA_ARGS__)


#define XPM_CATCH_BLOCK \
    catch(std::runtime_error &e) { lasterror = ERROR_RUNTIME; lasterror_string = e.what();  return; } \
    catch(std::exception &e) { lasterror = ERROR_GENERIC; lasterror_string = e.what();  return; } \
    catch(...) { lasterror = ERROR_UNKNOWN;  lasterror_string = "Unknown exception"; return; }


template<class T, class... Args> 
inline typename C_API_HELPER<T>::CObject * mkcptr(Args&&... args) { 
    std::shared_ptr<T> s_ptr = std::make_shared<T>(std::forward<Args>(args)...);
     
    std::shared_ptr<T> * c_ptr = new std::shared_ptr<T>(s_ptr);
    LOGGER->debug("Created shared pointer {} at {} (count={}) : pointer {}", 
        demangle(*s_ptr), (void*)s_ptr.get(), s_ptr.use_count(), (void*) c_ptr);
    return reinterpret_cast<typename C_API_HELPER<T>::CObject *>(c_ptr);
}

template<class U> void freecptr(U *c_ptr) {
    typedef typename C_API_HELPER<U>::Object T;
    auto s_ptr = reinterpret_cast<std::shared_ptr<T>*>(c_ptr);
    LOGGER->debug("Freeing shared pointer {} at {} (count={}) : pointer {}", 
        demangle(*s_ptr), (void*)s_ptr->get(), s_ptr->use_count(), (void*) c_ptr);
    delete s_ptr;
}

template<class T> auto newcptr(std::shared_ptr<T> const & s_ptr) {
    typedef typename C_API_HELPER<T>::CObject U;
    if (!s_ptr) return (U*)nullptr;

    std::shared_ptr<T> * c_ptr = new std::shared_ptr<T>(s_ptr);
    LOGGER->debug("Copied shared pointer {} at {} (count={}) : pointer {}", 
        demangle(*s_ptr), (void*)s_ptr.get(), s_ptr.use_count(), (void*) c_ptr);
    return reinterpret_cast<U *>(c_ptr);
}

template<class U> typename C_API_HELPER<U>::Object & c2ref(U *c_ptr) {
    typedef typename C_API_HELPER<U>::Object T;
    if (!c_ptr) throw std::runtime_error("Null pointer");
    auto s_ptr = *reinterpret_cast<std::shared_ptr<T>*>(c_ptr);
    if (!s_ptr) throw std::runtime_error("Null pointer");
    return *s_ptr;
}

template<class U> std::shared_ptr<typename C_API_HELPER<U>::Object> const & c2sptr(U *c_ptr) {
    typedef typename C_API_HELPER<U>::Object T;
    static std::shared_ptr<T> NULL_PTR;
    if (!c_ptr) return NULL_PTR;
    return *reinterpret_cast<std::shared_ptr<T>*>(c_ptr);
}

namespace {
struct ApiObject : public xpm::Object {
    void * self;
    object_init_callback init_cb;
    object_delete_callback delete_cb; 
    object_setvalue_callback setvalue_cb;

    ApiObject(void * self, object_init_callback init_cb, object_delete_callback delete_cb, 
        object_setvalue_callback setvalue_cb)
         : self(self), init_cb(init_cb), delete_cb(delete_cb), setvalue_cb(setvalue_cb) {
        LOGGER->debug("Created API object with handle {}", self);
    }

    virtual ~ApiObject() {
        // TODO: maybe do something better?
        if (!STOPPING) {
            LOGGER->debug("Deleting API object with handle {}", self);
            auto error = init_cb(self);
            if (error) {
                LOGGER->error("Error while calling object::delete");
            }
        }
    }
    
    virtual void init() override {
        LOGGER->debug("Init API object with handle {}", self);
        auto error = init_cb(self);
        if (error) throw std::runtime_error("Error while calling object::init");
    }

    virtual void setValue(std::string const & name, xpm::ptr<xpm::Value> const & value) override {            
        LOGGER->debug("Set value {} for API object with handle {}", name, self);
        auto p = std::const_pointer_cast<xpm::Value>(value);
        auto error = setvalue_cb(self, name.c_str(), reinterpret_cast<Value *>(&p));
        if (error) throw std::runtime_error("Error while calling object::setvalue");
    }
};

typedef typename std::map<std::string, xpm::Scalar> TagValueMap;

struct TagValueIteratorCpp {
    TagValueMap map;
    TagValueMap::const_iterator iterator;
    std::string key;
    xpm::Scalar scalar;
    
    TagValueIteratorCpp(TagValueMap && map) : map(map) {
        iterator = this->map.begin();
    }
    bool next() {
        if (map.end() == iterator) {
            scalar = xpm::Scalar();
            return false;
        }
        key = iterator->first;
        scalar = iterator->second;
        ++iterator;
        return true;
    }
};

} // unnamed

// Abstract classes

DECLARE_XPM_COBJECT(connector, Connector, xpm::Connector);
DECLARE_XPM_COBJECT(value, Value, xpm::Value);
DECLARE_XPM_COBJECT(token, Token, xpm::Token);
DECLARE_XPM_COBJECT(complexvalue, ComplexValue, xpm::ComplexValue);
DECLARE_XPM_COBJECT(launcher, Launcher, xpm::Launcher);
DECLARE_XPM_COBJECT(generator, Generator, xpm::Generator);
DECLARE_XPM_COBJECT(dependency, Dependency, xpm::Dependency);
DECLARE_XPM_COBJECT(job, Job, xpm::Job);
DECLARE_XPM_COBJECT(abstractcommandcomponent, AbstractCommandComponent, xpm::AbstractCommandComponent);
DECLARE_XPM_COBJECT(tagvalueiterator, TagValueIterator, TagValueIteratorCpp);


XPM_COBJECT(typename, Typename, CString name) {
    return mkcptr<xpm::Typename>(name);
}

XPM_COBJECT(countertoken, CounterToken, int tokens) {
    return mkcptr<xpm::CounterToken>(tokens);
}

XPM_COBJECT(type, Type, Typename * typeName, Type * parentTypeOrNull) {
    return mkcptr<xpm::Type>(c2ref(typeName), c2sptr(parentTypeOrNull));
}

XPM_COBJECT(task, Task, Typename * c_tn, Type * c_type) {
    if (c_tn) {
        return mkcptr<xpm::Task>(c2ref(c_tn), c2sptr(c_type));
    }
    return mkcptr<xpm::Task>(c2sptr(c_type));
}


XPM_COBJECT(path, Path, CString path) {
    return mkcptr<xpm::Path>(path);
}

XPM_COBJECT(command, Command) {
    return mkcptr<xpm::Command>();
}

XPM_COBJECT(commandline, CommandLine) {
    return mkcptr<xpm::CommandLine>();
}

XPM_COBJECT(commandpath, CommandPath, Path * path) {
    return mkcptr<xpm::CommandPath>(c2ref(path));
}

XPM_COBJECT(commandstring, CommandString, CString str) {
    return mkcptr<xpm::CommandString>(str);
}

XPM_COBJECT(commandparameters, CommandParameters) {
    return mkcptr<xpm::CommandParameters>();
}

XPM_COBJECT(argument, Argument, CString c_str) {
    return mkcptr<xpm::Argument>(c_str);
}


XPM_CUSTOM_COBJECT(object, Object, ApiObject, void * handle,
     object_init_callback init_cb, object_delete_callback delete_cb, 
        object_setvalue_callback setvalue_cb) {
    return mkcptr<ApiObject>(handle, init_cb, delete_cb, setvalue_cb);
}


XPM_COBJECT(directlauncher, DirectLauncher, Connector *c_connector) {
    return mkcptr<xpm::DirectLauncher>(c2sptr(c_connector));
}

XPM_COBJECT(workspace, Workspace, CString path) {
    return mkcptr<xpm::Workspace>(std::string(path));
}

XPM_COBJECT(mapvalue, MapValue) {
    return mkcptr<xpm::MapValue>();
}

XPM_COBJECT(pathgenerator, PathGenerator, CString path) {
    return mkcptr<xpm::PathGenerator>(path);
}

XPM_COBJECT(arrayvalue, ArrayValue) {
    return mkcptr<xpm::ArrayValue>();
}

XPM_COBJECT(localconnector, LocalConnector) {
    return mkcptr<xpm::LocalConnector>();
}

XPM_COBJECT(arraytype, ArrayType, Type * componentType) {
    return mkcptr<xpm::ArrayType>(c2sptr(componentType));
}

XPM_COBJECT(scalarvalue, ScalarValue) {
    return mkcptr<xpm::ScalarValue>();
}

XPM_CUSTOM_COBJECT(dependencyarray, DependencyArray, std::vector<std::shared_ptr<xpm::Dependency>>) {
    return mkcptr<std::vector<std::shared_ptr<xpm::Dependency>>>();
}

XPM_CUSTOM_COBJECT(stringarray, StringArray, std::vector<std::string>) {
    return mkcptr<std::vector<std::string>>();
}

XPM_CUSTOM_COBJECT(string, String, std::string, CString str) {
    return mkcptr<std::string>(str);
}



namespace {

class ApiRegister : public xpm::Register {
    void * handle;
    register_create_object_callback create_callback;
    register_run_task_callback run_callback;
public:
    ApiRegister(void * handle, register_create_object_callback create_callback, register_run_task_callback run_callback) 
        : handle(handle), create_callback(create_callback), run_callback(run_callback) {}
    
    virtual void runTask(std::shared_ptr<xpm::Task> const & task, std::shared_ptr<xpm::Value> const & value) {
        auto _task = std::const_pointer_cast<xpm::Task>(task);
        auto _value = std::const_pointer_cast<xpm::Value>(value);
        auto error = run_callback(handle, reinterpret_cast<Task*>(&_task), reinterpret_cast<Value*>(&_value));
        if (error) throw std::runtime_error("Error while running task");
    }

    virtual std::shared_ptr<xpm::Object> createObject(std::shared_ptr<xpm::Value> const & value) {
        auto _value = std::const_pointer_cast<xpm::Value>(value);
        Object * object = create_callback(handle, reinterpret_cast<Value *>(&_value));
        if (object == nullptr) {
            throw xpm::assertion_error("Object should not be null in register::createObject");
        }
        return c2sptr(object);
    }

};
}


XPM_CUSTOM_COBJECT(register, Register, ApiRegister, void * handle, 
    register_create_object_callback create_callback, register_run_task_callback run_callback) {
    return mkcptr<ApiRegister>(handle, create_callback, run_callback);
}

extern "C" {
    
    Error lasterror;

    Type const * BOOLEAN_TYPE = reinterpret_cast<Type const *>(&xpm::BooleanType);
    Type const * INTEGER_TYPE = reinterpret_cast<Type const *>(&xpm::IntegerType);
    Type const * REAL_TYPE = reinterpret_cast<Type const *>(&xpm::RealType);
    Type const * PATH_TYPE = reinterpret_cast<Type const *>(&xpm::PathType);
    Type const * STRING_TYPE = reinterpret_cast<Type const *>(&xpm::StringType);
    Type const * ANY_TYPE = reinterpret_cast<Type const *>(&xpm::AnyType);

    // Error lasterror;
    const char * lasterror_message() {
        return lasterror_string.c_str();
    }
    enum Error lasterror_code() {
        return lasterror;
    }

    #define API_ERROR(CODE, MESSAGE) lasterror_string = MESSAGE; lasterror = CODE;
    #define API_NO_ERROR() API_ERROR(ERROR_NONE, "")
    #define CATCHALL(INSTR) catch(...) { API_ERROR(ERROR_UNKNOWN, "Caught exception in C++ code"); INSTR }
    
    CString argument_getname(Argument * argument) {
        return c2ref(argument).name().c_str();
    }
    Type * argument_gettype(Argument * argument) {
        return newcptr(c2ref(argument).type());
    }
    Value * argument_getdefaultvalue(Argument * argument) {
        return newcptr(c2ref(argument).defaultValue());
    }
    CString argument_gethelp(Argument * argument) {
        return c2ref(argument).help().c_str();
    }

    void argument_settype(Argument * argument, Type * type) {
        c2ref(argument).type(c2sptr(type));
    }

    void argument_sethelp(Argument * argument, CString str) {
        c2ref(argument).help(str);
    }

    void argument_setrequired(Argument * argument, bool required) {
        c2ref(argument).required(required);
    }

    void argument_setignored(Argument * argument, bool ignored) {
        c2ref(argument).ignored(ignored);
    }

    void argument_setdefault(Argument * argument, Value * value) {
        c2ref(argument).defaultValue(c2sptr(value));
    }

    void argument_setconstant(Argument * argument, Value * value) {
        c2ref(argument).constant(c2sptr(value));
    }

    void argument_setgenerator(Argument * argument, Generator * generator) {
        c2ref(argument).generator(c2sptr(generator));
    }


    void arrayvalue_add(ArrayValue * array, Value * value) {
        c2ref(array).push_back(c2sptr(value));
    }
    size_t arrayvalue_size(ArrayValue * array) {
        return c2ref(array).size();
    }
    Value * arrayvalue_get(ArrayValue * array, size_t index) {
        return newcptr(c2ref(array)[index]);
    }


    void command_add(Command * command, AbstractCommandComponent * component) {
        c2ref(command).add(c2sptr(component));
    }
    
    void commandline_add(CommandLine * commandline, Command * command) {
        c2ref(commandline).add(c2sptr(command));
    }

    void complexvalue_settagcontext(ComplexValue * value, CString key) {
        c2ref(value).setTagContext(key);
    }

    Dependency * countertoken_createdependency(CounterToken * token, int count) {
        return newcptr(c2ref(token).createDependency(count));
    }

    void dependencyarray_add(DependencyArray * array, Dependency * dependency) {
        c2ref(array).push_back(c2sptr(dependency));
    }

    JobState job_state(Job * job) {
        switch(c2ref(job).state()) {
            case xpm::JobState::WAITING: return JOB_WAITING;
            case xpm::JobState::READY: return JOB_READY;
            case xpm::JobState::RUNNING: return JOB_RUNNING;
            case xpm::JobState::DONE: return JOB_DONE;
            case xpm::JobState::ERROR: return JOB_ERROR;
        }
        return JOB_UNKNOWN;
    }

    JobState job_wait(Job * job) {
        c2ref(job).wait();
        return job_state(job);
    }

    Path * job_codepath(Job * job) {
        return mkcptr<xpm::Path>(c2ref(job).pathTo(xpm::EXIT_CODE_PATH));
    }

    Path * job_stdoutpath(Job * job) {
        return mkcptr<xpm::Path>(c2ref(job).stdoutPath());
    }
    Path * job_stderrpath(Job * job) {
        return mkcptr<xpm::Path>(c2ref(job).stderrPath());        
    }

    void mapvalue_addtag(MapValue * value, CString key, ScalarValue * scalar) {
        c2ref(value).addTag(key, c2ref(scalar).value());
    }

    void * mapvalue_getobjecthandle(MapValue * value) {
        auto object = c2ref(value).object();
        if (auto apiobject = std::dynamic_pointer_cast<ApiObject>(object)) {
            return apiobject->self;
            API_NO_ERROR();
        }
        API_ERROR(ERROR_CAST, "Cannot cast Object to ApiObject");
        return nullptr;
    }

    Job * mapvalue_getjob(MapValue * value) {
        auto sptr = c2ref(value).job();
        return sptr ? newcptr(sptr) : nullptr;
    }


    void mapvalue_set(MapValue * mapvalue, CString key, Value * value) {
        c2ref(mapvalue).set(key, c2sptr(value));
    }

    void mapvalue_set_unsets(MapValue * mapvalue) {
        c2ref(mapvalue).setUnsets();
    }

    void mapvalue_setobject(MapValue * value, Object * object) {
        c2ref(value).object(c2sptr(object));
    }
    void mapvalue_settype(MapValue * value, Type * type) {
        c2ref(value).type(c2sptr(type));
    }

    String * path_string(Path * path) {
        return mkcptr<std::string>(c2ref(path).toString());
    }

    String * path_localpath(Path * path) {
        try {
            auto local = c2ref(path).localpath();
            return mkcptr<std::string>(local);
        } catch(...) {
            // not a local path
        }
        return nullptr;
    }

    void register_addType(Register * c_register, Type * c_type) {
        c2ref(c_register).addType(c2sptr(c_type));
    }
    void register_addTask(Register * c_register, Task * c_task) {
        c2ref(c_register).addTask(c2sptr(c_task));
    }
    bool register_parse(Register * ptr, StringArray * arguments, bool tryParse) {
        return c2ref(ptr).parse(c2ref(arguments), tryParse);
    }

    Value * register_build(Register * r, CString str) {
        return newcptr(c2ref(r).build(str));
    }
    
    ScalarValue * scalarvalue_fromreal(double value) {
        return mkcptr<xpm::ScalarValue>(value);
    }
    ScalarValue * scalarvalue_fromboolean(bool value) {
        return mkcptr<xpm::ScalarValue>(value);
    }
    ScalarValue * scalarvalue_frominteger(long value) {
        return mkcptr<xpm::ScalarValue>(value);
    }
    ScalarValue * scalarvalue_frompath(Path * value) {
        return mkcptr<xpm::ScalarValue>(c2ref(value));
    }
    ScalarValue * scalarvalue_frompathstring(CString value) {
        return mkcptr<xpm::ScalarValue>(xpm::Path(value));
    }
    ScalarValue * scalarvalue_fromstring(CString value) {
        return mkcptr<xpm::ScalarValue>(std::string(value));
    }
    void scalarvalue_tag(ScalarValue * value, CString key) {
        c2ref(value).tag(key);
    }

    bool scalarvalue_isnull(ScalarValue * scalar) {
        return c2ref(scalar).null();
    }


    double scalarvalue_asreal(ScalarValue * value) {
        return c2ref(value).asReal();
    }
    bool scalarvalue_asboolean(ScalarValue * value) {
        return c2ref(value).asBoolean();
    }
    int scalarvalue_asinteger(ScalarValue * value) {
        return c2ref(value).asInteger();
    }
    Path * scalarvalue_aspath(ScalarValue * value) {
        return mkcptr<xpm::Path>(c2ref(value).asPath());
    }
    String * scalarvalue_asstring(ScalarValue * value) {
        return mkcptr<std::string>(c2ref(value).asString());
    }

    CString string_ptr(String * string) {
        return c2ref(string).c_str();
    }

    void stringarray_add(StringArray *array, CString str) {
        c2ref(array).push_back(str);
    }

    bool task_isrunning() {
        return xpm::Task::isRunning();
    }

    void task_commandline(Task * task, CommandLine * commandline) {
        c2ref(task).commandline(c2sptr(commandline));
    }

    Typename * task_name(Task *c_ptr) {
        return mkcptr<xpm::Typename>(c2ref(c_ptr).name());
    }

    void task_submit(Task * task, Workspace * ws, Launcher * launcher, Value * value, DependencyArray * deps) {
        c2ref(task).submit(c2sptr(ws), c2sptr(launcher), c2sptr(value), c2ref(deps));
    }


    void type_addargument(Type * type, Argument * argument) {
        c2ref(type).addArgument(c2sptr(argument));
    }
    Argument * type_getargument(Type * type, CString key) {
        auto arg = c2ref(type).argument(key);
        return arg ? newcptr(arg) : nullptr;
    }

    bool type_isarray(Type * type) {
        return c2ref(type).array();
    }

    CString type_tostring(Type *c_type) {
        return c2ref(c_type).name().toString().c_str();
    }

    CString typename_name(Typename *c_ptr) {
        return c2ref(c_ptr).toString().c_str();
    }

    Typename * typename_sub(Typename *tn, CString str) {
        return mkcptr<xpm::Typename>(c2ref(tn), str);
    }

    void launcher_setenv(Launcher *c_ptr, CString key, CString value) {
        c2ref(c_ptr).environment()[key] = value;
    }

    Launcher * launcher_defaultlauncher() {
        return newcptr(xpm::Launcher::defaultLauncher());
    }

    void launcher_setnotificationURL(Launcher * launcher, CString url) {
        c2ref(launcher).notificationURL(url);
    }


    Type * value_gettype(Value * value) {
        return newcptr(c2ref(value).type());
    }

    ArrayValue * value_asarray(Value * value) {
        auto sptr = std::dynamic_pointer_cast<xpm::ArrayValue>(c2sptr(value));
        return sptr ? newcptr(sptr) : nullptr;
    }

    MapValue * value_asmap(Value * value) {
        auto sptr = std::dynamic_pointer_cast<xpm::MapValue>(c2sptr(value));
        return sptr ? newcptr(sptr) : nullptr;
    }
    ScalarValue * value_asscalar(Value * value) {
        auto sptr = std::dynamic_pointer_cast<xpm::ScalarValue>(c2sptr(value));
        return sptr ? newcptr(sptr) : nullptr;
    }
    bool value_ismap(Value * value) {
        return (bool)std::dynamic_pointer_cast<xpm::MapValue>(c2sptr(value));
    }

    String * value_tostring(Value * value) {
        return mkcptr<std::string>(c2ref(value).toString());
    }

    TagValueIterator * value_tags(Value * value) {
        return mkcptr<TagValueIteratorCpp>(std::move(c2ref(value).tags()));
    }
    bool tagvalueiterator_next(TagValueIterator * iterator) {
        return c2ref(iterator).next();
    }
    CString tagvalueiterator_key(TagValueIterator * iterator) {
        return c2ref(iterator).key.c_str();
    }
    ScalarValue * tagvalueiterator_value(TagValueIterator * iterator) {
        return mkcptr<xpm::ScalarValue>(c2ref(iterator).scalar);
    }


    void workspace_current(Workspace *c_ws) {
        xpm::Workspace::current(c2sptr(c_ws));
    }
    void workspace_experiment(Workspace *c_ws, CString path) {
        c2ref(c_ws).experiment(std::string(path));
    }
    void workspace_server(Workspace *c_ws, int port, CString htdocs) {
        try { 
            c2ref(c_ws).server(port, std::string(htdocs)); 
            lasterror = ERROR_NONE;
        } XPM_CATCH_BLOCK
    }   

    void workspace_waitUntilTaskCompleted() {
        xpm::Workspace::waitUntilTaskCompleted();
    }


    void setLogLevel(CString key, LogLevel c_level) {
        xpm::LogLevel level = xpm::LogLevel::INFO;
        switch(c_level) {
            case LogLevel_TRACE: level = xpm::LogLevel::TRACE; break;
            case LogLevel_DEBUG: level = xpm::LogLevel::DEBUG; break;
            case LogLevel_INFO: level = xpm::LogLevel::INFO; break;
            case LogLevel_WARN: level = xpm::LogLevel::WARN; break;
            case LogLevel_ERROR: level = xpm::LogLevel::ERROR; break;
        }

        xpm::setLogLevel(key, level);
    }

    void progress(float value) {
        xpm::progress(value);
    }

    void stopping() {
        STOPPING = true;
    }

} // extern "C"