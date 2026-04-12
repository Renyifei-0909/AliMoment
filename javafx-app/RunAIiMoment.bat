@echo off
rem Second launch keeps a console open so you always see errors.
if /i not "%~1"=="inner" (
    start "AIiMoment" cmd /k "%~f0" inner
    exit /b 0
)

setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

echo ========================================
echo AIiMoment - JavaFX
echo Folder: %CD%
echo ========================================
echo.

rem ----- JDK: env, then PATH, then common folders -----
set "_JAVA_OK=0"
if not "%JAVA_HOME%"=="" if exist "%JAVA_HOME%\bin\java.exe" set "_JAVA_OK=1"
if "!_JAVA_OK!"=="0" (
    for /f "delims=" %%I in ('where java 2^>nul') do (
        set "_BIN=%%~dpI"
        rem Parent of bin folder becomes JAVA_HOME
        set "JAVA_HOME=!_BIN:~0,-5!"
        if exist "!JAVA_HOME!\bin\java.exe" set "_JAVA_OK=1" & goto :jdk_done
    )
)
:jdk_done
if "!_JAVA_OK!"=="0" (
    for %%D in (
        "%ProgramFiles%\Eclipse Adoptium\jdk-17.0.13.11-hotspot"
        "%ProgramFiles%\Eclipse Adoptium\jdk-21.0.5.11-hotspot"
        "%ProgramFiles%\Java\jdk-17"
        "%ProgramFiles%\Java\jdk-11"
        "%LocalAppData%\Programs\Eclipse Adoptium\jdk-17.0.13.11-hotspot"
    ) do (
        if exist "%%~D\bin\java.exe" set "JAVA_HOME=%%~D" & set "_JAVA_OK=1" & goto :jdk2
    )
    for %%E in ("%ProgramFiles(x86)%") do set "P86=%%~E"
    if exist "!P86!\Android\openjdk\jdk-17.0.14\bin\java.exe" (
        set "JAVA_HOME=!P86!\Android\openjdk\jdk-17.0.14"
        set "_JAVA_OK=1"
    )
)
:jdk2
if "!_JAVA_OK!"=="0" (
    echo [X] JDK not found.
    echo     Install Temurin JDK 17: https://adoptium.net/
    echo     Or set JAVA_HOME to your JDK folder ^(must contain bin\java.exe^).
    echo.
    goto :end
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo [OK] JAVA_HOME=!JAVA_HOME!

rem ----- Maven: project wrapper first, then PATH, then common dirs -----
if exist "%~dp0mvnw.cmd" (
    echo [OK] Using Maven Wrapper ^(mvnw.cmd^) — first run may download Maven, please wait.
    echo.
    echo Starting: mvnw.cmd javafx:run
    echo ----------------------------------------
    call "%~dp0mvnw.cmd" javafx:run
    echo ----------------------------------------
    if errorlevel 1 echo. & echo Maven exited with error code !errorlevel!
    goto :end
)

set "MVN_CMD="
for /f "delims=" %%M in ('where mvn.cmd 2^>nul') do set "MVN_CMD=%%M" & goto :mvn_ok
for %%P in (
    "D:\apache-maven-3.8.8\bin\mvn.cmd"
    "D:\apache-maven-3.9.9\bin\mvn.cmd"
    "C:\apache-maven-3.8.8\bin\mvn.cmd"
    "C:\Program Files\Apache\Maven\bin\mvn.cmd"
    "%UserProfile%\apache-maven\bin\mvn.cmd"
) do if exist "%%~P" set "MVN_CMD=%%~P" & goto :mvn_ok
:mvn_ok
if not defined MVN_CMD (
    echo [X] Maven not found ^(mvn.cmd^).
    echo     This project includes mvnw.cmd — if you see this, mvnw.cmd is missing from javafx-app folder.
    echo     Or install Maven: https://maven.apache.org/download.cgi
    echo.
    goto :end
)
echo [OK] Maven: !MVN_CMD!
echo.
echo Starting: mvn javafx:run
echo ----------------------------------------
call "!MVN_CMD!" javafx:run
echo ----------------------------------------
if errorlevel 1 echo. & echo Maven exited with error code !errorlevel!

:end
echo.
echo Window stays open. Type EXIT to close.
endlocal
