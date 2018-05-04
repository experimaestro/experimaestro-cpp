# This file is a template for creating an external project


set(external_project_opt)

# set up an external GIT repository
if(NOT "${GIT_REPOSITORY}" STREQUAL "")

  set(external_project_opt ${external_project_opt} 
                           GIT_REPOSITORY "${GIT_REPOSITORY}")
  
  if(NOT "${GIT_TAG}" STREQUAL "")
    set(external_project_opt ${external_project_opt} 
                             GIT_TAG "${GIT_TAG}")
  endif()
endif()

include(ExternalProject)
ExternalProject_Add(
  @PROJECT_NAME@
  ${external_project_opt}
)