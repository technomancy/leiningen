# Leiningen

    "Leiningen!" he shouted. "You're insane! They're not creatures you can
    fight--they're an elemental--an 'act of God!' Ten miles long, two
    miles wide--ants, nothing but ants! And every single one of them a
    fiend from hell...
    -- from Leiningen Versus the Ants by Carl Stephenson

Leiningen is a build tool for Clojure designed to not set your hair on fire.

## Usage

    $ lein deps # install dependencies in lib/

    $ lein test [PRED] # run the project's tests, optionally filtered on PRED

    $ lein compile # ahead-of-time compile into classes/

    $ lein repl # launch a REPL with the project classpath configured

    $ lein jar # create a jar of the compiled project

    $ lein uberjar # create standalone jar that bundles dependencies too

## Configuration

Place a build.clj file in the project root that looks something like this: 

    (defproject leiningen
      :version "1.0-SNAPSHOT"
      :dependencies [["org.clojure" "clojure" "1.1.0-alpha-SNAPSHOT"]
                     ["org.clojure" "clojure-contrib" "1.0-SNAPSHOT"]
                     ["org.clojure" "lancet" "1.0-SNAPSHOT"]
                     ["org.apache.ant" "ant" "1.7.1"]
                     ["org.apache.ant" "ant-launcher" "1.7.1"]
                     ["org.apache.maven" "maven-ant-tasks" "2.0.10"]])

## License

Copyright (C) 2009 Phil Hagelberg

Distributed under the Eclipse Public License, the same as Clojure
uses. See the file COPYING.
