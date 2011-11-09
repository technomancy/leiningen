#compdef lein

# Lein ZSH completion function
# Drop this somewhere in your $fpath (like /usr/share/zsh/site-functions)
# and rename it _lein

_lein() {
  if (( CURRENT > 2 )); then
    # shift words so _arguments doesn't have to be concerned with second command
    (( CURRENT-- ))
    shift words
    # use _call_function here in case it doesn't exist
    _call_function 1 _lein_${words[1]}
  else
    _values "lein command" \
     "classpath[Print the classpath of the current project.]" \
     "clean[Remove compiled class files and jars from project.]" \
     "compile[Compile Clojure source into \.class files.]" \
     "deploy[Build jar and deploy to remote repository.]" \
     "deps[Download :dependencies and put them in :library-path.]" \
     "help[Display a list of tasks or help for a given task.]" \
     "install[Install current project or download specified project.]" \
     "interactive[Enter an interactive task shell.]" \
     "jack-in[Jack in to a Clojure SLIME session from Emacs.]" \
     "jar[Package up all the project's files into a jar file.]" \
     "javac[Compile Java source files.]" \
     "new[Create a new project skeleton.]" \
     "plugin[Manage user-level plugins.]" \
     "pom[Write a pom.xml file to disk for Maven interop.]" \
     "repl[Start a repl session either with the current project or standalone.]" \
     "retest[Run only the test namespaces which failed last time around.]" \
     "run[Run the project's -main function.]" \
     "search[Search remote repositories.]" \
     "swank[Launch swank server for Emacs to connect.]" \
     "test[Run the project's tests.]" \
     "test![Run a project's tests after cleaning and fetching dependencies.]" \
     "trampoline[Run a task without nesting the project's JVM inside Leiningen's.]" \
     "uberjar[Package up the project files and all dependencies into a jar file.]" \
     "upgrade[Upgrade Leiningen to the latest stable release.]" \
     "version[Print version for Leiningen and the current JVM.]"
  fi
}

_lein_plugin() {
  _values "lein plugin commands" \
    "install[Download, package, and install plugin jarfile into ~/.lein/plugins]" \
    "uninstall[Delete the plugin jarfile: \[GROUP/\]ARTIFACT-ID VERSION]"
}


_lein_namespaces() {
  if [ -d test ]; then
    _values "lein valid namespaces" $(find $1 -type f -name "*.clj" -exec grep -E \
                 '^\(ns[[:space:]]+\w+' '{}' ';' | awk '/\(ns[ ]*([A-Za-z\.]+)/ {print $2}')
  fi 
}

_lein_run() {
  _lein_namespaces "src/"
}

_lein_test() {
  _lein_namespaces "test/"
}

_lein_test!() {
  _lein_namespaces "test/"
}

