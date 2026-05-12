@echo off
set JAVA_HOME=C:\Progra~1\Android\Androi~1\jbr
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d C:\Users\77497\.qclaw\workspace\AutoBookkeeper
gradlew.bat clean assembleDebug
if %ERRORLEVEL% EQU 0 (
    copy app\build\outputs\apk\debug\app-debug.apk C:\Users\77497\Desktop\autobookkeeper-drag-fix.apk
    echo Build successful, APK copied to desktop
)
