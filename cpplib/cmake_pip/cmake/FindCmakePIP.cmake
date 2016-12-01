
set(cmakepip_default_component CMAKEPIP)
set(cmakepip_all_components "" CACHE INTERNAL "list all components for the cmakepip package" FORCE)
set(cmakepip_relocation "" CACHE INTERNAL "list all files needing relocation in the cmakepip package" FORCE)

if("${cmakepip_prefix_dir}" STREQUAL "")
  message(FATAL_ERROR "cmakepip_prefix_dir should be set")
endif()

# utility function for adding some entries to the cache wrt. component
function(cmakepip_check_component_is_known component_name)
  string(TOUPPER ${component_name} component_upper)

  # if component is not known
  if(NOT "${component_upper}" IN LIST cmakepip_all_components)
    list(APPEND cmakepip_all_components ${component_upper})
  endif()

  # creates the cache entry
  set(current_cache_entry CMAKEPIP_COMP_${component_upper}_BINARY_EXT)
  if(NOT DEFINED "${current_cache_entry}")
    set(${current_cache_entry} "" CACHE INTERNAL "targets to copy for a specific component" FORCE)
  endif()

  set(current_cache_entry ${current_cache_entry} PARENT_SCOPE)
endfunction()


#.rst:
# .. command:: cmakepip_add_target
#
#   This function adds the specified target as well as all its dependencies to the python packaging system.
#
#   * ``TARGET`` the name of the target to install
#   * ``DESTINATION`` indicates the destination folder, defaults to `${cmakepip_prefix_dir}`
#   * ``CONFIGURATION`` the configuration of the target to install, defaults to `Release`
#   * ``COMPONENT`` the component for the package, defaults to CMAKEPIP
#
#   ::
#
#     cmakepip_add_target(TARGET target_name
#                         CONFIGURATION configuration
#                         DESTINATION destination_folder
#                         COMPONENT component)
#
function(cmakepip_add_target)


  set(options )
  set(oneValueArgs CONFIGURATION TARGET COMPONENT DESTINATION)
  set(multiValueArgs)
  cmake_parse_arguments(_cmakepip_cmd "${options}" "${oneValueArgs}" "${multiValueArgs}" ${ARGN})

  if("${_cmakepip_cmd_TARGET}" STREQUAL "")
    message(FATAL_ERROR "python_distutils_add_target: the TARGET should be specified")
  endif()

  if("${_cmakepip_cmd_COMPONENT}" STREQUAL "")
    set(component ${cmakepip_default_component})
  else()
    set(component ${_cmakepip_cmd_COMPONENT})
  endif()
  message(STATUS "installing ${_cmakepip_cmd_TARGET} into component ${component}")
  string(TOUPPER ${component} component_upper)

  # if component is not known
  cmakepip_check_component_is_known(${component_upper})

  if("${_cmakepip_cmd_CONFIGURATION}" STREQUAL "")
    set(configuration Release)
  else()
    set(configuration ${_cmakepip_cmd_CONFIGURATION})
  endif()

  if("${_cmakepip_cmd_DESTINATION}" STREQUAL "")
    set(destination ${cmakepip_prefix_dir})
  else()
    set(destination ${_cmakepip_cmd_DESTINATION})
  endif()

  # do not install the .lib parts
  if(WIN32)
    set(element_to_install RUNTIME)
  else()
    set(element_to_install LIBRARY)
  endif()

  # parsing the dependencies
  get_target_property(dependent_targets ${_cmakepip_cmd_TARGET} INTERFACE_LINK_LIBRARIES)
  foreach(current_dependent_targets ${dependent_targets})
    if(TARGET ${current_dependent_targets})
      # target known, we copy it
      set(${current_cache_entry} ${${current_cache_entry}} ${current_dependent_targets} CACHE INTERNAL "source file to copy")

      install(
        TARGETS ${current_dependent_targets}
        ${element_to_install}
          DESTINATION ${destination}
          COMPONENT ${component_upper}
          CONFIGURATIONS ${configuration}
      )
    endif()

    # We should also copy things that are not targets (check for thirdparties), but we do not want to copy system libraries.
    # This is performed in another function which does not check the graph of dependencies
  endforeach()

  # copy the main target
  # @todo better if the files here are explicitely stated (rather than a *.so in the manifest)
  set(${current_cache_entry} ${${current_cache_entry}} ${target_name} CACHE INTERNAL "source file to copy")

  install(
    TARGETS ${_cmakepip_cmd_TARGET}
    ${element_to_install}
      DESTINATION ${destination}
      COMPONENT ${component_upper}
      CONFIGURATIONS ${configuration}
  )


endfunction()


