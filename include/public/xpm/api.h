// API conventions

// Memory:
// - returned object memory should be managed by the caller
// - callback objects memory should **not** be managed by the caller

typedef char const * const CString;


// --- All types

typedef struct AbstractCommandComponent AbstractCommandComponent;
typedef struct ArrayType ArrayType;
typedef struct ArrayValue ArrayValue;
typedef struct Argument Argument;
typedef struct Command Command;
typedef struct Command Command;
typedef struct CommandLine CommandLine;
typedef struct CommandParameters CommandParameters;
typedef struct CommandPath CommandPath;
typedef struct CommandString CommandString;
typedef struct ComplexValue ComplexValue;
typedef struct Connector Connector;
typedef struct CounterToken CounterToken;
typedef struct Dependency Dependency;
typedef struct DependencyArray DependencyArray;
typedef struct DirectConnector LocalConnector;
typedef struct DirectLauncher DirectLauncher;
typedef struct Generator Generator;
typedef struct Job Job;
typedef struct Launcher Launcher;
typedef struct MapValue MapValue;
typedef struct Object Object;
typedef struct Path Path;
typedef struct PathGenerator PathGenerator;
typedef struct Register Register;
typedef struct ScalarValue ScalarValue;
typedef struct String String;
typedef struct StringArray StringArray;
typedef struct TagValueIterator TagValueIterator;
typedef struct Task Task;
typedef struct Token Token;
typedef struct Type Type;
typedef struct Typename Typename;
typedef struct Value Value;
typedef struct Workspace Workspace;


// --- Error

enum Error { ERROR_NONE, ERROR_UNKNOWN, ERROR_GENERIC, ERROR_RUNTIME, ERROR_CAST };

enum Error lasterror_code();
const char * lasterror_message();


// --- Typename


Typename * typename_new(CString path);
Typename * typename_sub(Typename *, CString);
void typename_free(Typename *);
CString typename_name(Typename *);

// --- Type


extern Type const * ANY_TYPE;
extern Type const * BOOLEAN_TYPE;
extern Type const * INTEGER_TYPE;
extern Type const * REAL_TYPE;
extern Type const * PATH_TYPE;
extern Type const * STRING_TYPE;


CString type_tostring(Type *);
Type * type_new(Typename *, Type * parentTypeOrNull);
void type_free(Type *);
void type_addargument(Type *, Argument *);
Argument * type_getargument(Type *, CString key);
bool type_isarray(Type *);

ArrayType * arraytype_new(Type * componentType);
void arraytype_free(ArrayType *);

// --- Scalars

String * string_new(CString);
void string_free(String *);
CString string_ptr(String *);

// --- Value

void value_free(Value *);
Type * value_gettype(Value *);
MapValue * value_asmap(Value *);
ScalarValue * value_asscalar(Value *);
ArrayValue * value_asarray(Value *);
bool value_ismap(Value *);
String * value_tostring(Value *);

ScalarValue * scalarvalue_fromreal(double value);
ScalarValue * scalarvalue_fromboolean(bool value);
ScalarValue * scalarvalue_frominteger(long value);
ScalarValue * scalarvalue_frompath(Path * value);
ScalarValue * scalarvalue_frompathstring(CString value);
ScalarValue * scalarvalue_fromstring(CString value);
ScalarValue * scalarvalue_new(); // null value
void scalarvalue_tag(ScalarValue *, CString key);
void scalarvalue_free(ScalarValue *);
double scalarvalue_asreal(ScalarValue *);
bool scalarvalue_asboolean(ScalarValue *);
int scalarvalue_asinteger(ScalarValue *);
Path * scalarvalue_aspath(ScalarValue *);
String * scalarvalue_asstring(ScalarValue *);
bool scalarvalue_isnull(ScalarValue *);

void complexvalue_settagcontext(ComplexValue *, CString);

ArrayValue * arrayvalue_new();
void arrayvalue_free(ArrayValue *);
void arrayvalue_add(ArrayValue *, Value *);
unsigned long arrayvalue_size(ArrayValue *);
Value * arrayvalue_get(ArrayValue *, unsigned long);

MapValue * mapvalue_new();
void mapvalue_free(MapValue *);
void mapvalue_setobject(MapValue *, Object *);
void * mapvalue_getobjecthandle(MapValue *);
Job * mapvalue_getjob(MapValue *);
void mapvalue_settype(MapValue *, Type *);
void mapvalue_set(MapValue *, CString, Value *);
void mapvalue_addtag(MapValue *, CString, ScalarValue *);


