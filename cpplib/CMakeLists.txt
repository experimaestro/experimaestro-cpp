cmake_minimum_required(VERSION 3.0.0)

include(UseSWIG)
find_package(SWIG REQUIRED)

find_package(OpenSSL REQUIRED)

FIND_PACKAGE(PythonInterp 3 REQUIRED)
add_definitions("-Wall")
enable_testing()

set(EXTERNAL_HEADERS include/xpm/xpm.h)
set(SOURCES src/JsonRPCClient.cpp src/xpm.cpp src/Client.cpp src/rpc/objects.cpp src/rpc/utils.cpp include/Context.cpp include/Context.h)

find_package (Threads)

include_directories("${CMAKE_SOURCE_DIR}" "${CMAKE_SOURCE_DIR}/include" "${CMAKE_SOURCE_DIR}/websocketpp" "${CMAKE_SOURCE_DIR}/asio/include")

# --- experimaestro library

add_library(experimaestro-library STATIC ${SOURCES})
target_link_libraries(experimaestro-library ${LINKED_LIBRARIES})
set_property(TARGET experimaestro-library PROPERTY CXX_STANDARD 11)

# ---- experimaestro executable

set(LINKED_LIBRARIES ${OPENSSL_LIBRARIES} ${LIBWEBSOCKETS_LIBRARIES} ${CMAKE_THREAD_LIBS_INIT})
add_executable(experimaestro src/experimaestro.cpp)
target_link_libraries(experimaestro experimaestro-library ${LINKED_LIBRARIES})
set_property(TARGET experimaestro PROPERTY CXX_STANDARD 11)


# --- Add googletest

add_subdirectory(test)

# --- Generate documentation

find_package(Doxygen REQUIRED)

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/Doxyfile.in ${CMAKE_CURRENT_BINARY_DIR}/Doxyfile @ONLY)

add_custom_command(
        OUTPUT index.xml
        COMMAND ${DOXYGEN_EXECUTABLE} Doxyfile
        COMMAND touch index.xml
        WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
        COMMENT "Generating XML documentation with Doxygen" VERBATIM
        DEPENDS ${EXTERNAL_HEADERS} ${CMAKE_CURRENT_SOURCE_DIR}/Doxyfile.in
)
add_custom_target(documentation_index DEPENDS index.xml)

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
	if (DEFINED PYTHON_BUILD_LOCATION)
	    set(CMAKE_SWIG_OUTDIR "${PYTHON_BUILD_LOCATION}/experimaestro")
	else()
	    set(CMAKE_SWIG_OUTDIR "python/experimaestro")
	endif()
	
	file(GENERATE OUTPUT install_manifest.txt CONTENT 
	"${CMAKE_SWIG_OUTDIR}/_experimaestro.so
		${CMAKE_SWIG_OUTDIR}/experimaestro.py
		${CMAKE_SWIG_OUTDIR}/_init_.py")
	
    file(MAKE_DIRECTORY ${CMAKE_SWIG_OUTDIR})
	message(STATUS "Python library will be built in ${CMAKE_SWIG_OUTDIR}")
	
    include_directories(${PYTHON_INCLUDE_PATH} ${OPENSSL_INCLUDE_DIR} ${LIBWEBSOCKETS_INCLUDE_DIRS})

    SET_SOURCE_FILES_PROPERTIES(swig/xpm.i PROPERTIES CPLUSPLUS ON)

	add_custom_target(copy_init_py
	      ${CMAKE_COMMAND} -E copy_if_different
	      ${CMAKE_SOURCE_DIR}/swig/python/__init__.py ${CMAKE_SWIG_OUTDIR})
	
    set(CMAKE_SWIG_FLAGS "-builtin")

    SET(SWIG_MODULE_experimaestro_python_EXTRA_DEPS ${EXTERNAL_HEADERS})
    swig_add_module(experimaestro_python python swig/xpm.i)
    swig_link_libraries(experimaestro_python ${PYTHON_LIBRARIES} experimaestro-library ${LINKED_LIBRARIES})

    # C++11 features
    set_target_properties(_experimaestro_python PROPERTIES CXX_STANDARD 11)

    # Set output name and path
    SET_TARGET_PROPERTIES(_experimaestro_python PROPERTIES
            LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SWIG_OUTDIR}"
            LIBRARY_OUTPUT_NAME _experimaestro)

    add_dependencies(_experimaestro_python documentation copy_init_py)

    # Test examples (compilation only)
    #    add_dependencies(run-tests DEPENDS _experimaestro_python)

    add_test(python_test ${PYTHON_EXECUTABLE} "${CMAKE_CURRENT_SOURCE_DIR}/test/python/test.py")
    set_tests_properties(python_test PROPERTIES ENVIRONMENT "PYTHONPATH=${CMAKE_SWIG_OUTDIR}")
endif ()
