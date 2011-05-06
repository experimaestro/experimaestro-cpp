#!/usr/bin/python

import xmlrpclib
import sys
import os
import os.path
import xpm

# --- Process arguments


xpm.processArgs(sys.argv)
xmlrpc = xpm.getServer()


if len(sys.argv) < 1: 
   print "Expects at least one argument"
   sys.exit(1)

env=[]
for key,value in os.environ.iteritems():
   env.append([key,value])

# Get the command line
for filename in sys.argv:
  filename=os.path.abspath(filename)
  print "### Running JS script %s" % filename
  try:
      r = xmlrpc.Server.runJSScript(True, filename, env)
      for line in r[2]:
      	print line
      if r[0]:
	 print
	 print "### Error while running %s ###" % filename
         print r[1]
         sys.exit(r[0])
      print
  except xmlrpclib.Fault as e:
  	print e
  	sys.exit(1)
  except Exception as e:
  	print e
  	sys.exit(1)     

sys.exit(0)
