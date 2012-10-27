#!/bin/bash -x

function crazy_test {
    rm -rf "$1"
    mkdir "$1"
    env HOME="$1" lein version
}

crazy_test "/tmp/test home with spaces/test dir"
crazy_test "/tmp/&crazy #home/test;dir"
