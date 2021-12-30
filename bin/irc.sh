#!/bin/bash

# This script announces build failures on IRC since CircleCI broke the
# IRC integration

LOG=$(git log --oneline -n 1 HEAD)

echo "NICK leiningen-build
USER leiningen-build 8 x : leiningen-build
JOIN #leiningen
PRIVMSG #leiningen :Build failure! $LOG | $CIRCLE_BUILD_URL
QUIT" | nc irc.libera.chat 6667

exit 1
