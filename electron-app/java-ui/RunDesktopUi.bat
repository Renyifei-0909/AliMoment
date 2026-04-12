@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "JAVA_HOME=C:\Program Files (x86)\Android\openjdk\jdk-17.0.14"
set "MAVEN_HOME=D:\apache-maven-3.8.8"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: JDK not found at:
    echo   %JAVA_HOME%
    echo Edit JAVA_HOME in this .bat to your installed JDK, then try again.
    pause
    exit /b 1
)

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo ERROR: Maven not found at:
    echo   %MAVEN_HOME%\bin\mvn.cmd
    echo Install Maven or edit MAVEN_HOME in this .bat, then try again.
    pause
    exit /b 1
)

echo Starting Alimoment desktop Java UI...
call "%MAVEN_HOME%\bin\mvn.cmd" javafx:run
if errorlevel 1 (
    echo.
    echo Maven finished with an error. Check JDK and Maven install paths in this .bat file.
)
echo.
pause
