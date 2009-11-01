#!/bin/bash

cd test/

LIBS="$(find -H ../lib/ -mindepth 1 -maxdepth 1 -print0 | tr \\0 \:)"

java -cp ../src/:./:$LIBS clojure.main -i test_leiningen.clj -e "(clojure.test/run-tests 'test-leiningen)"