cmake_minimum_required(VERSION 3.0.0)
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_CURRENT_SOURCE_DIR}/cmake/")

project(experimaestro)
set(LINKED_LIBRARIES "")
set(INCLUDE_DIRECTORIES "")

include(UseSWIG)
include(CMakeHelpers)
include(CMakeFindExtensions)

find_package(SWIG REQUIRED)
find_package(OpenSSL REQUIRED)
   find_package(libssh REQUIRED)
find_package(Poco REQUIRED COMPONENTS NetSSL SQLite)

# --- Set YAML options
SET(BUILD_SHARED_LIBS ON CACHE BOOL "Build shared library (YAML)")
set(YAML_CPP_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(YAML_CPP_BUILD_CONTRIB OFF CACHE BOOL "" FORCE)

set(GCC_EXTRA_OPTIONS "${GCC_EXTRA_OPTIONS} -fPIC")
add_subdirectory(${CMAKE_CURRENT_LIST_DIR}/ext/yaml-cpp EXCLUDE_FROM_ALL)

# ---

add_definitions("-Wall -Werror=return-type")

# Enables PIC (position independent code)
enable_testing()


set(PUBLIC_HEADERS
        include/public/xpm/commandline.hpp
        include/public/xpm/cpp.hpp 
        include/public/xpm/filesystem.hpp
        include/public/xpm/json.hpp
        include/public/xpm/logging.hpp
        include/public/xpm/register.hpp
        include/public/xpm/task.hpp
        include/public/xpm/type.hpp 
        include/public/xpm/scalar.hpp
        include/public/xpm/workspace.hpp
        include/public/xpm/xpm.hpp
        
        include/public/xpm/connectors/connectors.hpp
        include/public/xpm/connectors/ssh.hpp
        include/public/xpm/connectors/local.hpp

        include/public/xpm/launchers/launchers.hpp
        include/public/xpm/launchers/oar.hpp

        include/public/xpm/rpc/client.hpp
        include/public/xpm/rpc/jsonrpcclient.hpp
)

set(PRIVATE_HEADERS
    include/private/__xpm/common.hpp
    include/private/__xpm/scriptbuilder.hpp
    include/private/__xpm/jsonserialization.hpp
)

set(SOURCES
        # Include files
        ${PUBLIC_HEADERS} 
        ${PRIVATE_HEADERS}

        # Source files
        src/xpm.cpp
        src/filesystem.cpp
        src/workspace.cpp
        src/commandline.cpp
        src/private.cpp
        src/cpp.cpp 
        src/task.cpp 
        src/common.cpp 
        src/register.cpp 
        src/jsonserialization.cpp
        src/scalar.cpp
        src/scriptbuilder.cpp
        src/type.cpp

        # Launchers
        src/launchers/launchers.cpp 
        src/launchers/oar.cpp 

        # Connectors
        src/connectors/connectors.cpp        
        src/connectors/ssh.cpp        
        src/connectors/local.cpp        
        src/connectors/local_unix.cpp        

        # RPC sources
        src/rpc/notifications.cpp
        src/rpc/jsonrpcclient.cpp
        src/rpc/configuration.cpp
        src/rpc/client.cpp
        src/rpc/server.cpp
)
        
find_package (Threads)
list(APPEND LINKED_LIBRARIES 
    ${OPENSSL_LIBRARIES} 
    ${LIBSSH_LIBRARIES} 
    ${CMAKE_THREAD_LIBS_INIT} 
    ${Poco_LIBRARIES}
    yaml-cpp
    sqlite3
    )
message(STATUS "Linked libraries: ${LINKED_LIBRARIES}")
SET(EXT_INSTALL_DIR "${CMAKE_BINARY_DIR}/external/googletest")

list(APPEND INCLUDE_DIRECTORIES 
    "${CMAKE_CURRENT_SOURCE_DIR}/ext"
    "${CMAKE_CURRENT_SOURCE_DIR}/include/public"
    "${CMAKE_CURRENT_SOURCE_DIR}/include/private"
    "${CMAKE_CURRENT_SOURCE_DIR}/ext/asio/include"
    "${CMAKE_CURRENT_SOURCE_DIR}/ext/spdlog/include"
)
include_directories(${INCLUDE_DIRECTORIES})


# --- experimaestro library

add_library(experimaestro_shared SHARED ${SOURCES})
install(TARGETS experimaestro_shared LIBRARY DESTINATION lib)
install(DIRECTORY include/xpm DESTINATION include)

foreach(xpmlib experimaestro_shared)
    target_link_libraries(${xpmlib} ${LINKED_LIBRARIES})
    target_include_directories(${xpmlib}  PRIVATE ${LIBSSH_INCLUDE_DIR})
    set_target_properties(${xpmlib} PROPERTIES 
        CXX_STANDARD 17
        OUTPUT_NAME experimaestro)

endforeach()

# ---- experimaestro executable

add_executable(experimaestro experimaestro/main.cpp)
target_link_libraries(experimaestro experimaestro_shared ${LINKED_LIBRARIES})
set_property(TARGET experimaestro PROPERTY CXX_STANDARD 17)


# --- Add googletest

add_subdirectory(test)

# --- Generate documentation

find_package(Doxygen REQUIRED)

set(DOXYGEN_INPUT_DIR ${CMAKE_CURRENT_SOURCE_DIR}/include)
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/Doxyfile.in ${CMAKE_CURRENT_BINARY_DIR}/Doxyfile @ONLY)

