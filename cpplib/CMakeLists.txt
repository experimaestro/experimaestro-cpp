cmake_minimum_required(VERSION 3.0.0)

include(UseSWIG)
find_package(SWIG REQUIRED)

FIND_PACKAGE(PythonInterp 3 REQUIRED)
add_definitions("-Wall")
enable_testing()
set(SOURCES xpm/JsonRPCClient.cpp xpm/xpm.cpp xpm/Client.cpp xpm/rpc/objects.cpp)

include_directories("${CMAKE_SOURCE_DIR}" "${CMAKE_SOURCE_DIR}/websocketpp" "${CMAKE_SOURCE_DIR}/asio/include")
add_executable(xpmcpp ${SOURCES})
set_property(TARGET xpmcpp PROPERTY CXX_STANDARD 11)


# --- Generate documentation

find_package(Doxygen REQUIRED)

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/Doxyfile.in ${CMAKE_CURRENT_BINARY_DIR}/Doxyfile @ONLY)

add_custom_command(
        OUTPUT index.xml
        COMMAND ${DOXYGEN_EXECUTABLE} Doxyfile
        COMMAND touch index.xml
        WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
        COMMENT "Generating XML documentation with Doxygen" VERBATIM
        DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/xpm.h ${CMAKE_CURRENT_SOURCE_DIR}/Doxyfile.in
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
    include_directories(${PYTHON_INCLUDE_PATH} ${OPENSSL_INCLUDE_DIR} ${LIBWEBSOCKETS_INCLUDE_DIRS} ${CMAKE_SOURCE_DIR})

    SET_SOURCE_FILES_PROPERTIES(swig/xpm.i PROPERTIES CPLUSPLUS ON)

    set(CMAKE_SWIG_OUTDIR "python")
    set(CMAKE_SWIG_FLAGS "-builtin")

    SET(SWIG_MODULE_xpm_python_EXTRA_DEPS ${CMAKE_SOURCE_DIR}/xpm.h ${CMAKE_SOURCE_DIR}/swig/xpm.swig.py)
    swig_add_module(xpm_python python swig/xpm.i ${SOURCES})
    swig_link_libraries(xpm_python ${PYTHON_LIBRARIES} ${OPENSSL_LIBRARIES} ${LIBWEBSOCKETS_LIBRARIES})

    # C++11 features
    set_target_properties(_xpm_python PROPERTIES CXX_STANDARD 11)

    # Set output name and path
    SET_TARGET_PROPERTIES(_xpm_python PROPERTIES
            LIBRARY_OUTPUT_DIRECTORY "${CMAKE_SWIG_OUTDIR}"
            LIBRARY_OUTPUT_NAME _xpm)

    add_dependencies(_xpm_python documentation)

    # Test examples (compilation only)
    #    add_dependencies(run-tests DEPENDS _xpm_python)

    add_test(python_test ${PYTHON_EXECUTABLE} "${CMAKE_CURRENT_SOURCE_DIR}/test/python/test.py")
    set_tests_properties(python_test PROPERTIES ENVIRONMENT "PYTHONPATH=${CMAKE_SWIG_OUTDIR}")

endif ()
