# Roadmap

## Serverless experimaestro

The goal is to be able to launch experiments without running a server first.

### Short term: basic machinery on localhost

- [x] Implement process builder (`ProcessBuilder`, `LocalhostProcessBuilder`)
- [x] Implement connectors (`Connector`, `LocalhostConnector`)
- [x] Implement script builders (`ScriptBuilder`, `ShScriptBuilder`)
- [x] Implement launchers (`Launcher`, `LocalhostLauncher`)
- [x] Implement scheduler (`Scheduler`): just monitors
- [ ] Implement database to store information
- [x] Add tags

### Medium term: external

- [x] SSH file system (`SSHConnector`, `SSHProcessBuilder`)
- [ ] OAR launcher (OARLauncher)
- [ ] Basic RPC server for global settings (e.g. token)
- [ ] Implement basic tools to manage
