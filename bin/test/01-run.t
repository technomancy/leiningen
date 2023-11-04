#!/usr/bin/env bash

source test/init

out=$(./lein)
has "$out" "Leiningen is a tool for working with Clojure projects." \
    "'lein' prints help message"

out=$(./bump 2>&1 || true)
is "$out" "Usage: bin/bump 2.9.7 2.9.8-SNAPSHOT" \
    "'bump' prints usage message"

done-testing
