#!/usr/bin/env bash
# Builds the runboard and starts it in a window on this machine, connected to the server set in
# local/runboard.properties. The first run creates that file and stops so that it can be filled in.
# The Finnish and Swedish Piper voices are downloaded to local/voices the first time they are needed.
set -euo pipefail

cd "$(dirname "$0")"
CONFIG=local/runboard.properties
VOICES=local/voices
VOICE_BASE_URL=https://huggingface.co/rhasspy/piper-voices/resolve/main

if [[ ! -f $CONFIG ]]; then
    mkdir -p local
    cat > "$CONFIG" <<'EOF'
# Settings for run-local.sh. Not in Git. See src/main/config/runboard.properties for every setting.

# The server's runboard endpoint: wss://, the host name, the context path and /ws/runboard.
idispatch.server.url=wss://idispatch.example.com/idispatch/ws/runboard

# The runboard key of a destination in the management console.
idispatch.runboardKey=

# A normal window instead of a full-screen kiosk.
idispatch.undecorated=false

idispatch.speech.engine=piper
# Used when the server doesn't send a language, as servers from before the language setting don't.
idispatch.speech.defaultLanguage=fi
idispatch.speech.piper.voice.fi=local/voices/fi_FI-harri-medium.onnx
idispatch.speech.piper.voice.sv=local/voices/sv_SE-nst-medium.onnx
EOF
    echo "Created runboard/$CONFIG."
    echo "Set idispatch.server.url and idispatch.runboardKey in it, then run this script again."
    exit 1
fi

if grep -q '^idispatch.server.url=.*example\.com' "$CONFIG" || grep -q '^idispatch.runboardKey=[[:space:]]*$' "$CONFIG"; then
    echo "Set idispatch.server.url and idispatch.runboardKey in runboard/$CONFIG first." >&2
    exit 1
fi

# Downloads a voice, given its path under piper-voices, unless it is already there.
download_voice() {
    local name
    name=$(basename "$1")
    if [[ ! -f $VOICES/$name.onnx ]]; then
        mkdir -p "$VOICES"
        echo "Downloading voice $name (about 60 MB)"
        curl -fL --progress-bar -o "$VOICES/$name.onnx.json" "$VOICE_BASE_URL/$1.onnx.json"
        curl -fL --progress-bar -o "$VOICES/$name.onnx.part" "$VOICE_BASE_URL/$1.onnx"
        mv "$VOICES/$name.onnx.part" "$VOICES/$name.onnx"
    fi
}

download_voice fi/fi_FI/harri/medium/fi_FI-harri-medium
download_voice sv/sv_SE/nst/medium/sv_SE-nst-medium

echo "Building the runboard"
(cd .. && mvn -q -pl runboard -Passemble package -DskipTests)

exec java -Didispatch.configFile="$CONFIG" -jar target/idispatch-runboard.jar
