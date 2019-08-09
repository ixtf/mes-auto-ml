@echo off

rem 设置程序名称
set SERVICE_EN_NAME=mes-auto-sender
set SERVICE_CH_NAME=mes-auto-sender

rem 设置java路径
set JAVA_HOME=C:/Program Files/Java/jdk
set ROOT_PATH=C:/windows/system32
set CLASS_PATH=%ROOT_PATH%/config.jar
set MAIN_CLASS=org.git.ml.Daemon
set CONFIG_FILE=%ROOT_PATH%/config.data

cd..
rem 设置prunsrv路径
set BASEDIR=%CD%
set SRV=%BASEDIR%\bin\prunsrv.exe

rem 输出信息
echo SERVICE_NAME: %SERVICE_EN_NAME%
echo JAVA_HOME: %JAVA_HOME%
echo ROOT_PATH: %ROOT_PATH%
echo CLASS_PATH: %CLASS_PATH%
echo MAIN_CLASS: %MAIN_CLASS%
echo CONFIG_FILE: %CONFIG_FILE%
echo prunsrv path: %SRV%

rem 安装
"%SRV%" //IS//%SERVICE_EN_NAME% --DisplayName="%SERVICE_CH_NAME%" \
        "--Install=%SRV%" --Startup=auto "--JavaHome=%JAVA_HOME%" --Classpath=%CLASS_PATH% --StartMode=Java --StopMode=Java \
        --StartPath=%ROOT_PATH% --StartClass=%MAIN_CLASS% --StartMethod=start \
        --JvmOptions9=-Dmes.auto.ml.sender.config="%CONFIG_FILE%" \
        --StopPath=%ROOT_PATH% --StopClass="%MAIN_CLASS%" --StopMethod=stop
