from distutils.core import setup
import distutils.command.build as _build
from distutils.command.build_py import build_py as _build_py
from distutils.sysconfig import get_python_lib

import setuptools.command.install as _install
import distutils.spawn as ds
import distutils.dir_util as dd

import os.path as op
import sys
import os

# Read the configuration
import configparser

config = configparser.ConfigParser()
config.read('config.ini')
informations = config["informations"]
author = config["author"]

build_dir = op.join(op.split(__file__)[0],'build')

def run_cmake():
    """
    Runs CMake to determine configuration for this build
    """
    if ds.find_executable('cmake') is None:
        print("CMake  is required to build xpm library")
        print("Please install cmake version >= 3.0 and re-run setup")
        sys.exit(-1)


    
    print("Configuring xpm build with CMake [directory %s].... " % build_dir)
    dd.mkpath(build_dir)
    os.chdir(build_dir)

    # construct argument string
    # FIXME!!!
    cmake_args = ["-DCMAKE_OSX_SYSROOT=/Applications/Xcode.app/Contents//Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.12.sdk"]
    try:
        # ds.spawn(["env"])
        ds.spawn(['cmake','../'] + cmake_args)
        ds.spawn(['make', '_xpm_python'])
    except ds.DistutilsExecError:
        print("Error while running cmake")
        print("run 'setup.py build --help' for build options")
        print("You may also try editing the settings in CMakeLists.txt file and re-running setup")
        sys.exit(-1)

class  build(_build_py):
    def initialize_options(self):
        _build_py.initialize_options(self)

    def run(self):
        if not self.dry_run:
            cwd = os.getcwd()
            run_cmake()
            os.chdir(cwd)
            _build.build.run(self)


setup(name='xpm',
      version=informations["version"],
      description=informations["description"],
      author=author["name"],
      author_email=author["email"],
      url=informations["url"],
      
      data_files = [(get_python_lib(), [
          op.join(build_dir, 'python', '_xpm.so'), 
          op.join(build_dir, 'python', 'xpm.py')
      ])],
      zip_safe = False,

      cmdclass={'build_py': build}
)
