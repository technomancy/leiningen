_lein_completion() {
    local cur prev tasks
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    tasks="classpath clean compile deploy deps help install interactive jar javac new plugin pom repl run swank test test! uberjar version"

    case "${prev}" in
        classpath | clean | compile | deploy | deps | install | interactive | jar | javac | new | plugin | pom | repl | swank | uberjar | version)
            COMPREPLY=()
            ;;
        help)
            # Show tasks again, but only once; don't infinitely recurse
            local prev2="${COMP_WORDS[COMP_CWORD-2]}"
            if [ "$prev2" == "help" ]; then
                COMPREPLY=()
            else
                COMPREPLY=( $(compgen -W "${tasks}" -- ${cur}) )
            fi
            ;;
        run | test | test!)
            # list project's test namespaces:
            local namespaces=$(find test/ -type f -name "*.clj" -exec grep -E \
                '^\(ns[[:space:]]+\w+' '{}' ';' | sed -n 's/(ns[ ]*//p')
            COMPREPLY=( $(compgen -W "${namespaces}" -- ${cur}) )
            ;;
        lein)
            COMPREPLY=( $(compgen -W "${tasks}" -- ${cur}) )
            ;;
    esac

    return 0
}
complete -F _lein_completion lein
