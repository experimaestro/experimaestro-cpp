[![CircleCI](https://circleci.com/gh/experimaestro/experimaestro.svg?style=svg)](https://circleci.com/gh/experimaestro/experimaestro)

Tired to **manually** manage your experiments? Experimaestro manager
gives you a systematic way to write and run experiments where parameters varies.

The overall goals of the experimaestro manager are to:

* Decompose experiments into a set of parameterizable tasks
* Schedule tasks and handle dependencies between tasks
* Avoids to re-run the same task two times by computing unique task IDs dependending on the parameters
* Handle experimental parameters through tags

Experimaestro is in an **beta** state. There is an [associated documentation](http://experimaestro.github.io/experimaestro/).

## Language support

- C/C++ (direct)
- Python: [experimaestro-python](https://github.com/experimaestro/experimaestro-python)