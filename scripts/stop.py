#!/usr/bin/python

import xpm
import xmlrpclib
import sys
import os

xpm.processArgs(sys.argv)
xpm.getServer().Server.shutdown();
