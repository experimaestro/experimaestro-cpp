#!/usr/bin/python

import xmlrpclib
import sys
import os

sys.argv.pop(0)

configfile=os.path.expanduser("~/.experimaestro")

while len(sys.argv) > 0:
   if sys.argv[0] == "--config": 
	configfile=sys.argv.pop(1)
   else: break

   syg.argv.pop(0)

if len(sys.argv) < 2: 
   print "Expects at least one ID and one command line argument"
   sys.exit(1)

# Initialise the server
xmlrpc = xmlrpclib.ServerProxy("http://localhost:8080/xmlrpc/")

# Get the ID
id=sys.argv.pop(0)

env=[]
for key,value in os.environ.iteritems():
   env.append([key,value])

# Get the command line
if xmlrpc.TaskManager.runCommand(id, 0, sys.argv, env, os.getcwd(), [], [], []):
	sys.exit(0)
else:
	sys.exit(1)
