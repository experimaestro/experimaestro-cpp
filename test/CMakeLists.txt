include(ExternalProject)


ExternalProject_Add(googletest
        SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../ext/googletest/googletest
        EXCLUDE_FROM_ALL 1
        BINARY_DIR ${CMAKE_BINARY_DIR}/external/googletest.build
        CMAKE_ARGS "-DCMAKE_INSTALL_PREFIX=${EXT_INSTALL_DIR}"
        INSTALL_COMMAND   ""
)


link_directories(${CMAKE_BINARY_DIR}/external/googletest.build)

add_executable(experimaestro-tests 
        tests.cpp 
        cppdefinitions.cpp
        digest.cpp 
        parameters-tests.cpp 
        ssh-tests.cpp
        scheduler-tests.cpp
)

set(XPM_TEST_SOURCEDIR "${CMAKE_CURRENT_SOURCE_DIR}")
set(XPM_TEST_USERDIR "${CMAKE_CURRENT_SOURCE_DIR}/docker/userdir")
configure_file(config.h.in config.h)

target_link_libraries(experimaestro-tests experimaestro_shared gtest ${LINKED_LIBRARIES})
target_include_directories(experimaestro-tests PUBLIC 
        ${CMAKE_CURRENT_BINARY_DIR}
        ${OPENSSL_INCLUDE_DIR} 
        ${CMAKE_SOURCE_DIR}/ext/googletest/googletest/include
)
add_dependencies(experimaestro-tests googletest)
set_property(TARGET experimaestro-tests PROPERTY CXX_STANDARD 11)

add_test(cpptests experimaestro-tests)

# Trick to get the target built
add_test(ctest_build_test_code "${CMAKE_COMMAND}" --build ${CMAKE_BINARY_DIR} --target experimaestro-tests)
set_tests_properties(cpptests PROPERTIES DEPENDS ctest_build_test_code)
