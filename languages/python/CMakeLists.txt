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

include_directories(${PYTHON_INCLUDE_PATH} ${OPENSSL_INCLUDE_DIR} ${LIBWEBSOCKETS_INCLUDE_DIRS})


# Build module experimaestro
set(SWIG_MODULE_experimaestro_python_EXTRA_DEPS ${PUBLIC_HEADERS} xpm.i ${CMAKE_CURRENT_BINARY_DIR}/../../documentation.i)
if (${CMAKE_VERSION} VERSION_LESS "3.8.0")
    swig_add_module(experimaestro_python python ../swig/xpm.i)
else()
    swig_add_library(experimaestro_python LANGUAGE python SOURCES ../swig/xpm.i)
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
            CXX_STANDARD 11
    )
    swig_link_libraries("${swigtargetname}_python" ${PYTHON_LIBRARIES} experimaestro_shared)

    install(FILES "${CMAKE_SWIG_OUTDIR}/${swigpythonname}.py" COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
    install(TARGETS "_${swigtargetname}_python" COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
endforeach()
configure_file(__init__.py ${CMAKE_SWIG_OUTDIR}/__init__.py COPYONLY)
install(FILES __init__.py COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
# install(TARGETS experimaestro_shared COMPONENT python DESTINATION ${PYTHON_INSTALL_LOCATION})
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