TagValueIterator * value_tags(Value *);
void tagvalue_free(TagValueIterator *);
void tagvalueiterator_free(TagValueIterator *);
bool tagvalueiterator_next(TagValueIterator *);
CString tagvalueiterator_key(TagValueIterator *);
ScalarValue * tagvalueiterator_value(TagValueIterator *);

// --- Tokens

void dependency_free(Dependency *);

void token_free(Token *);

CounterToken * countertoken_new(int);
void countertoken_free(CounterToken *);
Dependency * countertoken_createdependency(CounterToken *, int);

// --- Job

void job_free(Job *);
Path * job_stdoutpath(Job *);
Path * job_stderrpath(Job *);

// --- Object

typedef int (*object_init_callback)(void *handle);
typedef int (*object_delete_callback)(void *handle);
typedef int (*object_setvalue_callback)(void * handle, CString, Value *);
Object * object_new(void * handle, object_init_callback, object_delete_callback, object_setvalue_callback);
void object_free(Object *);

// --- Generator

PathGenerator * pathgenerator_new(CString);
void pathgenerator_free(PathGenerator *);

// --- Argument

Argument * argument_new(CString name);
void argument_free(Argument *);
void argument_settype(Argument *, Type *);
void argument_sethelp(Argument *, CString);
void argument_setrequired(Argument *, bool);
void argument_setignored(Argument *, bool);
void argument_setdefault(Argument *, Value *);
void argument_setconstant(Argument *, Value *);
void argument_setgenerator(Argument *, Generator *);
CString argument_getname(Argument *);
CString argument_gethelp(Argument *);
Type * argument_gettype(Argument *);
Value * argument_getdefaultvalue(Argument *);

// --- Command




Command * command_new();
void command_free(Command *);
void command_add(Command *, AbstractCommandComponent *);


CommandPath * commandpath_new(Path * path);
void commandpath_free(CommandPath *);


CommandString * commandstring_new(CString str);
void commandstring_free(CommandString *);


CommandParameters * commandparameters_new();
void commandparameters_free(CommandParameters *);




CommandLine * commandline_new();
void commandline_free(CommandLine *);
void commandline_add(CommandLine *, Command *);


// --- Task


Task * task_new(Typename *, Type *);
void task_free(Task *);
Typename *task_name(Task *);
void task_commandline(Task *, CommandLine *);
bool task_isrunning();
void task_submit(Task *, Workspace *, Launcher * launcher, Value *, DependencyArray *);

// --- Path

Path * path_new(CString path);
void path_free(Path *);
String * path_string(Path *);
String * path_localpath(Path *);

// --- Register


typedef Object * (*register_create_object_callback)(void * handle, Value *) ;
typedef int (*register_run_task_callback)(void * handle, Task *, Value *) ;

Register * register_new(void * handle, register_create_object_callback, register_run_task_callback);
void register_free(Register *);
Task * register_getTask(Register *, CString);
void register_addType(Register * c_register, Type * c_type);
void register_addTask(Register * c_register, Task * c_type);
bool register_parse(Register * ptr, StringArray * arguments, bool tryParse);
Value * register_build(Register * r, CString str);

// --- Dependencies

DependencyArray * dependencyarray_new();
void dependencyarray_free(DependencyArray *);
void dependencyarray_add(DependencyArray *, Dependency *);

// --- Arrays

StringArray * stringarray_new();
void stringarray_free(StringArray *);
void stringarray_add(StringArray *, CString);


// --- Workspace

Workspace * workspace_new(CString path);
void workspace_free(Workspace *);
/// Make the workspace the current one
void workspace_current(Workspace *);
void workspace_experiment(Workspace *, CString path);
void workspace_server(Workspace *, int port, CString htdocs);
void workspace_waitUntilTaskCompleted();


// --- Connectors

LocalConnector * localconnector_new();
void localconnector_free(LocalConnector *);

// --- Launchers


DirectLauncher * directlauncher_new(Connector *);
void directlauncher_free(DirectLauncher *);

void launcher_free(Launcher *);
void launcher_setenv(Launcher *, CString key, CString value);
void launcher_setnotificationURL(Launcher *, CString);
Launcher * launcher_defaultlauncher();

// --- Misc function

enum LogLevel { LogLevel_DEBUG, LogLevel_INFO, LogLevel_WARN };

void setLogLevel(CString key, enum LogLevel level);

/**
 * Reports progress
 * @param value A float between 0 and 1
 */
void progress(float value);