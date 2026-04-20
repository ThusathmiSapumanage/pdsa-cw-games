@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-23
set PATH=%JAVA_HOME%\bin;%PATH%
set JAVA_TOOL_OPTIONS=-Xmx4g
cd /d "%~dp0"
call "%~dp0mvnw.cmd" test %*
