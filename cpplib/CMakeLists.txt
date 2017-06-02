cmake_minimum_required(VERSION 3.0.0)
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_CURRENT_SOURCE_DIR}/cmake/")

include(UseSWIG)
find_package(SWIG REQUIRED)
find_package(OpenSSL REQUIRED)
find_package(PythonInterp 3 REQUIRED)
find_package(libssh REQUIRED)
find_package(YamlCPP REQUIRED)

add_definitions("-Wall")
enable_testing()

set(PUBLIC_HEADERS
        include/xpm/utils.hpp
        include/xpm/json.hpp
        include/xpm/xpm.hpp
        include/xpm/context.hpp
        include/xpm/filesystem.hpp
        include/xpm/commandline.hpp
        include/xpm/logging.hpp)

set(RPC_HEADERS
        include/xpm/rpc/optional.hpp
        include/xpm/rpc/objects.hpp
)

set(SOURCES
        ${PUBLIC_HEADERS}
        ${RPC_HEADERS}
        src/rpc/jsonrpcclient.cpp
        src/xpm.cpp
        src/filesystem.cpp
        src/context.cpp
        src/commandline.cpp
        src/private.cpp
        src/rpc/client.cpp
        src/rpc/objects.cpp
        src/rpc/utils.cpp
        src/rpc/notifications.cpp
        src/xpmssh.cpp
        src/xpmssh.hpp
        include/xpm/cpp.cpp include/xpm/cpp.hpp src/task.cpp src/common.cpp src/register.cpp src/jsonserialization.cpp
        src/jsonserialization.hpp
        src/value.cpp)

find_package (Threads)
set(LINKED_LIBRARIES ${OPENSSL_LIBRARIES} ${LIBWEBSOCKETS_LIBRARIES} ${LIBSSH_LIBRARY} ${CMAKE_THREAD_LIBS_INIT} ${LIBYAMLCPP_LIBRARY})
SET(EXT_INSTALL_DIR "${CMAKE_BINARY_DIR}/external/googletest")

include_directories(
        "${CMAKE_CURRENT_SOURCE_DIR}/ext"
        "${CMAKE_CURRENT_SOURCE_DIR}/include"
        "${CMAKE_CURRENT_SOURCE_DIR}/ext/websocketpp"
        "${CMAKE_CURRENT_SOURCE_DIR}/ext/asio/include"
        "${CMAKE_CURRENT_SOURCE_DIR}/ext/spdlog/include"
)

# --- experimaestro library

add_library(experimaestro_shared SHARED ${SOURCES})
install(TARGETS experimaestro_shared LIBRARY DESTINATION lib)
install(DIRECTORY include/xpm DESTINATION include)

foreach(xpmlib experimaestro_shared)
    target_link_libraries(${xpmlib} ${LINKED_LIBRARIES})
    target_include_directories(${xpmlib} PRIVATE ${LIBSSH_INCLUDE_DIR})
    set_target_properties(${xpmlib} PROPERTIES
        CXX_STANDARD 11
        OUTPUT_NAME experimaestro)

endforeach()

# ---- experimaestro executable

add_executable(experimaestro src/experimaestro.cpp)
target_link_libraries(experimaestro experimaestro_shared ${LINKED_LIBRARIES})
set_property(TARGET experimaestro PROPERTY CXX_STANDARD 11)


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

