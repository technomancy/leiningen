#!/bin/bash

export LEIN_VERSION="1.7.1"

case $LEIN_VERSION in
    *SNAPSHOT) SNAPSHOT="YES" ;;
    *) SNAPSHOT="NO" ;;
esac

# Make sure classpath is in unix format for manipulating, then put
# it back to windows format when we use it
if [ "$OSTYPE" = "cygwin" ] && [ "$CLASSPATH" != "" ]; then
    CLASSPATH=`cygpath -up $CLASSPATH`
fi

if [ `whoami` = "root" ] && [ "$LEIN_ROOT" = "" ]; then
    echo "WARNING: You're currently running as root; probably by accident."
    echo "Press control-C to abort or Enter to continue as root."
    echo "Set LEIN_ROOT to disable this warning."
    read _
fi

NOT_FOUND=1
ORIGINAL_PWD="$PWD"
while [ ! -r "$PWD/project.clj" ] && [ "$PWD" != "/" ] && [ $NOT_FOUND -ne 0 ]
do
    cd ..
    if [ "$(dirname "$PWD")" = "/" ]; then
        NOT_FOUND=0
        cd "$ORIGINAL_PWD"
    fi
done

if [ "$LEIN_HOME" = "" ]; then
  if [ -d "$PWD/.lein" ] && [ "$PWD" != "$HOME" ]; then
    echo "Leiningen is running in bundled mode."
    export LEIN_HOME="$PWD/.lein"
  else
    export LEIN_HOME="$HOME/.lein"
  fi
fi

DEV_PLUGINS="$(ls -1 lib/dev/*jar .lein-plugins/*jar 2> /dev/null)"
USER_PLUGINS="$(ls -1 "$LEIN_HOME"/plugins/*jar 2> /dev/null)"

artifact_name () {
    which rev > /dev/null
    if [ $? -eq 0 ]; then
        echo "$1" | sed -e "s/.*\/\(.*\)/\1/" | \
            rev | sed -e "s/raj[-[:digit:].]*-\(.*\)/\1/" | rev
    else
        echo "$1"
    fi
}

unique_user_plugins () {
    saveIFS="$IFS"
    IFS="$(printf '\n\t')"

    plugins="$(echo "$DEV_PLUGINS"; echo "$USER_PLUGINS")"
    artifacts="$(for i in $plugins; do echo "$(artifact_name "$i")"; done)"
    duplicates="$(echo "$artifacts" | sort | uniq -d)"

    if [ -z "$duplicates" ]; then
        echo "$USER_PLUGINS"
    else
        for i in $USER_PLUGINS; do
            artifact="$(artifact_name "$i")"
            if ! echo "$duplicates" | grep -xq "$artifact"; then
                echo "$i"
            fi
        done
    fi
    IFS="$saveIFS"
}

LEIN_PLUGIN_PATH="$(echo "$DEV_PLUGINS" | tr \\n :)"
LEIN_USER_PLUGIN_PATH="$(echo "$(unique_user_plugins)" | tr \\n :)"
CLASSPATH="$CLASSPATH:$LEIN_PLUGIN_PATH:$LEIN_USER_PLUGIN_PATH:test/:src/:resources/"
LEIN_JAR="$LEIN_HOME/self-installs/leiningen-$LEIN_VERSION-standalone.jar"
CLOJURE_JAR="$HOME/.m2/repository/org/clojure/clojure/1.2.1/clojure-1.2.1.jar"
NULL_DEVICE=/dev/null

# apply context specific CLASSPATH entries
if [ -f .lein-classpath ]; then
    CLASSPATH="`cat .lein-classpath`:$CLASSPATH"
fi

# normalize $0 on certain BSDs
if [ "$(dirname "$0")" = "." ]; then
    SCRIPT="$(which $(basename "$0"))"
else
    SCRIPT="$0"
fi

