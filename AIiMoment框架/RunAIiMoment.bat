@echo off
set JAVA_HOME=C:\Program Files (x86)\Android\openjdk\jdk-17.0.14
set MAVEN_HOME=D:\apache-maven-3.8.8
set PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%

cd /d %~dp0
echo Starting AIiMoment...
mvn exec:java -Dexec.mainClass=com.aiimoment.Main
pause