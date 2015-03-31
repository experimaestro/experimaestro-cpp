# -*- encoding: utf-8 -*-
"""Routines to run python code in a background process (aka daemon).

This module provides one main function called daemonize().

This module also provides a basic command line interface as an example
showing how to use it.

Examples:
$ echo 'logger.debug("some debug message")' | python daemonlib.py
$ echo 'import sys; sys.exit(42)' | python daemonlib.py
$ echo 'raise RuntimeError("intentional error")' | python daemonlib.py
$ python daemonlib.py <<EOF
import time
logger.info("start")
for i in range(60):
    logger.info("Counting %d", i)
    time.sleep(1)
logger.info("stop")
EOF

Should work with Python 2 or 3.

Copyright (c) 2015 Nicolas Despres
"""


from __future__ import print_function
import os
import sys
import logging
import logging.handlers
import traceback
import signal
import errno
import resource


def _create_pid_file(pathname, logger):
    logger.debug("creating pid file '%s'...", pathname)
    with open(pathname, "w") as stream:
        stream.write(str(os.getpid()))
    logger.debug("pid file '%s' created", pathname)

def _remove_pid_file(pathname, logger):
    logger.debug("removing pid file: %s...", pathname)
    try:
        os.remove(pathname)
    except OSError as e:
        if e.errno == errno.ENOENT:
            logger.debug("pid file '%s' was already gone",
                         pathname)
        else:
            logger.fatal("cannot remove pid file '%s' - %s",
                         pathname, str(e))
    except Exception as e:
        logger.fatal("unexpected exception when removing "
                     "pid file '%s' - %s: %s",
                     pathname, type(e).__name__, str(e))
    else:
        logger.debug("pid file '%s' removed", pathname)

DEFAULT_MAXFD = 1024

def get_maxfd(default=DEFAULT_MAXFD):
    """Return the maximum number of file descriptors."""
    try:
        maxfd = os.sysconf("SC_OPEN_MAX")
    except (AttributeError, ValueError):
        maxfd = default
    r_maxfd = resource.getrlimit(resource.RLIMIT_NOFILE)[1]
    if r_maxfd != resource.RLIM_INFINITY:
        maxfd = r_maxfd
    return maxfd

def collect_logger_fds(logger):
    """Yield all file descriptors currently opened by *logger*"""
    for handler in logger.root.handlers:
        for attrname in dir(handler):
            attr = getattr(handler, attrname)
            if hasattr(attr, "fileno"):
                try:
                    fileno = attr.fileno()
                except Exception:
                    pass
                else:
                    yield fileno

