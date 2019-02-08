# --- Task and types definitions

import logging
from experimaestro import TypeArgument, RegisterTask, setLogLevel, LogLevel_DEBUG, Typename

logging.basicConfig(level=logging.DEBUG, format="[%(asctime)-15s] [%(name)s] [%(levelname)s] %(message)s")
setLogLevel("xpm", LogLevel_DEBUG)

# Namespace
hw = Typename("helloworld")

# Register a class as a task: 
# - There is one experimental parameter (word)
# - the task identifier is hw.say
@TypeArgument("word", type=str, required=True, help="Word to generate")
@RegisterTask(hw.say, prefix_args=["xpm", "--"])
class Say(object):
    def execute(self):
        print(self.word.upper(),)

# Definition of the "concat" task
@TypeArgument("strings", type=ArrayOf(Say))
@RegisterTask(hw.concat, prefix_args=["xpm", "--"])
class Concat(object):
    def execute(self):
        # We access the file where standard output was stored
        s = ""
        for string in self.strings:
            with open(string._stdout()) as fp:
                s += " " + fp.read().strip()
        print(s)


# --- Defines the experiment
def xp(args):
    # Sets the working directory and the name of the xp
    experiment(args.workdir, "helloworld")

    # Submit the tasks
    hello = Say(word="hello").submit()
    world = Say(word="world").submit()

    # Concat will depend on the two first tasks
    Concat(strings=[hello, world]).submit()


# --- Parse the command line

import argparse

parser = argparse.ArgumentParser()

subparsers = parser.add_subparsers()

xpm_parser = subparsers.add_parser("xpm")
xpm_parser.add_argument("args", nargs="*")
xpm_parser.set_defaults(func=(lambda args: register.parse(args.args)))

xp_parser = subparsers.add_parser("xp")
xp_parser.add_argument("workdir", type=str, help="Working directory")
xp_parser.set_defaults(func=xp)

args = parser.parse_args()
args.func(args)

