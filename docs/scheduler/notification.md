It is possible to notify Experimaestro of the progress of a job. If notification is possible, the
environment variable `XPM_NOTIFICATION_URL` is set by the launcher. It gives the URL that has
to be used to notify that something has happened (for the moment, only a progress
value between 0 and 1 is allowed)

# Bash

With `wget`

```
test "$XPM_NOTIFICATION_URL" && wget --quiet -O /dev/null "$XPM_NOTIFICATION_URL/progress/$progress"
```

# Python

In Python, the simplest is to use the experimaestro library

```
from experimaestro import progress

# Report progress of 10%
progress(.1)
```
