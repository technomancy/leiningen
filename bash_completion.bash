_lein_completion() {
    local cur prev tasks
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    tasks="change check classpath clean compile deploy deps do help install jar javac new plugin pom release repl retest run search show-profiles test trampoline uberjar update-in upgrade vcs version with-profile"

    case "${prev}" in
        change | check | classpath | clean | deploy | deps | do | install | jar | javac | new | plugin | pom | release | repl | show-profiles | uberjar | update-in | vcs | version)
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
