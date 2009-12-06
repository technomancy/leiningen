# Leiningen Swank Plugin

This plugin lets you launch a swank server via Leiningen. Simply add
[leiningen/lein-swank "1.0.0"] to your :dev-dependencies in
project.clj and run "lein swank" to start the server. Then from Emacs
run M-x slime-connect to connect to your project.

You will need slime and slime-repl (but not swank-clojure) installed
[from ELPA](http://tromey.com/elpa) for this to work.
