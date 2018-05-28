FROM debian:jessie

# Install packages (SSH)
RUN apt-get update && apt-get install -yq --no-install-recommends mysql-client debconf-utils openssh-server oar-server oar-server-mysql oar-user oar-user-mysql oar-node && apt-get -y clean

# Allow servers to start
RUN sed -i "s/^exit 101$/exit 0/" /usr/sbin/policy-rc.d

# Allow SSH to work
RUN sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd

# Create a user with a key
RUN useradd -ms /bin/bash testuser
# RUN mkdir /home/testuser/.ssh
# COPY id_rsa.pub /home/testuser/.ssh/authorized_keys
# RUN chown -R testuser.testuser /home/testuser && chmod og-rwx /home/testuser/.ssh

# Configure OAR
RUN sed -i -e 's/^DB_TYPE=.*/DB_TYPE="mysql"/' -e 's/^DB_PORT=.*/DB_PORT="3306"/'  -e 's/^DB_HOSTNAME=.*/DB_HOSTNAME="mysql"/' -e 's/^DB_BASE_PASSWD=.*/DB_BASE_PASSWD="oar"/'  -e 's/^DB_BASE_PASSWD_RO=.*/DB_BASE_PASSWD_RO="oar_ro"/' /etc/oar/oar.conf && sed -i 's/-n "-20"//' /etc/init.d/oar-node

# Copy start files

COPY start-oar-node.sh /root
COPY run.sh /root

# Expose testuser volume
VOLUME ["/home/testuser"]

EXPOSE 22

