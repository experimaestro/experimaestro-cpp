# - Try to find libyaml-cpp
# Once done this will define
#  LIBYAMLCPP_FOUND - System has libyaml-cpp
#  LIBYAMLCPP_INCLUDE_DIRS - The libyaml-cpp include directories
#  LIBYAMLCPP_LIBRARIES - The libraries needed to use libyaml-cpp
#  LIBYAMLCPP_DEFINITIONS - Compiler switches required for using libyaml-cpp

find_package(PkgConfig)
pkg_check_modules(PC_LIBYAMLCPP QUIET yaml-cpp)
set(LIBYAMLCPP_DEFINITIONS ${PC_LIBYAMLCPP_CFLAGS_OTHER})

message(STATUS "Looking for yaml-cpp...")
find_path(LIBYAMLCPP_INCLUDE_DIR yaml-cpp/yaml.h
        HINTS ${PC_LIBYAMLCPP_INCLUDEDIR} ${PC_LIBYAMLCPP_INCLUDE_DIRS}
        )

find_library(LIBYAMLCPP_LIBRARY NAMES yaml-cpp libyaml-cpp
        HINTS ${PC_LIBYAMLCPP_LIBDIR} ${PC_LIBYAMLCPP_LIBRARY_DIRS} )

set(LIBYAMLCPP_LIBRARIES ${LIBYAMLCPP_LIBRARY} )
set(LIBYAMLCPP_INCLUDE_DIRS ${LIBYAMLCPP_INCLUDE_DIR} )

include(FindPackageHandleStandardArgs)
# handle the QUIETLY and REQUIRED arguments and set LIBYAMLCPP_FOUND to TRUE
# if all listed variables are TRUE
find_package_handle_standard_args(LIBYAMLCPP  DEFAULT_MSG
        LIBYAMLCPP_LIBRARY LIBYAMLCPP_INCLUDE_DIR)

mark_as_advanced(LIBYAMLCPP_INCLUDE_DIR LIBYAMLCPP_LIBRARY)
