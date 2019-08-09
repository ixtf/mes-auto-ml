@echo off
cd..
set BASEDIR=%CD%
set SERVICE_NAME=jsw-test
set "SRV=%BASEDIR%\bin\prunsrv.exe"

%SRV% //DS//%SERVICE_NAME%

:end