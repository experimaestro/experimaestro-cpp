# Try to find FMT
# Once done, this will define
#
#  FMT_FOUND - system has FMT
#  FMT_INCLUDE_DIR - the FMT include directories
#  FMT_LIBRARY - link these to use FMT

include(LibFindMacros)

# Use pkg-config to get hints about paths
libfind_pkg_check_modules(FMT_PKGCONF fmt)

# Include dir
find_path(FMT_INCLUDE_DIR
  NAMES fmt/format.h
  PATHS ${FMT_PKGCONF_INCLUDE_DIRS}
)

# Finally the library itself
find_library(FMT_LIBRARY
  NAMES fmt
  PATHS ${FMT_PKGCONF_LIBRARY_DIRS}
)

# Set the include dir variables and the libraries and let libfind_process do the rest.
# NOTE: Singular variables for this library, plural for libraries this this lib depends on.
set(FMT_PROCESS_INCLUDES FMT_INCLUDE_DIR FMT_INCLUDE_DIR)
set(FMT_PROCESS_LIBS FMT_LIBRARY FMT_LIBRARY)
libfind_process(FMT)