#!/usr/bin/env bash

# Ensure this file is executable via `chmod a+x lein`, then place it
# somewhere on your $PATH, like ~/bin. The rest of Leiningen will be
# installed upon first run into the ~/.lein/self-installs directory.

function msg {
    echo "$@" 1>&2
}

export LEIN_VERSION="2.9.8"
# Must be sha256sum, will be replaced by bin/release
export LEIN_CHECKSUM='2a0e9114e0d623c748a9ade5d72b54128b31b5ddb13f51b04c533f104bb0c48d'

case $LEIN_VERSION in
    *SNAPSHOT) SNAPSHOT="YES" ;;
    *) SNAPSHOT="NO" ;;
esac

if [[ "$CLASSPATH" != "" ]]; then
    cat <<-'EOS' 1>&2
	WARNING: You have $CLASSPATH set, probably by accident.
	It is strongly recommended to unset this before proceeding.
	EOS
fi

if [[ "$OSTYPE" == "cygwin" ]] || [[ "$OSTYPE" == "msys" ]]; then
    delimiter=";"
else
    delimiter=":"
fi

if [[ "$OSTYPE" == "cygwin" ]]; then
  cygwin=true
else
  cygwin=false
fi

function command_not_found {
    msg "Leiningen couldn't find $1 in your \$PATH ($PATH), which is required."
    exit 1
}

