#!/bin/sh

mkdir -p /var/run/sshd

/usr/sbin/sshd -f /etc/oar/sshd_config  -D