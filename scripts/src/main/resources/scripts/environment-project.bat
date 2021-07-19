rem This batch is not supposed to be called manually
@echo off
if NOT "%DEVON_IDE_TRACE%"=="" echo on

call :load_properties "%DEVON_IDE_HOME%\scripts\devon.properties"
call :load_properties "%USERPROFILE%\devon.properties"
call :load_properties "%DEVON_IDE_HOME%\devon.properties"
call :load_properties "%SETTINGS_PATH%\devon.properties"
if "%WORKSPACE%" == "" (
  set WORKSPACE=main
)
set "WORKSPACE_PATH=%CD%\workspaces\%WORKSPACE%"
if not exist "%WORKSPACE_PATH%" (
  if "%WORKSPACE%" == "main" (
    echo Creating main workspace directory
    md "%WORKSPACE_PATH%"
  ) else (
    echo WARNING: Worksapce %WORKSPACE% does not exist
  )
)
call :load_properties "%WORKSPACE_PATH%\devon.properties"

rem setup path
if not defined DEVON_OLD_PATH (
  set "DEVON_OLD_PATH=%PATH%"
)
set "SOFTWARE_PATH=%CD%\software"
if not exist "%SETTINGS_PATH%" (
  echo:
  echo *** ATTENTION ***
  echo Your devonfw-ide is missing the settings at %SETTINGS_PATH%
  echo Please run the following command to complete your IDE setup:
  echo devon ide setup [^<settings-url^>]
)
setlocal EnableDelayedExpansion
for /f "delims=" %%i in ('dir /a:d /b "%SOFTWARE_PATH%\*.*"') do (
  if exist "%SOFTWARE_PATH%\%%i\bin" (
    set "IDE_PATH=%SOFTWARE_PATH%\%%i\bin;!IDE_PATH!"
  ) else (
    set "IDE_PATH=%SOFTWARE_PATH%\%%i;!IDE_PATH!"
  )
  rem Load custom configuration of software
  if exist "%SOFTWARE_PATH%\%%i\ide-config.bat" (
    call "%SOFTWARE_PATH%\%%i\ide-config.bat"
  )
)
(
  endlocal
  set "PATH=%IDE_PATH%%DEVON_OLD_PATH%"
)

rem node.js support
if exist "%SOFTWARE_PATH%\nodejs" (
  if not exist "%APPDATA%\npm" (
    md "%APPDATA%\npm";
  )
  set "PATH=%PATH%;%APPDATA%\npm"
)

:variables
rem load user settings late so variables like M2_REPO can be overridden
call :load_properties "%DEVON_IDE_HOME%\conf\devon.properties"
goto :eof

rem subroutine to load properties as environment variables with
rem %~1: path to properties file
:load_properties
if not exist "%~1" (
  goto :eof
)
for /F "eol=# delims== tokens=1,*" %%a in (%~1) do (
  if not "%%a" == "" (
    call :set_variable %%a %%b
  )
)
goto :eof

rem subroutine to set environment variable with
rem %~1: variable name
rem %~2: variable value (may contain ~ or ${var})
:set_variable
setlocal EnableDelayedExpansion
set "line=%*"
if "%~1%" == "export" (
  rem remove export as this only makes sense on linux/mac
  set line=!line:~7!
  shift
)
if "%~2%" == "" (
  set "search=%~1"
) else (
  set "search=%~1 "
)
set "replacement=%~1="
set value=%~2
if "!value:~0,1!" == "~" (
  rem variable value starts with tilde that needs to be replaced with users home dir (USERPROFILE)
  set line=%replacement%%USERPROFILE%!value:~1!
) else (
  set line=!line:%search%=%replacement%!
)
rem replace ${var} variable syntax with windows %var% syntax
echo "%~2" | findstr /r "\${..*}" >nul
if not errorlevel 1 (
  set line=!line:${=%%!
  set line=!line:}=%%!
)
set line=!line:"=!
(
  rem endlocal in () block to access local variable and "export" it
  endlocal
  rem use call in order to evaluate %var% inside value
  call set "%line%"
)
goto :eof
