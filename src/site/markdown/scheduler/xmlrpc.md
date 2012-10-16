<head><title>XML-RPC</title></head>

# Commands

All the commands must be prefixed by `Server.`. For instance, `shutdown` would be called with `Server.shutdown`.

## Stopping

`shutdown` can be used to stop the Experimaestro server.

## Scheduling jobs

`runCommands` adds a new command line execution to the scheduler.

`updateJobs` can be used to force updating the statuses of jobs. This is useful when, for whatever reason, Experimaestro has not detected that a job finished or that its dependencies where satisfied.

## Running scripts

`runJSScript` can be used to run one or more JavaScript file(s).
