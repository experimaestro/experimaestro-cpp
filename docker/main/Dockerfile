FROM debian:buster

# Install packages (SSH)
RUN apt-get update  \
 && apt-get install -y libpoco-dev git g++ cmake libssh-dev ninja-build pkg-config \
 && apt-get -y clean
