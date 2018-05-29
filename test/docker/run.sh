#!/bin/sh

# # Wait for MySQL to be up
# export PGPASSWORD=password
# while ! psql  -h postgres -U admin  -c "select 'It is running'" | grep -q "It is running"; do sleep 1; done

# # Create OAR database
# oar-database --create  --db-host=postgres --db-user=oar --db-pass=oar --db-ro-user=oar_ro --db-ro-pass=oar_ro --db-admin-user=admin --db-admin-pass=password --db-port=5432 --db-type=Pg

# Add node
# service oar-server start
service ssh start

# Add resources
# echo yes  | oar_resources_init  ~oar/oar.hosts
# source /tmp/oar_resources_init.cmd

# And then... wait!
sleep infinity