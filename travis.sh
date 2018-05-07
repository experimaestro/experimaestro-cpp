#!/bin/sh

# (c) B. Piwowarski, 2017
# Runs the test suite

# Fail on any error
set -e

log() {
    echo "$@" 1>&2
}

log "Build and test the experimaestro"
log "cmake version $(cmake --version)"

mkdir build
cd build || exit 1

log "--- Configuring"
CXX=g++-7 CC=gcc-7 cmake -C ..

log "--- Building"
make

log "--- Running tests"
make experimaestro-tests
