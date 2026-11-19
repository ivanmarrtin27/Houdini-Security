@echo off
setlocal
title Houdini Security

REM Always run from this script's directory (project root)
cd /d "%~dp0"

set "MVN=%~dp0.tools\apache-maven-3.9.9\bin\mvn.cmd"
if not exist "%MVN%" (
  where mvn >nul 2>&1
  if errorlevel 1 (
    echo ERROR: No se encuentra Maven embebido ni "mvn" en el PATH.
    echo Esperado: .tools\apache-maven-3.9.9\bin\mvn.cmd
    pause
    exit /b 1
  )
  set "MVN=mvn"
)

where java >nul 2>&1
if errorlevel 1 (
  echo ERROR: Java no esta en el PATH. Instala JDK 21+ y reintenta.
  pause
  exit /b 1
)

echo Iniciando Houdini Security...
call "%MVN%" -q javafx:run
set "EC=%ERRORLEVEL%"
if not "%EC%"=="0" (
  echo.
  echo ERROR: La aplicacion salio con codigo %EC%.
  pause
)
exit /b %EC%
