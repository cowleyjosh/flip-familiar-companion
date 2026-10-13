@echo off
setlocal EnableExtensions
cd /d "%~dp0"
echo.
echo GE Trade Journal - Windows RuneLite Development Test
echo Standalone GE tracking only. No Discord, bot, or network integration.
echo.
where java >nul 2>&1 || goto nojava
where javac >nul 2>&1 || goto nojava
where gradle >nul 2>&1 || goto nogradle
echo Using:
java -version
call gradle --version
echo.
echo Launching the development RuneLite client.
echo For Jagex Account login see LIVE_TEST_WINDOWS.md.
call gradle --no-daemon --console=plain run
set "exitcode=%ERRORLEVEL%"
echo.
if not "%exitcode%"=="0" echo Client task exited with code %exitcode%. Read the error text above.
echo See LIVE_TEST_WINDOWS.md for GE test steps and file location.
pause
exit /b %exitcode%

:nojava
echo Java/JDK not found. Install JDK 21, then reopen this launcher.
echo https://adoptium.net/temurin/releases/
pause
exit /b 1

:nogradle
echo Gradle was not found on PATH. Install Gradle 8.x to run this source.
echo https://gradle.org/install/
pause
exit /b 1
