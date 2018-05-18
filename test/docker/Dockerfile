FROM debian:jessie

RUN apt-get update && apt-get install -y openssh-server
RUN useradd -ms /bin/bash testuser
RUN mkdir /home/testuser/.ssh
RUN sed 's@session\s*required\s*pam_loginuid.so@session optional pam_loginuid.so@g' -i /etc/pam.d/sshd
COPY id_rsa id_rsa.pub /home/testuser/.ssh/
RUN chown -R testuser.testuser /home/testuser && chmod og-rwx /home/testuser/.ssh

EXPOSE 22