set_source_files_properties(swig/xpm.i swig/xpm_rpc.i PROPERTIES CPLUSPLUS ON)

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
	message(STATUS "Python library will be built in ${CMAKE_SWIG_OUTDIR} and installed in ${PYTHON_INSTALL_LOCATION}")

    include_directories(${PYTHON_INCLUDE_PATH} ${OPENSSL_INCLUDE_DIR} ${LIBWEBSOCKETS_INCLUDE_DIRS})


    set(CMAKE_SWIG_FLAGS "-builtin")

    # Build module experimaestro
    set(SWIG_MODULE_experimaestro_python_EXTRA_DEPS ${PUBLIC_HEADERS} swig/python/xpm.i swig/ignores.i ${CMAKE_CURRENT_BINARY_DIR}/documentation.i)
    swig_add_module(experimaestro_python python swig/xpm.i)

    # Build module experimaestro.rpc
    set(SWIG_MODULE_experimaestro_rpc_python_EXTRA_DEPS ${RPC_HEADERS} swig/ignores.i ${CMAKE_CURRENT_BINARY_DIR}/documentation.i)
    swig_add_module(experimaestro_rpc_python python swig/xpm_rpc.i)

    # Determine appropriate loader path
    if(APPLE)
        set(XPM_LOADER_PATH @loader_path)
    else()
        set(XPM_LOADER_PATH "$ORIGIN")
    endif()

    # Set output name and path
    foreach(swigtargetlist "experimaestro;experimaestro" "experimaestro_rpc;rpc")
        # set(swigtargetlist ${swigtarget})
        list(GET swigtargetlist 0 swigtargetname)
        list(GET swigtargetlist 1 swigpythonname)
        message(STATUS "${swigtargetname} and ${swigpythonname}")
        set_target_properties("_${swigtargetname}_python" PROPERTIES
                LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SWIG_OUTDIR}"
                LIBRARY_OUTPUT_NAME "_${swigpythonname}"
				MACOSX_RPATH TRUE
				INSTALL_RPATH "${XPM_LOADER_PATH}"
				CXX_STANDARD 11
        )
        swig_link_libraries("${swigtargetname}_python" ${PYTHON_LIBRARIES} experimaestro_shared)

		install(FILES "${CMAKE_SWIG_OUTDIR}/${swigpythonname}.py" COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
		install(TARGETS "_${swigtargetname}_python" COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
    endforeach()
	install(FILES swig/python/__init__.py COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
	install(TARGETS experimaestro_shared COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
    add_dependencies(_experimaestro_python documentation _experimaestro_rpc_python)

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

FIND_PACKAGE(JNI REQUIRED)

set(JAVA_XPM_OUTDIR "${CMAKE_CURRENT_BINARY_DIR}/src/main/java/net/bpiwowar")
set(CMAKE_SWIG_FLAGS -c++ -package net.bpiwowar)
set(CMAKE_SWIG_OUTDIR "${JAVA_XPM_OUTDIR}")

set(SWIG_MODULE_experimaestro_java_EXTRA_DEPS ${PUBLIC_HEADERS} swig/ignores.i ${CMAKE_CURRENT_BINARY_DIR}/documentation.i)
swig_add_module(experimaestro_java java swig/xpm.i)

set(SWIG_MODULE_experimaestro_rpc_java_EXTRA_DEPS ${RPC_HEADERS} swig/ignores.i ${CMAKE_CURRENT_BINARY_DIR}/documentation.i)
swig_add_module(experimaestro_rpc_java java swig/xpm_rpc.i)

foreach(swigtargetlist "experimaestro;experimaestro" "experimaestro_rpc;rpc")
    # set(swigtargetlist ${swigtarget})
    list(GET swigtargetlist 0 swigtargetname)
    list(GET swigtargetlist 1 swigpythonname)
    set_target_properties("${swigtargetname}_java" PROPERTIES
			MACOSX_RPATH TRUE
			INSTALL_RPATH "@loader_path"
			CXX_STANDARD 11
    )
    target_include_directories("${swigtargetname}_java" PUBLIC ${JNI_INCLUDE_DIRS})
    swig_link_libraries("${swigtargetname}_java" ${JAVA_JVM_LIBRARY} experimaestro_shared)
endforeach()

add_dependencies(experimaestro_java documentation experimaestro_rpc_java)

export(PACKAGE experimaestro)
export(TARGETS experimaestro experimaestro_shared FILE ${CMAKE_CURRENT_BINARY_DIR}/experimaestro.cmake EXPORT_LINK_INTERFACE_LIBRARIES)