# resolve symlinks to the script itself portably
while [ -h "$SCRIPT" ] ; do
    ls=`ls -ld "$SCRIPT"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        SCRIPT="$link"
    else
        SCRIPT="$(dirname "$SCRIPT"$)/$link"
    fi
done

BIN_DIR="$(dirname "$SCRIPT")"

if [ -r "$BIN_DIR/../src/leiningen/core.clj" ]; then
    # Running from source checkout
    LEIN_DIR="$(dirname "$BIN_DIR")"
    LEIN_LIBS="$(find -H "$LEIN_DIR/lib" -mindepth 1 -maxdepth 1 -print0 2> /dev/null | tr \\0 \:)"
    CLASSPATH="$CLASSPATH:$LEIN_LIBS:$LEIN_DIR/src:$LEIN_DIR/classes:$LEIN_DIR/resources:$LEIN_JAR"

    if [ "$LEIN_LIBS" = "" -a "$1" != "self-install" -a ! -r "$LEIN_JAR" ]; then
        echo "Leiningen is missing its dependencies. Please see \"Building\" in the README."
        exit 1
    fi
else
    # Not running from a checkout
    CLASSPATH="$CLASSPATH:$LEIN_JAR"

    if [ ! -r "$LEIN_JAR" -a "$1" != "self-install" ]; then
        "$0" self-install
    fi
fi

HTTP_CLIENT="wget --no-check-certificate -O"
if type -p curl >/dev/null 2>&1; then
    if [ "$https_proxy" != "" ]; then
        CURL_PROXY="-x $https_proxy"
    fi
    HTTP_CLIENT="curl $CURL_PROXY --insecure -f -L -o"
fi

# If we don't have Leiningen's own Clojure on the bootclasspath, boot
# will be slower, and versions of Clojure in :dev-dependencies can
# take precedence.
if [ ! -r "$CLOJURE_JAR" ]; then
    CLOJURE_JAR_URL=http://build.clojure.org/releases/org/clojure/clojure/1.2.1/clojure-1.2.1.jar
    CLOJURE_HASH="be088d20c078ce48d42afba05984f1ef7c02142b"
    mkdir -p "$(dirname $CLOJURE_JAR)"
    $HTTP_CLIENT $CLOJURE_JAR $CLOJURE_JAR_URL
    if [ "`shasum -a 1 $CLOJURE_JAR | cut -f 1 -d \" \"`" != "$CLOJURE_HASH" ]; then
        echo "WARNING: Clojure jar failed to download from $CLOJURE_JAR_URL"
    fi
fi

export JAVA_CMD=${JAVA_CMD:-"java"}
export LEIN_JAVA_CMD=${LEIN_JAVA_CMD:-$JAVA_CMD}

# Support $JAVA_OPTS for backwards-compatibility.
export JVM_OPTS="${JVM_OPTS:-"$JAVA_OPTS"}"
export LEIN_JVM_OPTS=${LEIN_JVM_OPTS:-$JVM_OPTS}

# TODO: investigate http://skife.org/java/unix/2011/06/20/really_executable_jars.html
# If you're packaging this for a package manager (.deb, homebrew, etc)
# you need to remove the self-install and upgrade functionality.
if [ "$1" = "self-install" ]; then
    if [ -r "$LEIN_JAR" ]; then
      echo "The self-install jar already exists at $LEIN_JAR."
      echo "If you wish to re-download, delete it and rerun \"$0 self-install\"."
      exit 1
    fi
    echo "Downloading Leiningen now..."
    LEIN_DIR=`dirname "$LEIN_JAR"`
    mkdir -p "$LEIN_DIR"
    LEIN_URL="https://github.com/downloads/technomancy/leiningen/leiningen-$LEIN_VERSION-standalone.jar"
    $HTTP_CLIENT "$LEIN_JAR" "$LEIN_URL"
    if [ $? != 0 ]; then
        echo "Failed to download $LEIN_URL"
        if [ $SNAPSHOT = "YES" ]; then
            echo "If you have Maven installed, you can do"
            echo "mvn dependency:copy-dependencies; mv target/dependency lib"
            echo "See README.md for further SNAPSHOT build instructions."
        fi
        rm $LEIN_JAR 2> /dev/null
        exit 1
    fi
elif [ "$1" = "upgrade" ]; then
    if [ "$LEIN_DIR" != "" ]; then
        echo "The upgrade task is not meant to be run from a checkout."
        exit 1
    fi
    if [ $SNAPSHOT = "YES" ]; then
        echo "The upgrade task is only meant for stable releases."
        echo "See the \"Hacking\" section of the README."
        exit 1
    fi
    if [ ! -w "$SCRIPT" ]; then
        echo "You do not have permission to upgrade the installation in $SCRIPT"
        exit 1
    else
        TARGET_VERSION="${2:-"stable"}"
        echo "The script at $SCRIPT will be upgraded to the latest $TARGET_VERSION version."
        echo -n "Do you want to continue [Y/n]? "
        read RESP
        case "$RESP" in
            y|Y|"")
                echo
                echo "Upgrading from $LEIN_VERSION..."
                TARGET="/tmp/lein-$$-upgrade"
                LEIN_SCRIPT_URL="https://github.com/technomancy/leiningen/raw/$TARGET_VERSION/bin/lein"
                $HTTP_CLIENT "$TARGET" "$LEIN_SCRIPT_URL" \
                    && mv "$TARGET" "$SCRIPT" \
                    && chmod +x "$SCRIPT" \
                    && echo && "$SCRIPT" self-install && echo && echo "Now running" `$SCRIPT version`
                exit $?;;
            *)
                echo "Aborted."
                exit 1;;
        esac
    fi
