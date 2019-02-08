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
extern "C" {
    #include <xpm/api.h>
}

using xpm::mkptr;
namespace {
    std::string lasterror_string;
}

#define XPM_CATCH_BLOCK \
    catch(std::runtime_error &e) { lasterror = ERROR_RUNTIME; lasterror_string = e.what();  return; } \
    catch(std::exception &e) { lasterror = ERROR_GENERIC; lasterror_string = e.what();  return; } \
    catch(...) { lasterror = ERROR_UNKNOWN;  lasterror_string = "Unknown exception"; return; }

template<class T, class U, class... Args> 
inline U* mkcptr(Args&&... args) { 
    std::shared_ptr<T> sptr = std::make_shared<T>(std::forward<Args>(args)...);
     
    std::shared_ptr<T> * c_ptr = new std::shared_ptr<T>(sptr);
    return reinterpret_cast<U*>(c_ptr);
}

template<class T, class U> void freecptr(U *c_ptr) {
    auto s_ptr = reinterpret_cast<std::shared_ptr<T>*>(c_ptr);
    delete s_ptr;

}

template<class T, class U> T & c2ref(U *c_ptr) {
    if (!c_ptr) throw std::runtime_error("Null pointer");
    auto s_ptr = *reinterpret_cast<std::shared_ptr<T>*>(c_ptr);
    if (!s_ptr) throw std::runtime_error("Null pointer");
    return *s_ptr;
}


template<class T, class U> std::shared_ptr<T> const & c2sptr(U *c_ptr) {
    static std::shared_ptr<T> NULL_PTR;
    if (!c_ptr) return NULL_PTR;
    return *reinterpret_cast<std::shared_ptr<T>*>(c_ptr);
}



namespace {
class _Object : public xpm::Object {
    object_setvalue_callback setvalue;
public:
    _Object(object_setvalue_callback setvalue) : setvalue(setvalue) {}
    virtual void setValue(std::string const & name, xpm::ptr<xpm::Value> const & value) override {            
        auto p = std::const_pointer_cast<xpm::Value>(value);
        setvalue(name.c_str(), reinterpret_cast<Value *>(&p));
    }
};
} // unnamed

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

    // --- Typename

    Typename * typename_new(CString name) {
        return mkcptr<xpm::Typename, Typename>(name);
    }
    void typename_free(Typename *c_ptr) {
        freecptr<xpm::Typename, Typename>(c_ptr);
    }
    CString typename_name(Typename *c_ptr) {
        return c2ref<xpm::Typename>(c_ptr).toString().c_str();
    }


    // --- Types

    Type * type_new(Typename * typeName, Type * parentTypeOrNull) {
        return mkcptr<xpm::Type, Type>(c2ref<xpm::Typename>(typeName), c2sptr<xpm::Type>(parentTypeOrNull));
    }
    void type_free(Type * c_ptr) {
        freecptr<xpm::Type, Type>(c_ptr);
    }
    CString type_tostring(Type *c_type) {
        return c2ref<xpm::Type>(c_type).name().toString().c_str();
    }

    // --- Task

    Task * task_new(Typename * c_tn, Type * c_type) {
        if (c_tn)
            return mkcptr<xpm::Task, Task>(c2ref<xpm::Typename>(c_tn), c2sptr<xpm::Type>(c_type));
        return mkcptr<xpm::Task, Task>(c2sptr<xpm::Type>(c_type));

    }
    void task_free(Task * c_task) {

    }


    // --- Objects

    void object_free(Object *c_ws) {
        freecptr<_Object, Object>(c_ws);
    }

    Object * object_new(object_setvalue_callback setvalue) {
        return mkcptr<_Object, Object>(setvalue);
    }

    // --- Argument

    Argument * argument_new(CString c_str) {
        return mkcptr<xpm::Argument, Argument>(c_str);
    }
    void argument_free(Argument * c_ptr) {
        freecptr<xpm::Argument, Argument>(c_ptr);
    }


    // ---- Register

    void register_free(Register *c_ws) {
        freecptr<xpm::Register, Register>(c_ws);
    }

    Register * register_new() {
        return mkcptr<xpm::Register, Register>();
    }

    void register_addType(Register * c_register, Type * c_type) {
        c2ref<xpm::Register>(c_register).addType(c2sptr<xpm::Type>(c_type));
    }
    void register_addTask(Register * c_register, Task * c_task) {
        c2ref<xpm::Register>(c_register).addTask(c2sptr<xpm::Task>(c_task));
    }

    // --- Connectors

    LocalConnector * localconnector_new() {
        return mkcptr<xpm::LocalConnector, LocalConnector>();
    }
    void localconnector_free(LocalConnector *c_ptr) {
        freecptr<xpm::LocalConnector, LocalConnector>(c_ptr);
    }


    // --- Launchers

    void launcher_setenv(Launcher *c_ptr, CString key, CString value) {
        c2ref<xpm::Launcher>(c_ptr).environment()[key] = value;
    }

    DirectLauncher * directlauncher_new(Connector *c_connector) {
        return mkcptr<xpm::DirectLauncher, DirectLauncher>(c2sptr<xpm::Connector>(c_connector));
    }
    void directlauncher_free(DirectLauncher *c_ptr) {
        freecptr<xpm::DirectLauncher, DirectLauncher>(c_ptr);
    }

    // ---- Workspace
    void workspace_free(Workspace *c_ws) {
        freecptr<xpm::Workspace, Workspace>(c_ws);
    }

    Workspace * workspace_new(char const * const path) {
        return mkcptr<xpm::Workspace, Workspace>(std::string(path));
    }

    void workspace_current(Workspace *c_ws) {
        c2ref<xpm::Workspace>(c_ws).current();
    }
    void workspace_experiment(Workspace *c_ws, CString path) {
        c2ref<xpm::Workspace>(c_ws).experiment(std::string(path));
    }
    void workspace_server(Workspace *c_ws, int port, CString htdocs) {
        try { 
            c2ref<xpm::Workspace>(c_ws).server(port, std::string(htdocs)); 
            lasterror = ERROR_NONE;
        } XPM_CATCH_BLOCK
    }   

    void workspace_waitUntilTaskCompleted() {
        xpm::Workspace::waitUntilTaskCompleted();
    }


    // --- Other functions

    void setLogLevel(CString key, LogLevel c_level) {
        xpm::LogLevel level = xpm::LogLevel::INFO;
        switch(c_level) {
            case LogLevel_DEBUG: level = xpm::LogLevel::DEBUG; break;
            case LogLevel_INFO: level = xpm::LogLevel::INFO; break;
        }

        xpm::setLogLevel(key, level);
    }

} // extern "C"