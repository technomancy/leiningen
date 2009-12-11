_lein_completion() {
    local cur prev tasks
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    tasks="clean compile deps help install jar new pom test uberjar version"

    case "${prev}" in
        clean | compile | deps | install | jar | new | pom | uberjar | version)
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
        test)
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
