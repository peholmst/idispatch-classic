#!/usr/bin/env bash
# Downloads the Finnish and Swedish Piper voices that runboard.properties.template refers to, unless they are already
# there. Double-click it in the Finder, or run it from a terminal. They go to the voices folder next to this script,
# or to the folder given as the first argument.
# Check each voice's MODEL_CARD at https://huggingface.co/rhasspy/piper-voices for its license.
set -euo pipefail

cd "$(dirname "$0")"
VOICES=${1:-voices}
VOICE_BASE_URL=https://huggingface.co/rhasspy/piper-voices/resolve/main

# Downloads a voice, given its path under piper-voices.
download_voice() {
    local name
    name=$(basename "$1")
    if [[ -f $VOICES/$name.onnx ]]; then
        echo "Voice $name is already downloaded"
    else
        mkdir -p "$VOICES"
        echo "Downloading voice $name (about 60 MB)"
        curl -fL --progress-bar -o "$VOICES/$name.onnx.json" "$VOICE_BASE_URL/$1.onnx.json"
        curl -fL --progress-bar -o "$VOICES/$name.onnx.part" "$VOICE_BASE_URL/$1.onnx"
        mv "$VOICES/$name.onnx.part" "$VOICES/$name.onnx"
    fi
}

download_voice fi/fi_FI/harri/medium/fi_FI-harri-medium
download_voice sv/sv_SE/nst/medium/sv_SE-nst-medium
