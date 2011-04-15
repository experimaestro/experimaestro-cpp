#!/usr/bin/python

import xmlrpclib
import sys
import os
import os.path

# --- Process arguments

configfile=os.path.expanduser("~/.experimaestro")

sys.argv.pop(0)
while len(sys.argv) > 0:
   if sys.argv[0] == "--config": 
	configfile=sys.argv.pop(1)
   else: break
   syg.argv.pop(0)

if len(sys.argv) < 1: 
   print "Expects at least one argument"
   sys.exit(1)


def log(message):
   sys.stderr.write(message)
   sys.stderr.write("\n")
   
def getProperties(filename):
   """Get a dictionnary from a property file
   """
   propFile= file(filename, "rU" )

   # Set some default values
   propDict= dict()
   propDict["url"] = "http://localhost:8080"
   
   for propLine in propFile:
      propDef= propLine.strip()
      if len(propDef) == 0:
         continue
      if propDef[0] in ( '!', '#' ):
         continue
      punctuation= [ propDef.find(c) for c in ':= ' ] + [ len(propDef) ]
      found= min( [ pos for pos in punctuation if pos != -1 ] )
      name= propDef[:found].rstrip()
      value= propDef[found:].lstrip(":= ").rstrip()
      propDict[name]= value
   propFile.close()
   return propDict


# Initialise the server
config = getProperties(configfile)
url = config["url"]
#log("Connecting to %s" % url)
xmlrpc = xmlrpclib.ServerProxy(url)


env=[]
for key,value in os.environ.iteritems():
   env.append([key,value])

# Get the command line
for filename in sys.argv:
  filename=os.path.abspath(filename)
  print "### Running JS script %s" % filename
  try:
      r = xmlrpc.TaskManager.runJSScript(True, filename, env)
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
