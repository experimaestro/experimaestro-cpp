#!/bin/sh

# Wait for MySQL to be up
while ! mysqladmin -h mysql ping 2> /dev/null; do sleep 1; done

# Create OAR database
oar-database --create  --db-host=mysql --db-user=oar --db-pass=oar --db-ro-user=oar_ro --db-ro-pass=oar_ro --db-admin-user=root --db-port=3306 --db-type=mysql

# Add node
service oar-server start
service ssh start

# And then... wait!
sleep infinity