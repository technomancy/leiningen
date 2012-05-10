_lein_completion() {
    local cur prev tasks
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    tasks="check classpath clean compile deploy deps help install jar javac new pom repl retest run search swank test trampoline uberjar upgrade version with-profile"

    case "${prev}" in
        check | classpath | clean | deploy | deps | install | jar | javac | new | pom | repl | swank | uberjar | version)
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
        test | retest )
            # list project's test namespaces:
            local namespaces=$(find test/ -type f -name "*.clj" -exec sed -n 's/^(ns[ ]*//p' '{}' '+')
            COMPREPLY=( $(compgen -W "${namespaces}" -- ${cur}) )
            ;;
        run | compile)
            # list project's src namespaces:
            local namespaces=$(find src/ -type f -name "*.clj" -exec sed -n 's/^(ns[ ]*//p' '{}' '+')
            COMPREPLY=( $(compgen -W "${namespaces}" -- ${cur}) )
            ;;
        lein)
            COMPREPLY=( $(compgen -W "${tasks}" -- ${cur}) )
            ;;
    esac

    return 0
}
complete -F _lein_completion lein
