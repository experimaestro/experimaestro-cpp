cmake_minimum_required(VERSION 3.0.0)

include(UseSWIG)
find_package(SWIG REQUIRED)

find_package(OpenSSL REQUIRED)

FIND_PACKAGE(PythonInterp 3 REQUIRED)
add_definitions("-Wall")
enable_testing()

set(EXTERNAL_HEADERS
        include/xpm/xpm.hpp
        include/xpm/rpc/objects.hpp
        include/xpm/context.hpp
        include/xpm/filesystem.hpp
        include/xpm/commandline.hpp)

set(SOURCES
        ${EXTERNAL_HEADERS}
        src/rpc/jsonrpcclient.cpp
        src/xpm.cpp
        src/filesystem.cpp
        src/context.cpp
        src/rpc/client.cpp
        src/rpc/objects.cpp
        src/rpc/utils.cpp
        src/commandline.cpp)

find_package (Threads)
set(LINKED_LIBRARIES ${OPENSSL_LIBRARIES} ${LIBWEBSOCKETS_LIBRARIES} ${CMAKE_THREAD_LIBS_INIT})

add_subdirectory(ext/spdlog EXCLUDE_FROM_ALL)

include_directories("${CMAKE_SOURCE_DIR}/ext" "${CMAKE_SOURCE_DIR}/include" "${CMAKE_SOURCE_DIR}/ext/websocketpp" "${CMAKE_SOURCE_DIR}/ext/asio/include" "${CMAKE_SOURCE_DIR}/ext/spdlog/include")

# --- experimaestro library

add_library(experimaestro_shared SHARED ${SOURCES})
target_link_libraries(experimaestro_shared ${LINKED_LIBRARIES})
set_target_properties(experimaestro_shared PROPERTIES
    CXX_STANDARD 11
    OUTPUT_NAME experimaestro)

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
        DEPENDS ${EXTERNAL_HEADERS} ${CMAKE_CURRENT_SOURCE_DIR}/Doxyfile.in
)
add_custom_target(documentation_index DEPENDS xml/index.xml)

add_custom_command(
        OUTPUT documentation.i
        COMMAND ${PYTHON_EXECUTABLE} ${CMAKE_SOURCE_DIR}/doxy2swig.py
            --function-signature --type-info --overloaded-functions --attribute-list --constructor-list
            ${CMAKE_CURRENT_BINARY_DIR}/xml/index.xml ${CMAKE_CURRENT_BINARY_DIR}/documentation.i
        DEPENDS documentation_index
        COMMENT "Generating Swig documentation" VERBATIM
)

add_custom_target(documentation DEPENDS documentation.i)

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

    set_source_files_properties(swig/xpm.i swig/xpm_rpc.i PROPERTIES CPLUSPLUS ON)

    set(CMAKE_SWIG_FLAGS "-builtin")


    # Build module experimaestro    
    set(SWIG_MODULE_experimaestro_python_EXTRA_DEPS ${EXTERNAL_HEADERS} swig/ignores.i ${CMAKE_CURRENT_BINARY_DIR}/documentation.i)
    swig_add_module(experimaestro_python python swig/xpm.i)

    # Build module experimaestro.rpc 
    set(SWIG_MODULE_experimaestro_rpc_python_EXTRA_DEPS ${EXTERNAL_HEADERS} swig/ignores.i ${CMAKE_CURRENT_BINARY_DIR}/documentation.i)
    swig_add_module(experimaestro_rpc_python python swig/xpm_rpc.i)

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
				INSTALL_RPATH "@loader_path"
				CXX_STANDARD 11)
        swig_link_libraries("${swigtargetname}_python" ${PYTHON_LIBRARIES} experimaestro_shared)

		INSTALL(FILES "${CMAKE_SWIG_OUTDIR}/${swigpythonname}.py" COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
		INSTALL(TARGETS "_${swigtargetname}_python" COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
    endforeach()
	INSTALL(FILES swig/python/__init__.py COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
	INSTALL(TARGETS experimaestro_shared COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
    add_dependencies(_experimaestro_python documentation _experimaestro_rpc_python)

    # Test examples (compilation only)
    #    add_dependencies(run-tests DEPENDS _experimaestro_python)

    add_test(python_test ${PYTHON_EXECUTABLE} "${CMAKE_CURRENT_SOURCE_DIR}/test/python/test.py")
    set_tests_properties(python_test PROPERTIES ENVIRONMENT "PYTHONPATH=${CMAKE_SWIG_OUTDIR}")
	
	# Add install target
	add_custom_target(install-python
	  DEPENDS _experimaestro_python
	  COMMAND "${CMAKE_COMMAND}" -DCMAKE_INSTALL_COMPONENT=python -P "${CMAKE_BINARY_DIR}/cmake_install.cmake"
  	)
	
endif ()
