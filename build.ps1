$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$($env:JAVA_HOME)\bin;$env:Path"
cd C:\Users\77497\.qclaw\workspace\AutoBookkeeper
.\gradlew.bat clean assembleDebug 2>&1
