include(ExternalProject)
ExternalProject_Add(googletest
        SOURCE_DIR ${CMAKE_SOURCE_DIR}/googletest/googletest
        EXCLUDE_FROM_ALL 1
)


link_directories(${CMAKE_BINARY_DIR}/googletest/googletest)
add_executable(experimaestro-tests tests.cpp digest.cpp)
target_link_libraries(experimaestro-tests experimaestro_shared gtest ${LINKED_LIBRARIES}) 
target_include_directories(experimaestro-tests PUBLIC ${CMAKE_SOURCE_DIR}/googletest/googletest/include)
add_dependencies(experimaestro-tests googletest)
set_property(TARGET experimaestro-tests PROPERTY CXX_STANDARD 11)
