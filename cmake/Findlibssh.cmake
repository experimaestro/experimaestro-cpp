# - Try to find libssh
# Once done this will define
#  LIBSSH_FOUND - System has libssh
#  LIBSSH_INCLUDE_DIRS - The libssh include directories
#  LIBSSH_LIBRARIES - The libraries needed to use libssh
#  LIBSSH_DEFINITIONS - Compiler switches required for using libssh

find_package(PkgConfig)

pkg_check_modules(PC_LIBSSH QUIET libssh libssh_threads)
set(LIBSSH_DEFINITIONS ${PC_LIBSSH_CFLAGS_OTHER})

message(STATUS "Looking for libssh...")
find_path(LIBSSH_INCLUDE_DIR libssh/ssh2.h
        HINTS ${PC_LIBSSH_INCLUDEDIR} ${PC_LIBSSH_INCLUDE_DIRS}
        )

find_library(LIBSSH_LIBRARY NAMES ssh libssh
        HINTS ${PC_LIBSSH_LIBDIR} ${PC_LIBSSH_LIBRARY_DIRS} )

# Not needed with libssh >= 0.8
# find_library(LIBSSH_THREADS_LIBRARY NAMES ssh_threads libssh_threads
#         HINTS ${PC_LIBSSH_LIBDIR} ${PC_LIBSSH_LIBRARY_DIRS} )

set(LIBSSH_LIBRARIES ${LIBSSH_LIBRARY}) # ${LIBSSH_THREADS_LIBRARY})
set(LIBSSH_INCLUDE_DIRS ${LIBSSH_INCLUDE_DIR} )


# Retrieve libssh version
# execute_process()

include(FindPackageHandleStandardArgs)

# handle the QUIETLY and REQUIRED arguments and set LIBSSH_FOUND to TRUE
# if all listed variables are TRUE
find_package_handle_standard_args(LibSsh  DEFAULT_MSG
        LIBSSH_LIBRARY LIBSSH_INCLUDE_DIR)

mark_as_advanced(LIBSSH_INCLUDE_DIR LIBSSH_LIBRARY)
