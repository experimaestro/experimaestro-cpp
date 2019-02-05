// --- Error

enum Error { ERROR_NONE, ERROR_UNKNOWN, ERROR_GENERIC, ERROR_RUNTIME };

enum Error lasterror_code();
const char * lasterror_message();

// --- Workspace

typedef struct Workspace Workspace;
typedef char const * const CString;

Workspace * workspace_new(CString path);
void workspace_free(Workspace *);
/// Make the workspace the current one
void workspace_current(Workspace *);
void workspace_experiment(Workspace *, CString path);
void workspace_server(Workspace *, int port, CString htdocs);
void workspace_waitUntilTaskCompleted();
