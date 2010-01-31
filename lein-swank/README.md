# Leiningen Swank Plugin

This plugin lets you launch a swank server via Leiningen. Simply add
[leiningen/lein-swank "1.1.0"] to your :dev-dependencies in
project.clj and run "lein swank" to start the server. Then from Emacs
run M-x slime-connect to connect to your project.

You will need slime and slime-repl (but not swank-clojure) installed
[from ELPA](http://tromey.com/elpa) for this to work.

Due to a bug in contrib's build, there may be problems using it along
with projects that use Clojure 1.0.