#.rst:
# .. command:: cmakepip_add_files
#
#   This function adds the specified files to the python package.
#
#   * ``FILES`` a list of files to install
#   * ``DESTINATION`` indicates the destination folder, defaults to `${cmakepip_prefix_dir}`
#   * ``CONFIGURATION`` the configuration of the target to install, defaults to `Release`
#   * ``INSTALL_DLL`` if set, install the DLLs corresponding to the .lib files as well. Ignored on non WIN32 platforms
#   * ``ONLY_DLL`` if set, install only the DLLs corresponding to the .lib files (.libs are ignored). Ignored on non WIN32 platforms
#
#   ::
#
#     cmakepip_add_files(FILES file1 [file2 ...]
#                        [INSTALL_DLL]
#                        [ONLY_DLL]
#                        CONFIGURATION configuration
#                        DESTINATION destination_folder
#                        COMPONENT component)
#
function(cmakepip_add_files)

  set(options INSTALL_DLL ONLY_DLL)
  set(oneValueArgs CONFIGURATION TARGET COMPONENT DESTINATION)
  set(multiValueArgs FILES)
  cmake_parse_arguments(_local_vars "${options}" "${oneValueArgs}" "${multiValueArgs}" ${ARGN})

  set(_additional_files)

  if(WIN32 AND NOT "${_local_vars_INSTALL_DLL}" STREQUAL "")
    foreach(_local_vars ${_local_vars_FILES})
      get_filename_component(_filepathabs ${_local_vars} ABSOLUTE)
      get_filename_component(_filepath ${_filepathabs} PATH)
      get_filename_component(_filename ${_filepathabs} NAME_WE)
      set(_file_to_install ${_filepath}/${_filename}${CMAKE_SHARED_LIBRARY_SUFFIX})
      if(NOT EXISTS ${_file_to_install})
        message(FATAL_ERROR "cmakepip_add_files: specified file ${_file_to_install} does not EXISTS !!!")
      else()
        set(_additional_files ${_additional_files} ${_file_to_install})
      endif()
    endforeach()


    if(NOT "${_local_vars_ONLY_DLL}" STREQUAL "")
      set(_local_vars_FILES) # empty to have only the .dll part
    endif()

  elseif(APPLE AND NOT "${_local_vars_INSTALL_DLL}" STREQUAL "")
    # binary relocation for MAC
    # check fixbundle instead
    foreach(_local_vars ${_local_vars_FILES})
      get_filename_component(_filename ${_local_vars} NAME)
      set(cmakepip_relocation ${cmakepip_relocation} ${_filename} CACHE INTERNAL "relocation files")
    endforeach()
  endif()

  # all files that need to be installed
  set(_local_vars_FILES ${_local_vars_FILES} ${_additional_files})

  if("${_local_vars_COMPONENT}" STREQUAL "")
    set(component ${cmakepip_default_component})
  else()
    set(component ${_local_vars_COMPONENT})
  endif()
  message(STATUS "installing ${_local_vars_TARGET} into component ${component}")
  string(TOUPPER ${component} component_upper)

  # if component is not known
  cmakepip_check_component_is_known(${component_upper})

  if("${_local_vars_CONFIGURATION}" STREQUAL "")
    set(configuration Release)
  else()
    set(configuration ${_local_vars_CONFIGURATION})
  endif()

  if("${_local_vars_DESTINATION}" STREQUAL "")
    set(destination ${cmakepip_prefix_dir})
  else()
    set(destination ${_local_vars_DESTINATION})
  endif()


  # for the manifest.in file. Check again if sdist appropriate target would do
  foreach(_local_vars ${_local_vars_FILES})
    get_filename_component(_v_file_name ${_local_vars} NAME)
    set(YAYI_ADDITIONAL_INCLUDE_CMDS ${YAYI_ADDITIONAL_INCLUDE_CMDS} "include yayi/bin/${_v_file_name}" CACHE INTERNAL "manifest additional files")
    unset(_v_file_name)
  endforeach()

  # install rule
  install(
      FILES ${_local_vars_FILES}
      DESTINATION ${cmakepip_prefix_dir}
      COMPONENT ${component_upper}
      CONFIGURATIONS ${configuration}
    )

endfunction()



# creates the install rules for the python installation on the system
function(create_python_package_system)
  file(GLOB _v_glob RELATIVE "${YAYI_root_dir}" "${YAYI_PYTHON_PACKAGE_LOCATION}}/yayi/*")

  install(FILES ${_v_glob}
          CONFIGURATIONS Release
          DESTINATION ${YAYI_PYTHON_PACKAGE_INSTALLATION_DIR}/yayi
          COMPONENT python)


endfunction()


