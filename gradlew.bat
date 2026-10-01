@echo off
setlocal
set GRADLE_VERSION=9.6.0
set GRADLE_HOME=%USERPROFILE%\.gradle\wrapper\dists\gradle-%GRADLE_VERSION%-bin\hasu-bootstrap
if not exist "%GRADLE_HOME%\gradle-%GRADLE_VERSION%\bin\gradle.bat" (
  if not exist "%GRADLE_HOME%" mkdir "%GRADLE_HOME%"
  powershell -NoProfile -Command "Invoke-WebRequest -UseBasicParsing https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip -OutFile '%GRADLE_HOME%\gradle.zip'; Expand-Archive -Force '%GRADLE_HOME%\gradle.zip' '%GRADLE_HOME%'; Remove-Item '%GRADLE_HOME%\gradle.zip'"
)
call "%GRADLE_HOME%\gradle-%GRADLE_VERSION%\bin\gradle.bat" %*
