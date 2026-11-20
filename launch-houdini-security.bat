@echo off
setlocal EnableExtensions
title Houdini Security
cd /d "%~dp0"

set "STAGE=%~dp0dist\desktop"
set "JAR=%STAGE%\Houdini Security.jar"
set "LIB=%STAGE%\lib"

if exist "%JAR%" if exist "%LIB%" goto :run_jar

REM Fallback: Maven javafx:run (dev)
set "MVN=%~dp0.tools\apache-maven-3.9.9\bin\mvn.cmd"
if not exist "%MVN%" (
  where mvn >nul 2>&1
  if errorlevel 1 (
    echo ERROR: No hay dist\desktop ni Maven.
    echo Ejecuta: mvn -DskipTests package
    echo y: powershell -File .\scripts\stage-desktop-launcher.ps1
    pause
    exit /b 1
  )
  set "MVN=mvn"
)
where java >nul 2>&1
if errorlevel 1 (
  echo ERROR: Java no esta en el PATH. Instala JDK 21+.
  pause
  exit /b 1
)
echo Iniciando Houdini Security (Maven javafx:run)...
call "%MVN%" -q javafx:run
exit /b %ERRORLEVEL%

:run_jar
set "JAVA_EXE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE if exist "C:\Program Files\Java\jdk-24\bin\java.exe" set "JAVA_EXE=C:\Program Files\Java\jdk-24\bin\java.exe"
if not defined JAVA_EXE if exist "C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe" set "JAVA_EXE=C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe"
if not defined JAVA_EXE (
  where java >nul 2>&1
  if not errorlevel 1 for /f "delims=" %%J in ('where java') do (
    set "JAVA_EXE=%%J"
    goto :have_java
  )
)
:have_java
if not defined JAVA_EXE (
  echo ERROR: Necesitas JDK 21+ en PATH o JAVA_HOME.
  pause
  exit /b 1
)
echo Iniciando Houdini Security...
"%JAVA_EXE%" --enable-native-access=ALL-UNNAMED -cp "%JAR%;%LIB%\*" com.houdinisecurity.Launcher
set "EC=%ERRORLEVEL%"
if not "%EC%"=="0" (
  echo.
  echo ERROR: La aplicacion salio con codigo %EC%.
  pause
)
exit /b %EC%
