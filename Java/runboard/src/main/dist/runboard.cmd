@echo off
rem Starts the iDispatch runboard on Windows. Double-click it, or run it from a command prompt.
rem The runboard reads runboard.properties in this folder. The first run creates it from runboard.properties.template.
setlocal
cd /d "%~dp0"

if exist runboard.properties goto configured
copy runboard.properties.template runboard.properties >nul
echo Created runboard.properties in %CD%.
echo Set idispatch.server.url and idispatch.runboardKey in it, then start the runboard again.
start "" notepad runboard.properties
pause
exit /b 1

:configured
set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"

set "JAVA_MAJOR=0"
set "JAVA_SETTINGS=%TEMP%\idispatch-runboard-java.txt"
"%JAVA%" -XshowSettings:properties -version >"%JAVA_SETTINGS%" 2>&1
for /f "tokens=2 delims==" %%v in ('findstr /c:"java.specification.version" "%JAVA_SETTINGS%"') do (
    for /f "tokens=1 delims=. " %%m in ("%%v") do set "JAVA_MAJOR=%%m"
)
del "%JAVA_SETTINGS%" >nul 2>&1
if %JAVA_MAJOR% LSS 25 (
    echo The runboard needs Java 25 or later.
    echo Install a Java runtime, for example from https://adoptium.net, or set JAVA_HOME to one.
    pause
    exit /b 1
)

if not exist voices\*.onnx echo No voices in %CD%\voices. Run download-voices.cmd to have notifications read out loud.

"%JAVA%" -jar idispatch-runboard.jar
if errorlevel 1 pause
