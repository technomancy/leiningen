@echo off

setLocal EnableExtensions EnableDelayedExpansion

set LEIN_VERSION=2.1.2

if "%LEIN_VERSION:~-9%" == "-SNAPSHOT" (
    set SNAPSHOT=YES
) else (
    set SNAPSHOT=NO
)

set ORIGINAL_PWD=%CD%
:: If ORIGINAL_PWD ends with a backslash (such as C:\),
:: we need to escape it with a second backslash.
if "%ORIGINAL_PWD:~-1%x" == "\x" set "ORIGINAL_PWD=%ORIGINAL_PWD%\"

call :FIND_DIR_CONTAINING_UPWARDS project.clj
if "%DIR_CONTAINING%" neq "" cd "%DIR_CONTAINING%"

:: LEIN_JAR and LEIN_HOME variables can be set manually.

if "x%LEIN_HOME%" == "x" (
    set LEIN_HOME=%USERPROFILE%\.lein
)

if "x%LEIN_JAR%" == "x" set LEIN_JAR=!LEIN_HOME!\self-installs\leiningen-!LEIN_VERSION!-standalone.jar

if "%1" == "self-install" goto SELF_INSTALL
if "%1" == "upgrade"      goto UPGRADE

if exist "%~dp0..\src\leiningen\version.clj" (
    :: Running from source checkout.
    call :SET_LEIN_ROOT "%~dp0.."

    set LEIN_LIBS=
    for %%j in ("!LEIN_ROOT!\leiningen-core\lib\*") do set LEIN_LIBS=!LEIN_LIBS!%%~fj;
    set LEIN_LIBS=!LEIN_LIBS!

    if "x!LEIN_LIBS!" == "x" goto NO_DEPENDENCIES

    set CLASSPATH=!LEIN_LIBS!!LEIN_ROOT!\leiningen-core\src;!LEIN_ROOT!\leiningen-core\resources;!LEIN_ROOT!\leiningen-core\test;!LEIN_ROOT!\src;!LEIN_ROOT!\resources

    :: Apply context specific CLASSPATH entries
    if exist "%~dp0..\.lein-classpath" (
        for /f %%i in ("%~dp0...lein-classpath") do set CONTEXT_CP=%%i

        if NOT "x!CONTEXT_CP!"=="x" (
            set CLASSPATH=!CONTEXT_CP!;!CLASSPATH!
        )
    )
) else (
    :: Not running from a checkout.
    if not exist "%LEIN_JAR%" goto NO_LEIN_JAR
    set CLASSPATH=%LEIN_JAR%
  
    if exist ".lein-classpath" (
        for /f %%i in (.lein-classpath) do set CONTEXT_CP=%%i 

        if NOT "x!CONTEXT_CP!"=="x" (
            set CLASSPATH=!CONTEXT_CP!;!CLASSPATH!
        )
    )
)

if not "x%DEBUG%" == "x" echo CLASSPATH=!CLASSPATH!
:: ##################################################

if "x!JAVA_CMD!" == "x" set JAVA_CMD=java
if "x!LEIN_JAVA_CMD!" == "x" set LEIN_JAVA_CMD=%JAVA_CMD%

rem remove quotes from around java commands
for /f "usebackq delims=" %%i in ('!JAVA_CMD!') do set JAVA_CMD=%%~i
for /f "usebackq delims=" %%i in ('!LEIN_JAVA_CMD!') do set LEIN_JAVA_CMD=%%~i

if "x%JVM_OPTS%" == "x" set JVM_OPTS=%JAVA_OPTS%
goto RUN

:DownloadFile
rem parameters: TargetFileName Address
powershell -? >nul 2>&1
if NOT ERRORLEVEL 9009 (
    powershell -Command "& {param($a,$f) (new-object System.Net.WebClient).DownloadFile($a, $f)}" %~2 %~1
) else (
    wget >nul 2>&1
    if NOT ERRORLEVEL 9009 (
        wget --no-check-certificate -O %1 %2
    ) else (
        curl>nul 2>&1
        if ERRORLEVEL 9009 goto NO_HTTP_CLIENT
        curl --insecure -f -L -o  %1 %2
    )
)

goto EOF


:NO_LEIN_JAR
echo.
echo %LEIN_JAR% can not be found.
echo You can try running "lein self-install"
echo or change LEIN_JAR environment variable
echo or edit lein.bat to set appropriate LEIN_JAR path.
echo.
goto EOF

:NO_DEPENDENCIES
echo.
echo Leiningen is missing its dependencies.
echo Please run "lein bootstrap" in the leiningen-core/ directory
echo with a stable release of Leiningen. See CONTRIBUTING.md for details.
echo.
goto EOF

