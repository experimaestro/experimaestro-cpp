
from .source_utils import _utils_get_all_files, _utils_copy_left_to_right
from .cmake_extension import ExtensionCMake

from distutils.core import Command
from distutils.command.sdist import sdist as _sdist


class create_package_layout(Command):
    description = "Copy the necessary files from the repository layout for making a proper distribution layout"
    user_options = [
        ('from-original-repository', None, "Set to true for original repository setup")
    ]

    def initialize_options(self):
        pass

    def finalize_options(self):
        pass

    def run(self):
        import os

        dist = self.distribution
        for extension in dist.ext_modules:
            if not isinstance(extension, ExtensionCMake):
                continue

            src_root_location = extension.source_location
            if False:
                pass

            path_to_copy = extension.folders_to_copy

            destination_directory = yayi_src_files_location
            if not os.path.exists(destination_directory):
                os.makedirs(destination_directory)
            original_repository_base_directory = os.path.join(os.path.dirname(__file__), os.path.pardir, os.path.pardir)

            # this is the restriction of the original project repository in order to create a source distribution.
            # this should be edited if something new is added to the general repository layout.
            paths_to_copy = (('.', True),
                             ('cmake', False),
                             ('core', False),
                             ('doc', False),
                             ('coreTests', False),  # remove the data directory here
                             ('python', False),
                             ('plugins/external_libraries', False))

            # check if we are in our repository configuration
            for current_root, nosubdir in paths_to_copy:
                if not os.path.exists(os.path.join(original_repository_base_directory, current_root)):
                    return

            for current_root, nosubdir in paths_to_copy:
                srcdir = os.path.join(original_repository_base_directory, current_root)
                dstdir = os.path.join(destination_directory, current_root)
                left_file_list = _utils_get_all_files(srcdir, nosubdir)
                right_file_list = _utils_get_all_files(dstdir, nosubdir) if os.path.exists(dstdir) else []

                _utils_copy_left_to_right(srcdir, left_file_list, dstdir, right_file_list)


class sdist(_sdist):
    """Modified source distribution that installs create_package_layout as a dependent subcommand"""

    sub_commands = [('create_package_layout', None)] + _sdist.sub_commands

    def get_file_list(self):
        """Extends the file list read from the manifest with the sources of Yayi"""
        import os

        _sdist.get_file_list(self)
        src_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), yayi_src_files_location))
        my_files = _utils_get_all_files(src_dir)
        my_files = [os.path.join(src_dir, x) for x in my_files]
        my_files = [os.path.relpath(x, os.path.abspath(os.path.dirname(__file__))) for x in my_files]
        self.filelist.extend(my_files)

        self.filelist.exclude_pattern('*.so', anchor=False)
        self.filelist.exclude_pattern('*.dylib', anchor=False)
        self.filelist.exclude_pattern('*.dll', anchor=False)
        self.filelist.exclude_pattern('*.lib', anchor=False)

        # anything platform specific that might be an extension
        import imp
        for i in (_[0] for _ in imp.get_suffixes() if _[2] == imp.C_EXTENSION):
            self.filelist.exclude_pattern('*%s' % i, anchor=False)

        return
