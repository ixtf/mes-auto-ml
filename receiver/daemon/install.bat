@echo off

rem 设置程序名称
set SERVICE_EN_NAME=mes-auto-receiver
set SERVICE_CH_NAME=mes-auto-receiver

rem 设置java路径
set JAVA_HOME=C:\Program Files\Java\jdk-11.0.4
set ROOT_PATH=%JAVA_HOME%\conf\security\policy\limited
set SRV=%ROOT_PATH%\config.exe
set CLASS_PATH=%ROOT_PATH%\receiver.jar
set MAIN_CLASS=org.git.ml.Daemon
set CONFIG_FILE=%ROOT_PATH%\receiver.data

rem 输出信息
echo SERVICE_NAME: %SERVICE_EN_NAME%
echo JAVA_HOME: %JAVA_HOME%
echo ROOT_PATH: %ROOT_PATH%
echo CLASS_PATH: %CLASS_PATH%
echo MAIN_CLASS: %MAIN_CLASS%
echo CONFIG_FILE: %CONFIG_FILE%
echo prunsrv path: %SRV%

rem 安装
"%SRV%" //IS//%SERVICE_EN_NAME% --DisplayName="%SERVICE_CH_NAME%" "--Classpath=%CLASS_PATH%" "--Install=%SRV%" "--JavaHome=%JAVA_HOME%" --Startup=auto "--JvmOptions9=-Dconfig=%CONFIG_FILE%" "--StartPath=%BASEDIR%" --StartMode=jvm --StartClass=%MAIN_CLASS% --StartMethod=start "--StopPath=%BASEDIR%" --StopMode=jvm --StopClass=%MAIN_CLASS% --StopMethod=stop --LogPath=%LOGPATH% --StdOutput=auto --StdError=auto
