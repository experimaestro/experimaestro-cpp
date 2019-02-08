typedef char const * const CString;

// --- Error

enum Error { ERROR_NONE, ERROR_UNKNOWN, ERROR_GENERIC, ERROR_RUNTIME };

enum Error lasterror_code();
const char * lasterror_message();


// --- Typename

typedef struct Typename Typename;

Typename * typename_new(CString path);
void typename_free(Typename *);
CString typename_name(Typename *);


// --- Type

typedef struct Type Type;

extern Type const * ANY_TYPE;
extern Type const * BOOLEAN_TYPE;
extern Type const * INTEGER_TYPE;
extern Type const * REAL_TYPE;
extern Type const * PATH_TYPE;
extern Type const * STRING_TYPE;


CString type_tostring(Type *);
Type * type_new(Typename *, Type * parentTypeOrNull);
void type_free(Type *);


// --- Value

typedef struct Value Value;

// --- Object

typedef struct Object Object;

typedef void (*object_setvalue_callback)(CString, Value *) ;

Object * object_new(object_setvalue_callback);
void object_free(Object *);

// --- Argument

typedef struct Argument Argument;
Argument * argument_new(CString name);
void argument_free(Argument *);

// --- Task

typedef struct Task Task;
Task * task_new(Typename *, Type *);
void task_free(Task *);

// --- Register

typedef struct Register Register;

Register * register_new();
void register_free(Register *);
Task * register_getTask(Register *, CString);
void register_addType(Register * c_register, Type * c_type);
void register_addTask(Register * c_register, Task * c_type);

// --- Workspace

typedef struct Workspace Workspace;

Workspace * workspace_new(CString path);
void workspace_free(Workspace *);
/// Make the workspace the current one
void workspace_current(Workspace *);
void workspace_experiment(Workspace *, CString path);
void workspace_server(Workspace *, int port, CString htdocs);
void workspace_waitUntilTaskCompleted();


// --- Connectors

typedef struct Connector Connector;
typedef struct DirectConnector LocalConnector;

LocalConnector * localconnector_new();
void localconnector_free(LocalConnector *);

// --- Launchers


typedef struct Launcher Launcher;
typedef struct DirectLauncher DirectLauncher;

DirectLauncher * directlauncher_new(Connector *);
void directlauncher_free(DirectLauncher *);

void launcher_setenv(Launcher *, CString key, CString value);


// --- Misc function

enum LogLevel { LogLevel_DEBUG, LogLevel_INFO };

void setLogLevel(CString key, enum LogLevel level);
