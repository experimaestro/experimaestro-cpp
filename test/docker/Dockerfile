FROM debian:jessie

# Install packages (SSH)
RUN apt-get update && apt-get install -yq --no-install-recommends mysql-client debconf-utils openssh-server && apt-get -y clean

# Allow servers to start
RUN sed -i "s/^exit 101$/exit 0/" /usr/sbin/policy-rc.d

# Allow SSH to work
RUN sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd

# Extra package
RUN apt-get install -yq --no-install-recommends vim less && apt-get -y clean

# Create a user with a key
RUN useradd -ms /bin/bash testuser
# RUN mkdir /home/testuser/.ssh
# COPY id_rsa.pub /home/testuser/.ssh/authorized_keys
# RUN chown -R testuser.testuser /home/testuser && chmod og-rwx /home/testuser/.ssh


# Copy start files

COPY run.sh /root
COPY ssh_defaults /etc/default/ssh

# Expose testuser volume
VOLUME ["/home/testuser"]

# Expose port
EXPOSE 22
