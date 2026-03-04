# SPDX-License-Identifier: BSD-3-Clause
# Copyright (C) 2022 iris-GmbH infrared & intelligent sensors
# This file is based on work which is:
# Copyright 2000-2022 Kitware, Inc. and Contributors

#[=======================================================================[.rst:
FindArgon2
------------

Finds the Argon2 library.

Imported Targets
^^^^^^^^^^^^^^^^

This module provides the following imported targets, if found:

``Argon2::Argon2``

Result Variables
^^^^^^^^^^^^^^^^

This will define the following variables:

``Argon2_FOUND``
  True if the system has the Argon2 library.
``Argon2_INCLUDE_DIRS``
  Include directories needed to use Argon2.
``Argon2_LIBRARIES``
  Libraries needed to link to Argon2.

#]=======================================================================]

# Look for the necessary header
find_path(Argon2_INCLUDE_DIR NAMES argon2.h)
mark_as_advanced(Argon2_INCLUDE_DIR)

# Look for the necessary library
find_library(Argon2_LIBRARY NAMES argon2)
mark_as_advanced(Argon2_LIBRARY)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(Argon2
    REQUIRED_VARS Argon2_INCLUDE_DIR Argon2_LIBRARY
)

# Create the imported target
if(Argon2_FOUND)
    set(Argon2_INCLUDE_DIRS ${Argon2_INCLUDE_DIR})
    set(Argon2_LIBRARIES ${Argon2_LIBRARY})
    if(NOT TARGET Argon2::Argon2)
        add_library(Argon2::Argon2 UNKNOWN IMPORTED)
        set_target_properties(Argon2::Argon2 PROPERTIES
            IMPORTED_LOCATION               "${Argon2_LIBRARY}"
            INTERFACE_INCLUDE_DIRECTORIES   "${Argon2_INCLUDE_DIR}")
    endif()
endif()
