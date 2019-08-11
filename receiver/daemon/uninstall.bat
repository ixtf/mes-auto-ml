@echo off

set SERVICE_NAME=mes-auto-sender

set JAVA_HOME=C:\Program Files\Java\jdk-11.0.4
set ROOT_PATH=%JAVA_HOME%\conf\security\policy\limited
set SRV=%ROOT_PATH%\config.exe

%SRV% //DS//%SERVICE_NAME%

:end