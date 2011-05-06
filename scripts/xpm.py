#!/usr/bin/python

# Common methods for scripts
import xmlrpclib
import sys
import os


options = {"config-file":  "~/.experimaestro"}

def processArgs(argv):
	global options
	argv.pop(0)
	while len(argv) > 0:
	   if argv[0] == "--config": options["config-file"] = argv.pop(1)
	   else: break
	   argv.pop(0)
   
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
   
   # Read the properties
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


def getServer():
	# Get the configuration
	configfile=os.path.expanduser(options["config-file"])
	
	# Initialise the server
	config = getProperties(configfile)
	url = config["url"]
	xmlrpc = xmlrpclib.ServerProxy(url)
	return xmlrpc