add_custom_command(
        OUTPUT xml/index.xml
        COMMAND ${DOXYGEN_EXECUTABLE} Doxyfile
        COMMAND touch xml/index.xml # Necessary when doxygen does not update the documentation
        WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
        COMMENT "Generating XML documentation with Doxygen" VERBATIM
        DEPENDS ${PUBLIC_HEADERS} ${CMAKE_CURRENT_SOURCE_DIR}/Doxyfile.in
)
add_custom_target(documentation_index DEPENDS xml/index.xml)

add_custom_command(
        OUTPUT documentation.i
        COMMAND ${PYTHON_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/doxy2swig.py
            --function-signature --type-info --overloaded-functions --attribute-list --constructor-list
            ${CMAKE_CURRENT_BINARY_DIR}/xml/index.xml ${CMAKE_CURRENT_BINARY_DIR}/documentation.i
        DEPENDS documentation_index
        COMMENT "Generating Swig documentation" VERBATIM
)

add_custom_target(documentation DEPENDS documentation.i)

# --- General SWIG configuration

set_source_files_properties(languages/swig/xpm.i PROPERTIES CPLUSPLUS ON)

# --- Python support


if (true)
    FIND_PACKAGE(PythonLibs 3 REQUIRED)
    set(CMAKE_SWIG_OUTDIR "${CMAKE_CURRENT_BINARY_DIR}/python/experimaestro")
	if(NOT PYTHON_INSTALL_LOCATION)
		set(PYTHON_INSTALL_LOCATION ${CMAKE_INSTALL_PREFIX}/python/experimaestro)
	else()
		set(PYTHON_INSTALL_LOCATION ${PYTHON_INSTALL_LOCATION}/experimaestro)
	endif()
    file(MAKE_DIRECTORY ${CMAKE_SWIG_OUTDIR})
    message(STATUS "Python library will be built in ${CMAKE_SWIG_OUTDIR} and installed in ${PYTHON_INSTALL_LOCATION}."
        " Use PYTHON_INSTALL_LOCATION configuration to change this setting."
        " Note: install prefix was ${CMAKE_INSTALL_PREFIX}")

    include_directories(${PYTHON_INCLUDE_PATH} ${OPENSSL_INCLUDE_DIR})

    set(CMAKE_SWIG_FLAGS "-builtin;-threads")

    # Build module experimaestro
    set(SWIG_MODULE_experimaestro_python_EXTRA_DEPS ${PUBLIC_HEADERS} languages/python/xpm.i ${CMAKE_CURRENT_BINARY_DIR}/documentation.i)
    if (${CMAKE_VERSION} VERSION_LESS "3.8.0")
        swig_add_module(experimaestro_python python languages/swig/xpm.i)
    else()
        swig_add_library(experimaestro_python LANGUAGE python SOURCES languages/swig/xpm.i)
    endif()

    # Determine appropriate loader path
    if(APPLE)
        set(XPM_LOADER_PATH @loader_path)
    else()
        set(XPM_LOADER_PATH "$ORIGIN")
    endif()

    # Set output name and path
    foreach(swigtargetlist "experimaestro;experimaestro")
        # set(swigtargetlist ${swigtarget})
        list(GET swigtargetlist 0 swigtargetname)
        list(GET swigtargetlist 1 swigpythonname)
        message(STATUS "${swigtargetname} and ${swigpythonname}")
        set_target_properties("_${swigtargetname}_python" PROPERTIES
                LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SWIG_OUTDIR}"
                LIBRARY_OUTPUT_NAME "_${swigpythonname}"
				MACOSX_RPATH TRUE
				INSTALL_RPATH "${XPM_LOADER_PATH}"
                CXX_STANDARD 17
        )
        target_compile_definitions("_${swigtargetname}_python" PUBLIC "SWIG_DIRECTOR_NO_UEH")
        target_compile_options("_${swigtargetname}_python" PUBLIC "-Wno-unused-label" "-Wno-dynamic-class-memaccess")
        swig_link_libraries("${swigtargetname}_python" ${PYTHON_LIBRARIES} experimaestro_shared)

		install(FILES "${CMAKE_SWIG_OUTDIR}/${swigpythonname}.py" COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
        install(TARGETS "_${swigtargetname}_python" COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
        install(TARGETS yaml-cpp-targets COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})

    endforeach()
    configure_file(languages/python/__init__.py ${CMAKE_SWIG_OUTDIR}/__init__.py COPYONLY)
	install(FILES languages/python/__init__.py COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
	install(TARGETS experimaestro_shared COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
    add_dependencies(_experimaestro_python documentation)

    # Test examples (compilation only)
    #    add_dependencies(run-tests DEPENDS _experimaestro_python)

#    add_test(python_test ${PYTHON_EXECUTABLE} "${CMAKE_CURRENT_SOURCE_DIR}/test/python/test.py")
#    set_tests_properties(python_test PROPERTIES ENVIRONMENT "PYTHONPATH=${CMAKE_SWIG_OUTDIR}")

	# Add install target
	add_custom_target(install-experimaestro-python
	  DEPENDS _experimaestro_python
	  COMMAND "${CMAKE_COMMAND}" -DCMAKE_INSTALL_COMPONENT=python -P "${CMAKE_BINARY_DIR}/cmake_install.cmake"
  	)

endif ()


# --- Java support

# FIND_PACKAGE(JNI REQUIRED)

# set(JAVA_XPM_OUTDIR "${CMAKE_CURRENT_BINARY_DIR}/src/main/java/net/bpiwowar")
# set(CMAKE_SWIG_FLAGS -c++ -package net.bpiwowar)
# set(CMAKE_SWIG_OUTDIR "${JAVA_XPM_OUTDIR}")

# set(SWIG_MODULE_experimaestro_java_EXTRA_DEPS ${PUBLIC_HEADERS} swig/ignores.i ${CMAKE_CURRENT_BINARY_DIR}/documentation.i)
# swig_add_module(experimaestro_java java swig/xpm.i)

# foreach(swigtargetlist "experimaestro;experimaestro" "experimaestro_rpc;rpc")
#     # set(swigtargetlist ${swigtarget})
#     list(GET swigtargetlist 0 swigtargetname)
#     list(GET swigtargetlist 1 swigpythonname)
#     set_target_properties("${swigtargetname}_java" PROPERTIES
# 			MACOSX_RPATH TRUE
# 			INSTALL_RPATH "@loader_path"
# 			CXX_STANDARD 11
#     )
#     target_include_directories("${swigtargetname}_java" PUBLIC ${JNI_INCLUDE_DIRS})
#     swig_link_libraries("${swigtargetname}_java" ${JAVA_JVM_LIBRARY} experimaestro_shared)
# endforeach()

# add_dependencies(experimaestro_java documentation experimaestro_rpc_java)


export(PACKAGE experimaestro)
export(TARGETS experimaestro experimaestro_shared FILE ${CMAKE_CURRENT_BINARY_DIR}/experimaestro.cmake EXPORT_LINK_INTERFACE_LIBRARIES)
