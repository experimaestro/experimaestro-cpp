FROM debian:stretch

# Copy apt-conf
COPY sources.list /etc/apt/sources.list

# Install packages (SSH)
RUN apt-get update  \
 && apt-get -y -t stretch-backports install cmake libssh-dev \
 && apt-get install -y perl libpoco-dev git g++8 swig ninja-build doxygen python3-dev pkg-config \
 && apt-get -y clean