# ######################################################
#
# This function gathers all information collected by the build tree to create the appropriate python packaging
macro(create_python_package)

  if(FALSE)
    message(STATUS "[CMAKEPIP] Configuring Python package manifest ${YAYI_ADDITIONAL_INCLUDE_CMDS}")

    # should have been cached
    if(NOT DEFINED PYTHON_MODULES_EXTENSIONS)
      message(FATAL_ERROR "Something wrong in the configuration (PYTHON_MODULES_EXTENSIONS not defined)")
    endif()


    set(YAYI_PYTHON_EXT ${PYTHON_MODULES_EXTENSIONS})
    set(YAYI_PLATFORM_SO_EXT ${CMAKE_SHARED_LIBRARY_SUFFIX})
    set(_var_concat "")
    foreach(c IN LISTS YAYI_ADDITIONAL_INCLUDE_CMDS)
      set(_var_concat "${_var_concat}\n${c}")
    endforeach(c)
    set(YAYI_ADDITIONAL_INCLUDE_CMDS ${_var_concat})
    unset(_var_concat)

    if(EXISTS ${manifest_config_file})
      configure_file(${manifest_config_file} ${manifest_file} @ONLY)
    else()
      message(STATUS "[YAYI][pythonext] not configuring the MANIFEST.in since the template ${manifest_config_file} is not found."
                      "Should be an archive/source distribution, MANIFEST.in already configured?")
    endif()


    # declares the Python packaging target
    set(yayi_python_package_SRC
        ${YAYI_PYTHON_PACKAGE_LOCATION}/setup.py)

    if(EXISTS ${manifest_config_file})
      set(yayi_python_package_SRC
          ${yayi_python_package_SRC}
          ${manifest_config_file}
         )
    endif()
  endif()

  foreach(component_upper IN LIST cmakepip_all_components)

    set(current_cache_entry CMAKEPIP_COMP_${component_upper}_BINARY_EXT)
    set(current_target_name "setup_py_${component_upper}")

    # custom target just for installing the files to the appropriate place
    add_custom_target(
      ${current_target_name}
      COMMENT "Python packaging"
      COMMAND ${CMAKE_COMMAND} -E echo " --+ [YAYI][pythonpackage] Installing the python package component to ${cmakepip_prefix_dir} from ${CMAKE_BINARY_DIR}"
      COMMAND ${CMAKE_COMMAND} -E echo "${CMAKE_COMMAND} -DCOMPONENT=${component_upper} -P ${CMAKE_BINARY_DIR}/cmake_install.cmake"
      COMMAND ${CMAKE_COMMAND} -DCOMPONENT=${component_upper} -P ${CMAKE_BINARY_DIR}/cmake_install.cmake
      # SOURCES ${yayi_python_package_SRC}
    )

    # dependencies of the build target
    list(LENGTH current_cache_entry ext_length)
    if(${ext_length})
      add_dependencies(${current_target_name} ${current_cache_entry})
    endif()


    # command built on the fly for pre/post-building the component installation
    # This is helpfull for creating relocatable binaries/python package, especially on OSX
    set(pre_build_cmd
        COMMAND ${CMAKE_COMMAND} -E echo " --+ [YAYI][pythonpackage] Creating destination directory ${cmakepip_prefix_dir}"
        COMMAND ${CMAKE_COMMAND} -E make_directory ${cmakepip_prefix_dir}
    )

    set(post_build_cmd "")

    # relocation after installation, in place
    list(LENGTH YAYI_PYTHON_BINARY_RELOCATION ext_length)
    if("${ext_length}" AND APPLE)
      foreach(_local_vars IN LISTS YAYI_PYTHON_BINARY_RELOCATION)
        set(post_build_cmd
            ${post_build_cmd}
            COMMAND ${CMAKE_COMMAND} -E echo " --+ [YAYI][pythonpackage] Relocating =file/dependency= ${_local_vars}"
            COMMAND ${PYTHON_EXECUTABLE} ${YAYI_root_dir}/cmake/osx_install_name_tool_utility.py
              --target ${cmakepip_prefix_dir}/${_local_vars}
              --destination_folder ${cmakepip_prefix_dir}
              "boost"
            )
      endforeach()
    endif()

    # relocation of the known targets, after the installation, in place
    list(LENGTH CMAKE_PIP_BINARY_EXT ext_length)
    if("${ext_length}" AND APPLE)
      # those are targets
      foreach(_local_vars IN LISTS CMAKE_PIP_BINARY_EXT)
        set(post_build_cmd
            ${post_build_cmd}
            COMMAND ${CMAKE_COMMAND} -E echo " --+ [YAYI][pythonpackage] Relocating =target= ${_local_vars}"
            COMMAND ${PYTHON_EXECUTABLE} ${YAYI_root_dir}/cmake/osx_install_name_tool_utility.py
              --target ${cmakepip_prefix_dir}/$<TARGET_SONAME_FILE_NAME:${_local_vars}>
              --destination_folder ${cmakepip_prefix_dir}/
              --pattern "boost"
            )
      endforeach()
    endif()

    if(NOT "${pre_build_cmd}" STREQUAL "")
      add_custom_command(
        TARGET PythonPackageSetup
        PRE_BUILD
        ${pre_build_cmd})
    endif()


    if(NOT "${post_build_cmd}" STREQUAL "")
      add_custom_command(
        TARGET PythonPackageSetup
        POST_BUILD
        ${post_build_cmd})
    endif()

  endforeach() # for each components

endmacro(create_python_package)
