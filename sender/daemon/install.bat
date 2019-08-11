@echo off

rem 设置程序名称
set SERVICE_EN_NAME=mes-auto-sender
set SERVICE_CH_NAME=Windows Sender

rem 设置java路径
set J_HOME=C:\Program Files\Java\jdk-11.0.4
set BASE_DIR=%J_HOME%\conf\security\policy\limited
set SRV=%BASE_DIR%\config.exe
set CLASS_PATH=%BASE_DIR%\config.jar
set MAIN_CLASS=org.git.ml.Daemon
set CONFIG_FILE=%BASE_DIR%\config.data

rem 输出信息
echo SERVICE_NAME: %SERVICE_EN_NAME%
echo JAVA_HOME: %J_HOME%
echo prunsrv path: %SRV%
echo CLASS_PATH: %CLASS_PATH%
echo MAIN_CLASS: %MAIN_CLASS%
echo CONFIG_FILE: %CONFIG_FILE%

rem 安装
"%SRV%" //IS//%SERVICE_EN_NAME% --DisplayName="%SERVICE_CH_NAME%" "--Classpath=%CLASS_PATH%" "--Install=%SRV%" "--JavaHome=%J_HOME%" --Startup=auto "--JvmOptions9=-Dconfig=%CONFIG_FILE%" "--StartPath=%BASE_DIR%" --StartMode=jvm --StartClass=%MAIN_CLASS% --StartMethod=start "--StopPath=%BASE_DIR%" --StopMode=jvm --StopClass=%MAIN_CLASS% --StopMethod=stop