def daemonize(daemon_func, main_func,
              daemon_cwd="/",
              pid_file=None,
              logger=None,
              log_level=logging.WARNING,
              log_format="%(asctime)s %(levelname)s %(process)s "\
                         "%(module)s:%(lineno)d %(message)s",
              log_date_format='%Y-%m-%dT%H:%M',
              sigterm_callback=None,
              error_exit_code=255, interrupt_exit_code=2,
              umask=0o022):
    """Calling this function make your process a daemon.

    It forks the process and call *daemon_func* in the child process and
    *main_func* in the parent (current process).

    The *daemon_func* is called with the *logger* and should setup a signal
    handler on SIGTERM if *sigterm_callback* is False to be stopped properly.

    If *logger* is None a basic logger will be created using a SysLogHandler.
    If it is a string a FileHandler will be used on the filename contained
    in the string. Otherwise it must be a logger created by the user and in
    such case *log_level*, *log_format* and *log_date_format* are ignored.

    If *sigterm_callback* is None a default signal handler raising
    KeyboardInterrupt is installed.
    if *sigterm_callback* is a callable, that function would be used as
    signal handler.

    Exception raised by *daemon_func* are properly logged. Specially
    uncaught exception (i.e. all sub-classes of Exception), SystemExit
    (hence, it is safe to use sys.exit() in *daemon_func* for early exit)
    and KeyboardInterrupt.

    The *main_func* is called with the PID of the daemon.
    You should write this PID to a file so the daemon is easy to stop by
    typing: kill `cat pid`. Each function must returns an integer which is
    the exit status.
    The daemon will creates its own session, changes its working directory
    to *daemon_cwd* and closes all its open files. For this reason, the
    *logging* module with a SysLogHandler or a FileHandler must be used by
    the daemon to communicate.

    If you want to pass arguments to *daemon_func* or *main_func* uses
    *functools.partial* or make them a functor.

    If you change the default value of *daemon_cwd* (not recommended) makes
    sure the directory inode will never changes for the whole life time of
    the daemon and will not block any partition or file system from being
    unmounted.

    A file containing the PID of the daemon can be created if *pid_file*
    is a file name. This file would be automatically removed when the
    daemon is properly terminated (i.e. not killed).

    This function never returns.

    WARNING: Works only on UNIX platform.
    """
    ### Prevent some errors that may happens in the child process.
    if not isinstance(daemon_cwd, str):
        raise TypeError("daemon_cwd must be a str, not {}"
                        .format(type(daemon_cwd).__name__))
    if not os.path.isdir(daemon_cwd) and not os.path.isabs(daemon_cwd):
        raise ValueError("daemon_cwd is not an absolute directory: {}"
                         .format(daemon_cwd))
    if not callable(daemon_func):
        raise TypeError("daemon_func is not callable")
    if not callable(main_func):
        raise TypeError("main_func is not callable")
    if not isinstance(error_exit_code, int):
        raise TypeError("error_exit_code must be an int, not {}"
                        .format(type(error_exit_code).__name__))
    if not 0 <= error_exit_code < 256:
        raise ValueError("invalid error_exit_code value {} "
                         "(must be between 0 and 256 (exclusive)"
                         .format(error_exit_code))
    if not isinstance(interrupt_exit_code, int):
        raise TypeError("interrupt_exit_code must be an int, not {}"
                        .format(type(interrupt_exit_code).__name__))
    if not 0 <= interrupt_exit_code < 256:
        raise ValueError("invalid interrupt_exit_code value {} "
                         "(must be between 0 and 256 (exclusive)"
                         .format(interrupt_exit_code))
    ### Create the logger
    if logger is None:
        daemon_logger = logging.getLogger("daemon")
        daemon_logger.setLevel(log_level)
        log_handler = logging.handlers.SysLogHandler()
        log_handler.setLevel(log_level)
        log_formatter = logging.Formatter(fmt=log_format,
                                          datefmt=log_date_format)
        log_handler.setFormatter(log_formatter)
        daemon_logger.addHandler(log_handler)
    elif isinstance(logger, str):
        logging.basicConfig(filename=logger,
                            filemode="a",
                            format=log_format,
                            datefmt=log_date_format,
                            level=log_level)
        daemon_logger = logging.getLogger("daemon")
    elif isinstance(logger, logging.Logger):
        daemon_logger = logger
    else:
        raise ValueError("invalid logger value: {!r}".format(logger))
    daemon_logger_fds = set(collect_logger_fds(daemon_logger))
    ### Setup SIGTERM callback
    if sigterm_callback is None:
        def interrupt_on_sigterm(signum, frame):
            raise KeyboardInterrupt
        sighandler = interrupt_on_sigterm
    elif sigterm_callback is False:
        sighandler = None
    elif callable(sigterm_callback):
        sighandler = sigterm_callback
    else:
        raise ValueError("invalid sigterm_callback: {!r}"
                         .format(sigterm_callback))
    ### Daemonize
    # Put the daemon in background
    child_pid = os.fork()
    if child_pid == 0: # child
        exit_code = 0
        try:
            daemon_logger.debug("configuring daemon process")
            # Use absolute path to pid_file since we gonna change the current
            # directory.
            if pid_file is not None:
                pid_file = os.path.abspath(pid_file)
            ### Make sure we won't block any mounted file system or partition.
            os.chdir(daemon_cwd)
            daemon_logger.debug("changed current working directory to: {}"
                                .format(daemon_cwd))
            ### Create a new session and make sure we have no terminal
            os.setsid()
            daemon_logger.debug("new session created")
            ### Close all opened files
            maxfd = get_maxfd()
            daemon_logger.debug("closing all fds up to %d except %r",
                                maxfd, daemon_logger_fds)
            for stream in (sys.stdin, sys.stdout, sys.stderr):
                if stream.fileno() not in daemon_logger_fds:
                    stream.close()
            for fd in range(maxfd):
                if fd in daemon_logger_fds:
                    continue
                try:
                    os.close(fd)
                except OSError:
                    pass
            daemon_logger.debug("closed all fds up to %d except %r",
                                maxfd, daemon_logger_fds)
            # We probably don't want the file mode creation mask inherited from
            # the parent, so we give the child complete control over
            # permissions.
            os.umask(umask)
            ### Install signal handler
            signal.signal(signal.SIGTERM, sighandler)
            daemon_logger.debug("installed SIGTERM handler: %s", sighandler)
            ### Create pid file
            if pid_file is not None:
                _create_pid_file(pid_file, daemon_logger)
            ### Start user's daemon function.
            # We use sys.exit() first so that cleanup handlers are called,
            # and IO's buffers are flushed. (see os._exit() documentation).
            daemon_logger.debug("start user's daemon function")
            sys.exit(daemon_func(daemon_logger))
        except SystemExit as e:
            daemon_logger.fatal("daemon exit with code {}".format(e.code))
            exit_code = e.code
        except KeyboardInterrupt as e:
            daemon_logger.fatal("daemon interrupted!")
            exit_code = interrupt_exit_code
        except Exception as e:
            err = sys.exc_info()
            daemon_logger.fatal("uncaught exception!")
            daemon_logger.fatal("%s: %s", type(e).__name__, str(e))
            for lines in traceback.format_exception(*err):
                for line in lines.splitlines():
                    daemon_logger.fatal(line)
            exit_code = error_exit_code
        finally:
            ### Remove pid file
            if pid_file is not None:
                _remove_pid_file(pid_file, daemon_logger)
            daemon_logger.debug("user's daemon stopped with code: %d",
                                exit_code)
            os._exit(exit_code)
    else: # parent
        sys.exit(main_func(child_pid))

