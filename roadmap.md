# Roadmap

## Serverless experimaestro

The goal is to be able to launch experiments without running a server first.

### Short term: basic machinery on localhost

- [ ] Implement process builder (`ProcessBuilder`, `LocalhostProcessBuilder`)
- [ ] Implement connectors (`Connector`, `LocalhostConnector`)
- [ ] Implement script builders (`ScriptBuilder`, `ShScriptBuilder`)
- [ ] Implement launchers (`Launcher`, `LocalhostLauncher`)
- [ ] Implement scheduler (`Scheduler`): just monitors
    - [ ] Starts a server on a random port (by default, try to use the same one as before)
    - [ ] Have a way to change the way to report notifications (i.e. port change to running apps, via update of a server configuration file `~/.experimaestro/servers/...HASH...`)

### Medium term: external

- [ ] SSH file system (`SSHConnector`, `SSHProcessBuilder`)
- [ ] OAR launcher (OARLauncher)

### Longer term: web services

- [ ] Implement database to store information
- [ ] Implement basic tools to manage
- [ ] Implement basic websocket / web server