#!/bin/sh
SCRIPT=$(readlink -f "$0")
SCRIPT_PATH=$(dirname "$SCRIPT")

/usr/bin/java -jar "$SCRIPT_PATH"/idispatch-server.jar