function make_native_path {
    # ensure we have native paths
    if $cygwin && [[ "$1"  == /* ]]; then
    echo -n "$(cygpath -wp "$1")"
    elif [[ "$OSTYPE" == "msys" && "$1"  == /?/* ]]; then
    echo -n "$(sh -c "(cd $1 2</dev/null && pwd -W) || echo $1 | sed 's/^\\/\([a-z]\)/\\1:/g'")"
    else
    echo -n "$1"
    fi
}

#  usage : add_path PATH_VAR [PATH]...
function add_path {
    local path_var="$1"
    shift
    while [ -n "$1" ];do
        # http://bashify.com/?Useful_Techniques:Indirect_Variables:Indirect_Assignment
        if [[ -z ${!path_var} ]]; then
          export ${path_var}="$(make_native_path "$1")"
        else
          export ${path_var}="${!path_var}${delimiter}$(make_native_path "$1")"
        fi
    shift
    done
}

function download_failed_message {
    cat <<-EOS 1>&2
	Failed to download $1 (exit code $2)
	It's possible your HTTP client's certificate store does not have the
	correct certificate authority needed. This is often caused by an
	out-of-date version of libssl. It's also possible that you're behind a
	firewall and haven't set HTTP_PROXY and HTTPS_PROXY.
	EOS
}

function checksum_failed_message {
    cat <<-EOS 1>&2
	Failed to properly download $1
	The checksum was mismatched. and we could not verify the downloaded
	file. We expected a sha256 of
	$2 and actually had
	$3.
	We used '$SHASUM_CMD' to verify the downloaded file.
	EOS
}

function self_install {
  if [ -r "$LEIN_JAR" ]; then
    cat <<-EOS 1>&2
	The self-install jar already exists at $LEIN_JAR.
	If you wish to re-download, delete it and rerun "$0 self-install".
	EOS
    exit 1
  fi
  msg "Downloading Leiningen to $LEIN_JAR now..."
  mkdir -p "$(dirname "$LEIN_JAR")"
  LEIN_URL="https://github.com/technomancy/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.jar"
  $HTTP_CLIENT "$LEIN_JAR.pending" "$LEIN_URL"
  local exit_code=$?
  if [ $exit_code == 0 ]; then
      printf "$LEIN_CHECKSUM  $LEIN_JAR.pending\n" > "$LEIN_JAR.pending.shasum"
      $SHASUM_CMD -c "$LEIN_JAR.pending.shasum"
      if [ $? == 0 ]; then
        mv -f "$LEIN_JAR.pending" "$LEIN_JAR"
      else
        got_sum="$($SHASUM_CMD "$LEIN_JAR.pending" | cut -f 1 -d ' ')"
        checksum_failed_message "$LEIN_URL" "$LEIN_CHECKSUM" "$got_sum"
        rm "$LEIN_JAR.pending" 2> /dev/null
        exit 1
      fi
  else
      rm "$LEIN_JAR.pending" 2> /dev/null
      download_failed_message "$LEIN_URL" "$exit_code"
      exit 1
  fi
}

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

export LEIN_HOME="${LEIN_HOME:-"$HOME/.lein"}"

for f in "/etc/leinrc" "$LEIN_HOME/leinrc" ".leinrc"; do
  if [ -e "$f" ]; then
    source "$f"
  fi
done

if $cygwin; then
    export LEIN_HOME=$(cygpath -w "$LEIN_HOME")
fi

LEIN_JAR="${LEIN_JAR:-${LEIN_HOME}/self-installs/leiningen-${LEIN_VERSION}-standalone.jar}"

# normalize $0 on certain BSDs
if [ "$(dirname "$0")" = "." ]; then
    SCRIPT="$(which "$(basename "$0")")"
    if [ -z "$SCRIPT" ]; then
        SCRIPT="$0"
    fi
else
    SCRIPT="$0"
fi

# resolve symlinks to the script itself portably
while [ -h "$SCRIPT" ] ; do
    ls=$(ls -ld "$SCRIPT")
    link=$(expr "$ls" : '.*-> \(.*\)$')
    if expr "$link" : '/.*' > /dev/null; then
        SCRIPT="$link"
    else
        SCRIPT="$(dirname "$SCRIPT"$)/$link"
    fi
done

BIN_DIR="$(dirname "$SCRIPT")"

export LEIN_JVM_OPTS="${LEIN_JVM_OPTS-"-XX:+TieredCompilation -XX:TieredStopAtLevel=1"}"

# This needs to be defined before we call HTTP_CLIENT below
if [ "$HTTP_CLIENT" = "" ]; then
    if type -p curl >/dev/null 2>&1; then
        if [ "$https_proxy" != "" ]; then
            CURL_PROXY="-x $https_proxy"
        fi
        HTTP_CLIENT="curl $CURL_PROXY -f -L -o"
    else
        HTTP_CLIENT="wget -O"
    fi
fi

# This needs to be defined before we call SHASUM_CMD below
if [ "$SHASUM_CMD" = "" ]; then
    if type -p sha256sum >/dev/null 2>&1; then
        export SHASUM_CMD="sha256sum"
    elif type -p shasum >/dev/null 2>&1; then
        export SHASUM_CMD="shasum --algorithm 256"
    elif type -p sha256 >/dev/null 2>&1; then
        export SHASUM_CMD="sha256 -q"
    else
        command_not_found sha256sum
    fi
fi

# When :eval-in :classloader we need more memory
grep -E -q '^\s*:eval-in\s+:classloader\s*$' project.clj 2> /dev/null && \
    export LEIN_JVM_OPTS="$LEIN_JVM_OPTS -Xms64m -Xmx512m"

if [ -r "$BIN_DIR/../src/leiningen/version.clj" ]; then
    # Running from source checkout
    LEIN_DIR="$(cd $(dirname "$BIN_DIR");pwd -P)"

    # Need to use lein release to bootstrap the leiningen-core library (for aether)
    if [ ! -r "$LEIN_DIR/leiningen-core/.lein-bootstrap" ]; then
        cat <<-'EOS' 1>&2
	Leiningen is missing its dependencies.
	Please run "lein bootstrap" in the leiningen-core/ directory
	with a stable release of Leiningen. See CONTRIBUTING.md for details.
	EOS
        exit 1
    fi

    # If project.clj for lein or leiningen-core changes, we must recalculate
    LAST_PROJECT_CHECKSUM=$(cat "$LEIN_DIR/.lein-project-checksum" 2> /dev/null)
    PROJECT_CHECKSUM=$(sum "$LEIN_DIR/project.clj" "$LEIN_DIR/leiningen-core/project.clj")
    if [ "$PROJECT_CHECKSUM" != "$LAST_PROJECT_CHECKSUM" ]; then
        if [ -r "$LEIN_DIR/.lein-classpath" ]; then
            rm "$LEIN_DIR/.lein-classpath"
        fi
    fi

    # Use bin/lein to calculate its own classpath.
    if [ ! -r "$LEIN_DIR/.lein-classpath" ] && [ "$1" != "classpath" ]; then
        msg "Recalculating Leiningen's classpath."
        cd "$LEIN_DIR"

        LEIN_NO_USER_PROFILES=1 "$LEIN_DIR/bin/lein" classpath .lein-classpath
        sum "$LEIN_DIR/project.clj" "$LEIN_DIR/leiningen-core/project.clj" > \
            .lein-project-checksum
        cd -
    fi

    mkdir -p "$LEIN_DIR/target/classes"
    export LEIN_JVM_OPTS="$LEIN_JVM_OPTS -Dclojure.compile.path=$LEIN_DIR/target/classes"
    add_path CLASSPATH "$LEIN_DIR/leiningen-core/src/" "$LEIN_DIR/leiningen-core/resources/" \
        "$LEIN_DIR/test:$LEIN_DIR/target/classes" "$LEIN_DIR/src" ":$LEIN_DIR/resources"

    if [ -r "$LEIN_DIR/.lein-classpath" ]; then
        add_path CLASSPATH "$(cat "$LEIN_DIR/.lein-classpath" 2> /dev/null)"
    else
        add_path CLASSPATH "$(cat "$LEIN_DIR/leiningen-core/.lein-bootstrap" 2> /dev/null)"
    fi
else # Not running from a checkout
    add_path CLASSPATH "$LEIN_JAR"

    if [ "$LEIN_USE_BOOTCLASSPATH" != "no" ]; then
        LEIN_JVM_OPTS="-Xbootclasspath/a:$LEIN_JAR $LEIN_JVM_OPTS"
    fi

    if [ ! -r "$LEIN_JAR" -a "$1" != "self-install" ]; then
        self_install
    fi
fi

if [ ! -x "$JAVA_CMD" ] && ! type -f java >/dev/null
then
    msg "Leiningen couldn't find 'java' executable, which is required."
    msg "Please either set JAVA_CMD or put java (>=1.6) in your \$PATH ($PATH)."
    exit 1
fi

export LEIN_JAVA_CMD="${LEIN_JAVA_CMD:-${JAVA_CMD:-java}}"

if [[ -z "${DRIP_INIT+x}" && "$(basename "$LEIN_JAVA_CMD")" == *drip* ]]; then
    export DRIP_INIT="$(printf -- '-e\n(require (quote leiningen.repl))')"
    export DRIP_INIT_CLASS="clojure.main"
fi

# Support $JAVA_OPTS for backwards-compatibility.
export JVM_OPTS="${JVM_OPTS:-"$JAVA_OPTS"}"

# Handle jline issue with cygwin not propagating OSTYPE through java subprocesses: https://github.com/jline/jline2/issues/62
cygterm=false
if $cygwin; then
  case "$TERM" in
    rxvt* | xterm* | vt*) cygterm=true ;;
  esac
fi

if $cygterm; then
  LEIN_JVM_OPTS="$LEIN_JVM_OPTS -Djline.terminal=jline.UnixTerminal"
  stty -icanon min 1 -echo > /dev/null 2>&1
fi

# TODO: investigate http://skife.org/java/unix/2011/06/20/really_executable_jars.html
# If you're packaging this for a package manager (.deb, homebrew, etc)
# you need to remove the self-install and upgrade functionality or see lein-pkg.
if [ "$1" = "self-install" ]; then
    if [ -r "$BIN_DIR/../src/leiningen/version.clj" ]; then
        cat <<-'EOS' 1>&2
	Running self-install from a checkout is not supported.
	See CONTRIBUTING.md for SNAPSHOT-specific build instructions.
	EOS
        exit 1
    fi
    msg "Manual self-install is deprecated; it will run automatically when necessary."
    self_install
elif [ "$1" = "upgrade" ] || [ "$1" = "downgrade" ]; then
    if [ "$LEIN_DIR" != "" ]; then
        msg "The upgrade task is not meant to be run from a checkout."
        exit 1
    fi
    if [ $SNAPSHOT = "YES" ]; then
        cat <<-'EOS' 1>&2
	The upgrade task is only meant for stable releases.
	See the "Bootstrapping" section of CONTRIBUTING.md.
	EOS
        exit 1
    fi
    if [ ! -w "$SCRIPT" ]; then
        msg "You do not have permission to upgrade the installation in $SCRIPT"
        exit 1
    else
        TARGET_VERSION="${2:-stable}"
        echo "The script at $SCRIPT will be upgraded to the latest $TARGET_VERSION version."
        echo -n "Do you want to continue [Y/n]? "
        read RESP
        case "$RESP" in
            y|Y|"")
                echo
                msg "Upgrading..."
                TARGET="/tmp/lein-${$}-upgrade"
                if $cygwin; then
                    TARGET=$(cygpath -w "$TARGET")
                fi
                LEIN_SCRIPT_URL="https://github.com/technomancy/leiningen/raw/$TARGET_VERSION/bin/lein"
                $HTTP_CLIENT "$TARGET" "$LEIN_SCRIPT_URL"
                if [ $? == 0 ]; then
                    cmp -s "$TARGET" "$SCRIPT"
                    if [ $? == 0 ]; then
                        msg "Leiningen is already up-to-date."
                    fi
                    mv "$TARGET" "$SCRIPT" && chmod +x "$SCRIPT"
                    unset CLASSPATH
                    exec "$SCRIPT" version
                else
                    download_failed_message "$LEIN_SCRIPT_URL"
                fi;;
            *)
                msg "Aborted."
                exit 1;;
        esac
    fi
else
    if $cygwin; then
        # When running on Cygwin, use Windows-style paths for java
        ORIGINAL_PWD=$(cygpath -w "$ORIGINAL_PWD")
    fi

    # apply context specific CLASSPATH entries
    if [ -f .lein-classpath ]; then
        add_path CLASSPATH "$(cat .lein-classpath)"
    fi

    if [ -n "$DEBUG" ]; then
        msg "Leiningen's classpath: $CLASSPATH"
    fi

    if [ -r .lein-fast-trampoline ]; then
        export LEIN_FAST_TRAMPOLINE='y'
    fi

    if [ "$LEIN_FAST_TRAMPOLINE" != "" ] && [ -r project.clj ]; then
        INPUTS="$* $(cat project.clj) $LEIN_VERSION $(test -f "$LEIN_HOME/profiles.clj" && cat "$LEIN_HOME/profiles.clj") $(test -f profiles.clj && cat profiles.clj)"

        INPUT_CHECKSUM=$(echo "$INPUTS" | $SHASUM_CMD | cut -f 1 -d " ")
        # Just don't change :target-path in project.clj, mkay?
        TRAMPOLINE_FILE="target/trampolines/$INPUT_CHECKSUM"
    else
        if hash mktemp 2>/dev/null; then
            # Check if mktemp is available before using it
            TRAMPOLINE_FILE="$(mktemp /tmp/lein-trampoline-XXXXXXXXXXXXX)"
        else
            TRAMPOLINE_FILE="/tmp/lein-trampoline-$$"
        fi
        trap 'rm -f $TRAMPOLINE_FILE' EXIT
    fi

    if $cygwin; then
        TRAMPOLINE_FILE=$(cygpath -w "$TRAMPOLINE_FILE")
    fi

    if [ "$INPUT_CHECKSUM" != "" ] && [ -r "$TRAMPOLINE_FILE" ]; then
        if [ -n "$DEBUG" ]; then
            msg "Fast trampoline with $TRAMPOLINE_FILE."
        fi
        exec sh -c "exec $(cat "$TRAMPOLINE_FILE")"
    else
        export TRAMPOLINE_FILE
        "$LEIN_JAVA_CMD" \
            -Dfile.encoding=UTF-8 \
            -Dmaven.wagon.http.ssl.easy=false \
            -Dmaven.wagon.rto=10000 \
            $LEIN_JVM_OPTS \
            -Dleiningen.input-checksum="$INPUT_CHECKSUM" \
            -Dleiningen.original.pwd="$ORIGINAL_PWD" \
            -Dleiningen.script="$SCRIPT" \
            -classpath "$CLASSPATH" \
            clojure.main -m leiningen.core.main "$@"

        EXIT_CODE=$?

        if $cygterm ; then
          stty icanon echo > /dev/null 2>&1
        fi

        if [ -r "$TRAMPOLINE_FILE" ] && [ "$LEIN_TRAMPOLINE_WARMUP" = "" ]; then
            TRAMPOLINE="$(cat "$TRAMPOLINE_FILE")"
            if [ "$INPUT_CHECKSUM" = "" ]; then # not using fast trampoline
                rm "$TRAMPOLINE_FILE"
            fi
            if [ "$TRAMPOLINE" = "" ]; then
                exit $EXIT_CODE
            else
                exec sh -c "exec $TRAMPOLINE"
            fi
        else
            exit $EXIT_CODE
        fi
    fi
fi
