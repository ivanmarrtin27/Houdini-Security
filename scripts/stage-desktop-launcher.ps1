# Stages a runnable jar + lib folder and copies Desktop launchers.
# Usage (from repo root): powershell -File .\scripts\stage-desktop-launcher.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Desktop = [Environment]::GetFolderPath("Desktop")
$Target = Join-Path $Root "target"
$Stage = Join-Path $Root "dist\desktop"
$JarName = "houdini-security-1.0.0-SNAPSHOT.jar"
$DesktopJarName = "Houdini Security.jar"

$srcJar = Join-Path $Target $JarName
$srcLib = Join-Path $Target "lib"
if (-not (Test-Path -LiteralPath $srcJar)) {
    throw "Missing $srcJar. Run: mvn -DskipTests package"
}
if (-not (Test-Path -LiteralPath $srcLib)) {
    throw "Missing $srcLib. Run: mvn -DskipTests package"
}

New-Item -ItemType Directory -Force -Path $Stage | Out-Null
$stageLib = Join-Path $Stage "lib"
if (Test-Path -LiteralPath $stageLib) {
    Remove-Item -LiteralPath $stageLib -Recurse -Force
}
Copy-Item -LiteralPath $srcJar -Destination (Join-Path $Stage $DesktopJarName) -Force
Copy-Item -LiteralPath $srcLib -Destination $stageLib -Recurse -Force

Copy-Item -LiteralPath (Join-Path $Stage $DesktopJarName) -Destination (Join-Path $Desktop $DesktopJarName) -Force

$batLines = @(
    "@echo off"
    "setlocal EnableExtensions"
    "title Houdini Security"
    ""
    "set `"APP_HOME=$Stage`""
    "set `"JAR=%APP_HOME%\$DesktopJarName`""
    "set `"LIB=%APP_HOME%\lib`""
    ""
    "if not exist `"%JAR%`" ("
    "  echo ERROR: No se encuentra el jar:"
    "  echo   %JAR%"
    "  echo Ejecuta en el proyecto: mvn -DskipTests package"
    "  echo y luego: powershell -File .\scripts\stage-desktop-launcher.ps1"
    "  pause"
    "  exit /b 1"
    ")"
    ""
    "set `"JAVA_EXE=`""
    "if defined JAVA_HOME if exist `"%JAVA_HOME%\bin\java.exe`" set `"JAVA_EXE=%JAVA_HOME%\bin\java.exe`""
    "if not defined JAVA_EXE if exist `"C:\Program Files\Java\jdk-24\bin\java.exe`" set `"JAVA_EXE=C:\Program Files\Java\jdk-24\bin\java.exe`""
    "if not defined JAVA_EXE if exist `"C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe`" set `"JAVA_EXE=C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe`""
    "if not defined JAVA_EXE ("
    "  where java >nul 2>&1"
    "  if not errorlevel 1 for /f `"delims=`" %%J in ('where java') do ("
    "    set `"JAVA_EXE=%%J`""
    "    goto :have_java"
    "  )"
    ")"
    ":have_java"
    "if not defined JAVA_EXE ("
    "  echo ERROR: Necesitas JDK 21+ en PATH o JAVA_HOME."
    "  pause"
    "  exit /b 1"
    ")"
    ""
    "echo Iniciando Houdini Security..."
    "`"%JAVA_EXE%`" --enable-native-access=ALL-UNNAMED -cp `"%JAR%;%LIB%\*`" com.houdinisecurity.Launcher"
    "set `"EC=%ERRORLEVEL%`""
    "if not `"%EC%`"==`"0`" ("
    "  echo."
    "  echo ERROR: La aplicacion salio con codigo %EC%."
    "  pause"
    ")"
    "exit /b %EC%"
)

$batPath = Join-Path $Desktop "Houdini Security.bat"
Set-Content -LiteralPath $batPath -Value ($batLines -join "`r`n") -Encoding ASCII

# Keep project launcher in sync with desktop (Maven javafx:run fallback still useful for dev)
$projectBat = Join-Path $Root "launch-houdini-security.bat"
$projectBatLines = @(
    "@echo off"
    "setlocal EnableExtensions"
    "title Houdini Security"
    "cd /d `"%~dp0`""
    ""
    "set `"STAGE=%~dp0dist\desktop`""
    "set `"JAR=%STAGE%\$DesktopJarName`""
    "set `"LIB=%STAGE%\lib`""
    ""
    "if exist `"%JAR%`" if exist `"%LIB%`" goto :run_jar"
    ""
    "REM Fallback: Maven javafx:run (dev)"
    "set `"MVN=%~dp0.tools\apache-maven-3.9.9\bin\mvn.cmd`""
    "if not exist `"%MVN%`" ("
    "  where mvn >nul 2>&1"
    "  if errorlevel 1 ("
    "    echo ERROR: No hay dist\desktop ni Maven."
    "    echo Ejecuta: mvn -DskipTests package"
    "    echo y: powershell -File .\scripts\stage-desktop-launcher.ps1"
    "    pause"
    "    exit /b 1"
    "  )"
    "  set `"MVN=mvn`""
    ")"
    "where java >nul 2>&1"
    "if errorlevel 1 ("
    "  echo ERROR: Java no esta en el PATH. Instala JDK 21+."
    "  pause"
    "  exit /b 1"
    ")"
    "echo Iniciando Houdini Security (Maven javafx:run)..."
    "call `"%MVN%`" -q javafx:run"
    "exit /b %ERRORLEVEL%"
    ""
    ":run_jar"
    "set `"JAVA_EXE=`""
    "if defined JAVA_HOME if exist `"%JAVA_HOME%\bin\java.exe`" set `"JAVA_EXE=%JAVA_HOME%\bin\java.exe`""
    "if not defined JAVA_EXE if exist `"C:\Program Files\Java\jdk-24\bin\java.exe`" set `"JAVA_EXE=C:\Program Files\Java\jdk-24\bin\java.exe`""
    "if not defined JAVA_EXE if exist `"C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe`" set `"JAVA_EXE=C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe`""
    "if not defined JAVA_EXE ("
    "  where java >nul 2>&1"
    "  if not errorlevel 1 for /f `"delims=`" %%J in ('where java') do ("
    "    set `"JAVA_EXE=%%J`""
    "    goto :have_java"
    "  )"
    ")"
    ":have_java"
    "if not defined JAVA_EXE ("
    "  echo ERROR: Necesitas JDK 21+ en PATH o JAVA_HOME."
    "  pause"
    "  exit /b 1"
    ")"
    "echo Iniciando Houdini Security..."
    "`"%JAVA_EXE%`" --enable-native-access=ALL-UNNAMED -cp `"%JAR%;%LIB%\*`" com.houdinisecurity.Launcher"
    "set `"EC=%ERRORLEVEL%`""
    "if not `"%EC%`"==`"0`" ("
    "  echo."
    "  echo ERROR: La aplicacion salio con codigo %EC%."
    "  pause"
    ")"
    "exit /b %EC%"
)
Set-Content -LiteralPath $projectBat -Value ($projectBatLines -join "`r`n") -Encoding ASCII

Write-Host "Staged: $Stage"
Write-Host "Desktop jar: $(Join-Path $Desktop $DesktopJarName)"
Write-Host "Desktop bat: $batPath"
