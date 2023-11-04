#!/usr/bin/env bash

source test/init

+cmd:ok-ver shellcheck 0.9.0 ||
  plan skip-all "Test requires shellcheck 0.9.0+ to be installed"

skip=1090,1091

while read -r file; do
  shebang=$(head -n1 "$file")

  if [[ $file == *.bash ]] ||
     [[ $shebang == '#!'*[/\ ]bash ]]
  then
    ok "$(shellcheck -e $skip "$file")" \
      "Bash file '$file' passes shellcheck"
  fi
done < <(
  git ls-files |
    grep -Ev '(^\.|\.swp$)'
)

done-testing
