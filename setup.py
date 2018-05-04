from distutils.core import setup
from distutils.command.build import build as _build
from distutils.command.install import install as _install
from distutils.command.build_ext import build_ext as _build_ext
from distutils.sysconfig import get_python_lib

import distutils.spawn as ds
import distutils.dir_util as dd

import os.path as op
import sys
import os
import logging

logging.basicConfig(level=logging.DEBUG)
# Read the configuration
import configparser

config = configparser.ConfigParser()
config.read('config.ini')
informations = config["informations"]
author = config["author"]

from cmake_pip.cmake_extension import ExtensionCMake, setup

extensions = [ExtensionCMake(name="experimaestro",
    cmake_file="CMakeLists.txt",
    cmake_target="_experimaestro_python",
    cmake_install_component="python")]

# DEBUG: Compare with another behavior
# from distutils.core import Extension
# extensions.append(Extension('_experimaestro_foo', ['_experimaestro_foo.i'], swig_opts=['-modern', '-I../include']))
# with open("_experimaestro_foo.i", "w") as f:
#     f.write("""%module foo
#
# %{
#     int a() {
#         return 1;
#     }
# %}
#
# int a();
# """)
#

setup(name='experimaestro',
      version=informations["version"],
      description=informations["description"],
      author=author["name"],
      author_email=author["email"],
      url=informations["url"],
      ext_modules=extensions
)
