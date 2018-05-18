from experimaestro import *

# An experimental parameter
@TypeArgument("word", type=str, required=True, help="Word to generate")

# Register this class as task (by default, with the same name as the type)
@RegisterTask()

# Register this class and gives it the type "Hello"
@RegisterType("say")
class Say(object):
    def execute(self):
        print(self.word)

@TypeArgument("first", type=Say)
@TypeArgument("second", type=Say)
@RegisterTask()
@RegisterType("concat")
class Concat(object):
    def execute(self):
        print(self.name)

if sys.argv[1] == "run":
    # (1) Runs a specific task 
    register.parse()
else:
    # (2) Configures the experiment

    # Get the parameters
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("workdir", type=str, help="Working directory")
    args = parser.parse_args()

    # Sets the working directory and the name of the experiment
    experiment(args.workdir, "helloworld")

    # Submit the tasks
    hello = Say(word="hello").submit()
    world = Say(word="world").submit()

    # Concat will depend on the two first tasks
    Concat(first=hello, second=world).submit()
