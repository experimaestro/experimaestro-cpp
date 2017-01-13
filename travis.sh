#!/bin/sh

# (c) B. Piwowarski, 2017
# Runs the test suite

# Fail on any error
set -e

log() {
    echo "$@" 1>&2
}

log "Build and test the CPP library"
(
    log "cmake version $(cmake --version)"
    cd cpplib
    mkdir build
    (cd build && cmake -C ..)
    make -C experimaestro-tests
)

log "Build and testing the server"
(
    cd server
    gradle build
    gradle test
)
