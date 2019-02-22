#!/bin/sh

# Start ssh
echo "Starting SSH..."
service ssh start

# And then... wait!
echo "Now, waiting!"
netstat -tlpn | grep 2200
sleep infinity