else
    if [ "$OSTYPE" = "cygwin" ]; then
        # When running on Cygwin, use Windows-style paths for java
        CLOJURE_JAR=`cygpath -w "$CLOJURE_JAR"`
        ORIGINAL_PWD=`cygpath -w "$ORIGINAL_PWD"`
        CLASSPATH=`cygpath -wp "$CLASSPATH"`
        NULL_DEVICE=NUL
    fi

    if [ $DEBUG ]; then
        echo $CLASSPATH
        echo $CLOJURE_JAR
    fi

    JLINE=""
    if [ -z $INSIDE_EMACS ] && [ "$TERM" != "dumb" ]; then
        # Use rlwrap if it's available, otherwise fall back to JLine
        RLWRAP=`which rlwrap`
        if [ ! -x "$RLWRAP" ] || [ "$RLWRAP" = "" ]; then
            if [ ! -r "$LEIN_HOME/.jline-warn" ]; then
                echo "Using JLine for console I/O; install rlwrap for optimum experience."
                touch "$LEIN_HOME/.jline-warn"
            fi
            RLWRAP=""
            JLINE=jline.ConsoleRunner
            if [ "$OSTYPE" = "cygwin" ]; then
                JLINE="-Djline.terminal=jline.UnixTerminal jline.ConsoleRunner"
                CYGWIN_JLINE=y
            fi
        else
            # Test to see if rlwrap supports custom quote chars
            rlwrap -m -q '"' echo "hi" > /dev/null 2>&1
            if [ $? -eq 0 ]; then
                RLWRAP="$RLWRAP -r -m -q '\"'"
            fi
            # see if there is a clojure-completion-file
            RLWRAP_CLJ_WORDS_FILE=${RLWRAP_CLJ_WORDS_FILE:-"${HOME}/.clj_completions"}
            RLWRAP_CLJ_WORDS_OPTION=""
            if [ -r "${RLWRAP_CLJ_WORDS_FILE}" ]; then
                RLWRAP_CLJ_WORDS_OPTION="-f ${RLWRAP_CLJ_WORDS_FILE}";
            fi
            RLWRAP="${RLWRAP} $RLWRAP_OPTIONS -b \"(){}[],^%$#@\";:'\" ${RLWRAP_CLJ_WORDS_OPTION}"
        fi
    fi

    test $CYGWIN_JLINE && stty -icanon min 1 -echo

    TRAMPOLINE_FILE="/tmp/lein-trampoline-$$"
    if [ "$OSTYPE" = "cygwin" ]; then
        TRAMPOLINE_FILE=`cygpath -w $TRAMPOLINE_FILE`
    fi

    # The -Xbootclasspath argument is optional here: if the jar
    # doesn't exist everything will still work, it will just have a
    # slower JVM boot.

    ## TODO: -XX:+UseCompressedOops only works on 64 bit jvm
    # is there some way of detecting 64bit at this stage? - tavisrudd
    $RLWRAP $LEIN_JAVA_CMD \
        -Xbootclasspath/a:"$CLOJURE_JAR" \
        -client $LEIN_JVM_OPTS \
        -XX:+TieredCompilation \
        -Dleiningen.original.pwd="$ORIGINAL_PWD" \
        -Dleiningen.trampoline-file=$TRAMPOLINE_FILE \
        -cp "$CLASSPATH" \
        $JLINE clojure.main \
        -e "(use 'leiningen.core)(-main)" \
        $NULL_DEVICE "$@"
    EXIT_CODE=$?

    if [ -r $TRAMPOLINE_FILE ]; then
        TRAMPOLINE="$(cat $TRAMPOLINE_FILE)"
        rm $TRAMPOLINE_FILE
        exec sh -c "exec $TRAMPOLINE"
    else
        test $CYGWIN_JLINE && stty icanon echo
        exit $EXIT_CODE
    fi
fi
