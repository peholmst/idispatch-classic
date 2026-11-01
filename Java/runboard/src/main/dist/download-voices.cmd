@echo off
rem Downloads the Finnish and Swedish Piper voices that runboard.properties.template refers to, unless they are
rem already there. They go to the voices folder next to this script. Needs Windows 10 or later, which comes with curl.
rem Check each voice's MODEL_CARD at https://huggingface.co/rhasspy/piper-voices for its license.
setlocal
cd /d "%~dp0"
set "VOICE_BASE_URL=https://huggingface.co/rhasspy/piper-voices/resolve/main"

call :download fi/fi_FI/harri/medium fi_FI-harri-medium || goto failed
call :download sv/sv_SE/lisa/medium sv_SE-lisa-medium || goto failed
pause
exit /b 0

:failed
echo Downloading the voices failed.
pause
exit /b 1

rem Downloads a voice, given its folder under piper-voices and its name.
:download
if exist "voices\%2.onnx" (
    echo Voice %2 is already downloaded
    exit /b 0
)
if not exist voices mkdir voices
echo Downloading voice %2 (about 60 MB)
curl -fL --progress-bar -o "voices\%2.onnx.json" "%VOICE_BASE_URL%/%1/%2.onnx.json" || exit /b 1
curl -fL --progress-bar -o "voices\%2.onnx.part" "%VOICE_BASE_URL%/%1/%2.onnx" || exit /b 1
move /y "voices\%2.onnx.part" "voices\%2.onnx" >nul
exit /b 0
