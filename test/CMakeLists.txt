# --- Add google test as external project

include(ExternalProject)

ExternalProject_Add(googletest
        SOURCE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../ext/googletest/googletest
        EXCLUDE_FROM_ALL 1
        BINARY_DIR ${CMAKE_BINARY_DIR}/external/googletest.build
        CMAKE_ARGS "-DCMAKE_INSTALL_PREFIX=${EXT_INSTALL_DIR}"
        INSTALL_COMMAND   ""
        BUILD_BYPRODUCTS 
        <BINARY_DIR>/lib/${CMAKE_FIND_LIBRARY_PREFIXES}gtest.a
)

# Specify include dir
ExternalProject_Get_Property(googletest source_dir)
set(GTEST_INCLUDE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../ext/googletest/googletest/include)

# Library
ExternalProject_Get_Property(googletest binary_dir)
set(GTEST_LIBRARY_PATH ${binary_dir}/lib/${CMAKE_FIND_LIBRARY_PREFIXES}gtest.a)
set(GTEST_LIBRARY gtest)
add_library(${GTEST_LIBRARY} UNKNOWN IMPORTED)
set_property(TARGET ${GTEST_LIBRARY} PROPERTY IMPORTED_LOCATION
                ${GTEST_LIBRARY_PATH} )
add_dependencies(${GTEST_LIBRARY} googletest)



add_executable(experimaestro-tests 
        tests.cpp 
        cppdefinitions.cpp
        digest.cpp 
        value-tests.cpp 
        ssh-tests.cpp
        process-tests.cpp
        scheduler-tests.cpp
)

set(XPM_TEST_SOURCEDIR "${CMAKE_CURRENT_SOURCE_DIR}")
set(XPM_TEST_USERDIR "${CMAKE_SOURCE_DIR}/docker/remote/userdir")
configure_file(config.h.in config.h)

target_link_libraries(experimaestro-tests experimaestro_shared gtest ${LINKED_LIBRARIES})
# add_dependencies(experimaestro-tests gtest)
target_include_directories(experimaestro-tests PUBLIC 
        ${CMAKE_CURRENT_BINARY_DIR}
        ${OPENSSL_INCLUDE_DIR} 
        ${GTEST_INCLUDE_DIR}
)
# add_dependencies(experimaestro-tests)
set_property(TARGET experimaestro-tests PROPERTY CXX_STANDARD 11)

add_test(cpptests experimaestro-tests)

# Trick to get the target built
add_test(ctest_build_test_code "${CMAKE_COMMAND}" --build ${CMAKE_BINARY_DIR} --target experimaestro-tests "--config" "$<CONFIG>")
set_tests_properties(cpptests PROPERTIES DEPENDS ctest_build_test_code)



# --- Experiment

add_executable(experiment experiment.cpp)
target_link_libraries(experiment experimaestro_shared ${LINKED_LIBRARIES})
target_include_directories(experiment PUBLIC 
        ${CMAKE_CURRENT_BINARY_DIR}
        ${OPENSSL_INCLUDE_DIR} 
)
