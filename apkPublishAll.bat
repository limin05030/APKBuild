@echo off
setlocal EnableDelayedExpansion

set startTime=%time%
set logFilePath=%TMP%\apkPublishAllLog.gradle
del %logFilePath% 2> nul
call gradlew -PlogFilePath=%logFilePath% apkPublishDebug && call gradlew -PlogFilePath=%logFilePath% apkPublishRelease
set endTime=%time%

echo INSTALL PATH:
FOR /F "delims=" %%i IN (%logFilePath%) DO echo %%i

set /a h=%endTime:~0,2%-%startTime:~0,2%
set /a m=%endTime:~3,2%-%startTime:~3,2%
set /a s=%endTime:~6,2%-%startTime:~6,2%
set /a w=%endTime:~9,2%-%startTime:~9,2%
set /a tw=%h%*3600000 + %m%*60000 + %s%*1000 + %s% + %w%*10
set /a rw=%tw%%%1000
set /a ts=%tw%/1000
set /a rs=%ts%%%60
set /a tm=%ts%/60
set /a rm=%tm%%%60
set /a rh=%tm%/60

set outTime=%rs%.%rw% secs
if %rm% gtr 0 (
	set outTime=%rm% mins %outTime%
)
if %rh% gtr 0 (
	set outTime=%rh% hours %outTime%
)

echo.
echo Build Total Time: %outTime%

endLocal