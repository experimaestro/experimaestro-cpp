from distutils import log


def _utils_copy_left_to_right(root_left, left, root_right, right):
    """Simple utility function for copying files from a left directory to a right directory. The copy
       does not overwrite files when the corresponding file has the same time stamp. The files on the right
       are removed accordingly to reflect any change on the left.

       :param (string) root_left: root location of the files in the left.
       :param (list) left: list of file names, relative to `root_left`
       :param (string) root_right: root location of the files in the right.
       :param (list) right: list of file names, relative to `root_right`
       :returns: None
    """

    import os
    import shutil
    from stat import ST_MTIME

    def keep_file(x, y):
        return os.path.exists(os.path.join(y, x)) and os.path.isfile(os.path.join(y, x))

    # keep only existing ones and files
    set_left = set((i for i in left if keep_file(i, root_left)))
    set_right = set((i for i in right if keep_file(i, root_right)))
    left_only = set_left - set_right
    right_only = set_right - set_left

    # removing right only, checking consistency first
    for f in right_only:
        file_to_remove = os.path.join(root_right, f)
        assert(os.path.exists(file_to_remove))

    nb_removed = 0
    for f in right_only:
        os.remove(os.path.join(root_right, f))
        nb_removed += 1

    # copying left only
    nb_copied = 0
    for f in left_only:
        destination = os.path.join(root_right, f)
        dirname = os.path.dirname(destination)
        if not os.path.exists(dirname):
            os.makedirs(dirname)

        shutil.copyfile(os.path.join(root_left, f), destination)
        nb_copied += 1

    # for the others, check the date
    nb_replaced = 0
    for f in set_left & set_right:
        src = os.path.join(root_left, f)
        dst = os.path.join(root_right, f)

        if os.stat(src)[ST_MTIME] > os.stat(dst)[ST_MTIME]:
            shutil.copyfile(src, dst)
            nb_replaced += 1

    log.warn("[CMAKE-PIP] sync %s -> %s", root_left, root_right)
    log.warn("[CMAKE-PIP] - copied %d / removed (right) %d / replaced %d", nb_copied, nb_removed, nb_replaced)

    pass


def _utils_get_all_files(directory, no_sub_dir=False):
    """Returns all the files contained in a directory, relatively to this directory. Some files
    and extensions are ignored in the list.

    :param (string) directory: the directory that should be parsed
    :param (boolean) no_sub_dir: indicate if the subdirectories should be parsed as well
    :returns: a list of files relative to `directory` (`directory` is not included in the file names)
    :rtype: list
    """
    import os

    files_to_ignore_lower_case = ['.ds_store', '.gitignore']

    def _filter_files(x):
        return (x.lower() not in files_to_ignore_lower_case and
                os.path.splitext(x)[1].lower() != '.bak' and
                x.find('~') == -1)

    file_list = []
    for root, dirlist, filelist in os.walk(directory, True):
        file_list += [os.path.join(root, f) for f in filelist if _filter_files(f)]
        if no_sub_dir:
            break

    file_list = [os.path.abspath(f) for f in file_list]
    file_list = [os.path.relpath(f, os.path.abspath(directory)) for f in file_list]
    return file_list
