# - Try to find SQLite3
# Once done, this will define
#
#  SQLite3_FOUND - system has SQLite3
#  SQLite3_INCLUDE_DIR - the SQLite3 include directories
#  SQLite3_LIBRARY - link these to use SQLite3

include(LibFindMacros)

# Use pkg-config to get hints about paths
libfind_pkg_check_modules(SQLite3_PKGCONF sqlite3)

# Include dir
find_path(SQLite3_INCLUDE_DIR
  NAMES sqlite3.h
  PATHS ${SQLite3_PKGCONF_INCLUDE_DIRS}
)

# Finally the library itself
find_library(SQLite3_LIBRARY
  NAMES sqlite3
  PATHS ${SQLite3_PKGCONF_LIBRARY_DIRS}
)

# Set the include dir variables and the libraries and let libfind_process do the rest.
# NOTE: Singular variables for this library, plural for libraries this this lib depends on.
set(SQLite3_PROCESS_INCLUDES SQLite3_INCLUDE_DIR SQLite3_INCLUDE_DIR)
set(SQLite3_PROCESS_LIBS SQLite3_LIBRARY SQLite3_LIBRARY)
libfind_process(SQLite3)