:SELF_INSTALL
if exist "%LEIN_JAR%" (
    echo %LEIN_JAR% already exists. Delete and retry.
    goto EOF
)

for %%f in ("%LEIN_JAR%") do set LEIN_INSTALL_DIR="%%~dpf"
if not exist %LEIN_INSTALL_DIR% mkdir %LEIN_INSTALL_DIR%

echo Downloading Leiningen now...

set LEIN_JAR_URL=https://leiningen.s3.amazonaws.com/downloads/leiningen-%LEIN_VERSION%-standalone.jar
call :DownloadFile "%LEIN_JAR%.pending" %LEIN_JAR_URL%
if ERRORLEVEL 1 (
    del "%LEIN_JAR%.pending" >nul 2>&1
    goto DOWNLOAD_FAILED
)
move /y "%LEIN_JAR%.pending" "%LEIN_JAR%"
goto EOF

:DOWNLOAD_FAILED
echo.
echo Failed to download %LEIN_JAR_URL%
if %SNAPSHOT% == YES echo See README.md for SNAPSHOT build instructions.
echo.
goto EOF


:UPGRADE
set LEIN_BAT=%~dp0%~nx0
echo The script at %LEIN_BAT% will be upgraded to the latest version in series %LEIN_VERSION%.
set /P ANSWER=Do you want to continue (Y/N)?
if /i {%ANSWER%}=={y}   goto YES_UPGRADE
if /i {%ANSWER%}=={yes} goto YES_UPGRADE
echo Aborted.
exit /B 1


:YES_UPGRADE
echo Downloading latest Leiningen batch script...

set LEIN_BAT_URL=https://raw.github.com/technomancy/leiningen/stable/bin/lein.bat
set TEMP_BAT=%~dp0temp-lein-%RANDOM%%RANDOM%.bat
call :DownloadFile "%LEIN_BAT%.pending" %LEIN_BAT_URL%
if ERRORLEVEL 1 (
    del "%LEIN_BAT%.pending" >nul 2>&1
    echo Failed to download %LEIN_BAT_URL%
    goto EOF
)
move /y "%LEIN_BAT%.pending" "%TEMP_BAT%"
echo.
echo Upgrading...
set LEIN_JAR=
call "%TEMP_BAT%" self-install
move /y "%TEMP_BAT%" "%LEIN_BAT%" && goto EOF
goto EOF


:NO_HTTP_CLIENT
echo.
echo ERROR: Neither PowerShell, Wget, or Curl could be found.
echo        Make sure at least one of these tools is installed
echo        and is in PATH. You can get them from URLs below:
echo.
echo PowerShell: "http://www.microsoft.com/powershell"
echo Wget:       "http://users.ugent.be/~bpuype/wget/"
echo Curl:       "http://curl.haxx.se/dlwiz/?type=bin&os=Win32&flav=-&ver=2000/XP"
echo.
goto EOF


:SET_LEIN_ROOT
set LEIN_ROOT=%~f1
goto EOF

:: Find directory containing filename supplied in first argument
:: looking in current directory, and looking up the parent
:: chain until we find it, or run out
:: returns result in %DIR_CONTAINING%
:: empty string if we don't find it
:FIND_DIR_CONTAINING_UPWARDS
set DIR_CONTAINING=%CD%
set LAST_DIR=

:LOOK_AGAIN
if "%DIR_CONTAINING%" == "%LAST_DIR%" (
    :: didn't find it
    set DIR_CONTAINING=
    goto EOF
)

if EXIST "%DIR_CONTAINING%\%1" (
    :: found it - use result in DIR_CONTAINING
    goto EOF
)

set LAST_DIR=%DIR_CONTAINING%
call :GET_PARENT_PATH "%DIR_CONTAINING%\.."
set DIR_CONTAINING=%PARENT_PATH%
goto LOOK_AGAIN

:GET_PARENT_PATH
set PARENT_PATH=%~f1
goto EOF


:RUN
:: We need to disable delayed expansion here because the %* variable
:: may contain bangs (as in test!). There may also be special
:: characters inside the TRAMPOLINE_FILE.
setLocal DisableDelayedExpansion

set "TRAMPOLINE_FILE=%TEMP%\lein-trampoline-%RANDOM%.bat"
del "%TRAMPOLINE_FILE%" 2>nul

"%LEIN_JAVA_CMD%" -client %LEIN_JVM_OPTS% ^
 -Dclojure.compile.path="%DIR_CONTAINING%/target/classes" ^
 -Dleiningen.original.pwd="%ORIGINAL_PWD%" ^
 -cp "%CLASSPATH%" clojure.main -m leiningen.core.main %*

if not exist "%TRAMPOLINE_FILE%" goto EOF
call "%TRAMPOLINE_FILE%"
del "%TRAMPOLINE_FILE%"
goto EOF

:EOF

