FROM debian:jessie

# To configure, use:

# Allow servers to start
RUN sed -i "s/^exit 101$/exit 0/" /usr/sbin/policy-rc.d
RUN apt-get update && apt-get install -yq openssh-server oar-server oar-server-mysql mysql-server && apt-get -y autoclean


RUN useradd -ms /bin/bash testuser

# Create a user and start ssh
RUN mkdir /home/testuser/.ssh
RUN sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd
COPY id_rsa id_rsa.pub /home/testuser/.ssh/
RUN chown -R testuser.testuser /home/testuser && chmod og-rwx /home/testuser/.ssh

EXPOSE 22

