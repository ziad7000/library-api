@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------
@IF "%DEBUG%" == "" @ECHO OFF
@setlocal EnableExtensions EnableDelayedExpansion

set ERROR_CODE=0
set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties

if not defined JAVA_HOME (
  for %%i in (java.exe) do set "JAVACMD=%%~$PATH:i"
) else (
  set "JAVACMD=%JAVA_HOME%\bin\java.exe"
)

if not exist "%JAVACMD%" (
  echo Error: JAVA_HOME not found in your environment. >&2
  echo Please set the JAVA_HOME variable in your environment to match the >&2
  echo location of your Java installation. >&2
  goto error
)

"%JAVACMD%" %MAVEN_OPTS% -jar "%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar" %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%
exit /B %ERROR_CODE%
