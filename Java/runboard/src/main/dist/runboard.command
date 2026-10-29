#!/usr/bin/env bash
# Starts the iDispatch runboard on macOS or Linux. Double-click it in the Finder, or run it from a terminal.
# The runboard reads runboard.properties in this folder. The first run creates it from runboard.properties.template.
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -f runboard.properties ]]; then
    cp runboard.properties.template runboard.properties
    echo "Created runboard.properties in $(pwd)."
    echo "Set idispatch.server.url and idispatch.runboardKey in it, then start the runboard again."
    if [[ $(uname) == Darwin ]]; then
        open -e runboard.properties
    fi
    exit 1
fi

if [[ -n ${JAVA_HOME:-} ]]; then
    JAVA=$JAVA_HOME/bin/java
elif [[ $(uname) == Darwin ]] && JAVA_HOME=$(/usr/libexec/java_home -v 25+ 2>/dev/null); then
    JAVA=$JAVA_HOME/bin/java
else
    JAVA=java
fi

JAVA_VERSION=$("$JAVA" -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java\.specification\.version = //p' || true)
if [[ -z $JAVA_VERSION || ${JAVA_VERSION%%.*} -lt 25 ]]; then
    echo "The runboard needs Java 25 or later, but found ${JAVA_VERSION:-no Java}." >&2
    echo "Install a Java runtime, for example from https://adoptium.net, or set JAVA_HOME to one." >&2
    exit 1
fi

if ! ls voices/*.onnx >/dev/null 2>&1; then
    echo "No voices in $(pwd)/voices. Run download-voices.command to have notifications read out loud."
fi

exec "$JAVA" -jar idispatch-runboard.jar
