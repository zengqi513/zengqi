@echo off
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set SDK_ROOT=C:\Users\77497\AppData\Local\Android\Sdk
"%SDK_ROOT%\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root=%SDK_ROOT% "platform-tools" "build-tools;34.0.0" "platforms;android-34"
