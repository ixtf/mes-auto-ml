@echo off

rem 设置程序名称
set SERVICE_EN_NAME=mes-auto-sender
set SERVICE_CH_NAME=mes-auto-sender

rem 设置java路径
set JAVA_HOME=C:\Program Files\Java\jdk11

cd..
set BASEDIR=%CD%
set MAIN_CLASS=org.jzb.mes.auto.ml.receiver.Receiver

rem 设置prunsrv路径
set SRV=%BASEDIR%\bin\prunsrv.exe

rem 输出信息
echo SERVICE_NAME: %SERVICE_EN_NAME%
echo JAVA_HOME: %JAVA_HOME%
echo MAIN_CLASS: %MAIN_CLASS%
echo prunsrv path: %SRV%

rem 设置jvm
if "%JVM%" == "" goto findJvm
if exist "%JVM%" goto foundJvm
:findJvm
set "JVM=%JAVA_HOME%\jre\bin\server\jvm.dll"
if exist "%JVM%" goto foundJvm
echo can not find jvm.dll automatically,
echo please use COMMAND to localation it
echo for example : set "JVM=C:\Program Files\Java\jdk1.8.0_25\jre\bin\server\jvm.dll"
echo then install service
goto end
:foundJvm

rem 安装
"%SRV%" //IS//%SERVICE_EN_NAME% --DisplayName="%SERVICE_CH_NAME%" "--Classpath=%CLASSPATH%" "--Install=%SRV%" "--JavaHome=%JAVA_HOME%" "--Jvm=%JVM%" --JvmMs=256 --JvmMx=1024 --Startup=auto --JvmOptions=-Djcifs.smb.client.dfs.disabled=false ++JvmOptions=-Djcifs.resolveOrder=DNS "--StartPath=%BASEDIR%" --StartMode=jvm --StartClass=%MAIN_CLASS% --StartMethod=start "--StopPath=%BASEDIR%" --StopMode=jvm --StopClass=%MAIN_CLASS% --StopMethod=stop --LogPath=%LOGPATH% --StdOutput=auto --StdError=auto