if __name__ == "__main__":
    import functools
    import argparse

    def eval_stream(code_string, pathname, global_scope):
        bytecode = compile(code_string, pathname, 'exec')
        exec(bytecode, global_scope, None)

    def daemon(code_string, pathname, logger):
        scope = locals()
        scope["logger"] = logger
        eval_stream(code_string, pathname, scope)
        return 0

    def main(pid_file, log_file, child_pid):
        print("Daemon running with PID {}.".format(child_pid))
        print("To read the log, use: tail -f {}".format(log_file))
        print("To stop it, use: kill `cat {}`".format(pid_file))
        return 1

    parser = argparse.ArgumentParser(
        description="Example of daemon counting in background",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument(
        "--pid-file",
        action="store",
        default="daemonlib.pid",
        help="Where to write the pid of the daemon.")
    parser.add_argument(
        "--log-file",
        action="store",
        default="daemonlib.log",
        help="Where daemon log information.")
    parser.add_argument(
        "--log-level",
        action="store",
        choices=("DEBUG", "INFO", "WARNING", "ERROR", "FATAL"),
        default="DEBUG",
        help="Set log verbosity level.")
    parser.add_argument(
        "source",
        action="store",
        default='-',
        nargs='?',
        help="Python file containing code to daemonize ('-' for stdin) "\
        "(use 'logger' variable to log messages.")
    opts = parser.parse_args(sys.argv[1:])

    # We have to read the code from stdin before to daemonize because
    # a daemon have all its standard channels closed.
    if opts.source == "-":
        code = sys.stdin.read()
    else:
        with open(opts.source) as stream:
            code = stream.read()
    daemonize(functools.partial(daemon, code, opts.source),
              functools.partial(main, opts.pid_file, opts.log_file),
              pid_file=opts.pid_file,
              logger=opts.log_file,
              log_level=opts.log_level)
    assert False, "should never get here"
