#include <stdexcept>
#include <memory>
#include <xpm/common.hpp>
#include <xpm/workspace.hpp>

using xpm::mkptr;

template<class T, class U, class... Args> 
inline U* mkcptr(Args&&... args) { 
    std::shared_ptr<T> sptr = std::make_shared<T>(std::forward<Args>(args)...);
     
    std::shared_ptr<T> * c_ptr = new std::shared_ptr<T>(sptr);
    return reinterpret_cast<U*>(c_ptr);
}

template<class T, class U> T & ref(U *c_ptr) {
    if (!c_ptr) throw std::runtime_error("Null pointer");
    auto s_ptr = *reinterpret_cast<std::shared_ptr<T>*>(c_ptr);
    if (!s_ptr) throw std::runtime_error("Null pointer");
    return *s_ptr;
}

template<class T, class U> void freecptr(U *c_ptr) {
    auto s_ptr = reinterpret_cast<std::shared_ptr<T>*>(c_ptr);
    delete s_ptr;

}

extern "C" {
    #include <xpm/api.h>

    void workspace_free(Workspace *c_ws) {
        freecptr<xpm::Workspace, Workspace>(c_ws);
    }

    Workspace * workspace_new(char const * const path) {
        return mkcptr<xpm::Workspace, Workspace>(std::string(path));
    }

    void workspace_current(Workspace *c_ws) {
        ref<xpm::Workspace>(c_ws).current();
    }
    void workspace_experiment(Workspace *c_ws, CString path) {
        ref<xpm::Workspace>(c_ws).experiment(std::string(path));
    }


    void workspace_waitUntilTaskCompleted() {
        xpm::Workspace::waitUntilTaskCompleted();
    }
} // extern "C"