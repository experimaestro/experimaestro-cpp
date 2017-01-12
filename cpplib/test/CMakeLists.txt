include(ExternalProject)
ExternalProject_Add(googletest
        SOURCE_DIR ${CMAKE_SOURCE_DIR}/ext/googletest/googletest
        EXCLUDE_FROM_ALL 1
)


link_directories(${CMAKE_BINARY_DIR}/ext/googletest/googletest)
add_executable(experimaestro-tests tests.cpp digest.cpp structuredvalues.cpp cppdefinitions.cpp)
target_link_libraries(experimaestro-tests experimaestro_shared gtest ${LINKED_LIBRARIES}) 
target_include_directories(experimaestro-tests PUBLIC ${CMAKE_SOURCE_DIR}/googletest/googletest/include)
add_dependencies(experimaestro-tests googletest)
set_property(TARGET experimaestro-tests PROPERTY CXX_STANDARD 11)

add_test(cpptests experimaestro-tests)

# Trick to get the target built
add_test(ctest_build_test_code "${CMAKE_COMMAND}" --build ${CMAKE_BINARY_DIR} --target experimaestro-tests)
set_tests_properties(cpptests PROPERTIES DEPENDS ctest_build_test_code)
