# lein-pprint

Pretty-print a representation of the project map.

This is a sample of how a simple plugin would work.

## Usage

Add `[lein-pprint "1.1.1"]` to `:plugins`.

    $ lein pprint

```clj
{:compile-path "/home/phil/src/leiningen/lein-pprint/classes",
 :group "lein-pprint",
 :source-path ("/home/phil/src/leiningen/lein-pprint/src"),
 :dependencies nil,
 :target-path "/home/phil/src/leiningen/lein-pprint/target",
 :name "lein-pprint",
 :root "/home/phil/src/leiningen/lein-pprint",
 :version "1.0.0",
 :jar-exclusions [#"^\."],
 :test-path ("/home/phil/src/leiningen/lein-pprint/test"),
 :repositories
 (["central" {:url "https://repo1.maven.org/maven2"}]
  ["clojars" {:url "http://clojars.org/repo/"}]),
 :uberjar-exclusions [#"^META-INF/DUMMY.SF"],
 :eval-in :leiningen,
 :plugins [[lein-swank "1.4.0-SNAPSHOT"]],
 :resources-path
 ("/home/phil/src/leiningen/lein-pprint/dev-resources"
  "/home/phil/src/leiningen/lein-pprint/resources"),
 :native-path "/home/phil/src/leiningen/lein-pprint/native",
 :description "Pretty-print a representation of the project map."}
```

## License

Copyright © 2012 Phil Hagelberg

Distributed under the Eclipse Public License, the same as Clojure.
