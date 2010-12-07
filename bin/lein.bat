@echo off

set LEIN_VERSION=1.4.1-SNAPSHOT

setLocal EnableDelayedExpansion

rem LEIN_JAR and LEIN_HOME variables can be set manually.

if "x%LEIN_JAR%" == "x" (
    set LEIN_DIR=%~dp0
    set LEIN_JAR=!LEIN_DIR!leiningen-%LEIN_VERSION%-standalone.jar
)

if "x%1" == "xself-install" goto SELF_INSTALL
if "x%1" == "xupgrade"      goto NO_UPGRADE


if "x%LEIN_HOME%" == "x" (
    set LEIN_HOME=%USERPROFILE%\.lein
)

set ORIGINAL_PWD=%CD%
call :FIND_DIR_CONTAINING_UPWARDS project.clj
if "%DIR_CONTAINING%" neq "" cd "%DIR_CONTAINING%"


set LEIN_PLUGINS="
for %%j in (".\lib\dev\*.jar") do (
    set LEIN_PLUGINS=!LEIN_PLUGINS!;%%~fj
)
set LEIN_PLUGINS=!LEIN_PLUGINS!"

set LEIN_USER_PLUGINS="
for %%j in ("%LEIN_HOME%\plugins\*.jar") do (
    set LEIN_USER_PLUGINS=!LEIN_USER_PLUGINS!;%%~fj
)
set LEIN_USER_PLUGINS=!LEIN_USER_PLUGINS!"

set CLASSPATH=%LEIN_USER_PLUGINS%;%LEIN_PLUGINS%;test;src;"%CLASSPATH%"

if exist "%~f0\..\..\src\leiningen\core.clj" (
    rem Running from source checkout.
    call :SET_LEIN_ROOT "%~f0\..\.."

    set LEIN_LIBS="
    for %%j in ("!LEIN_ROOT!\lib\*") do set LEIN_LIBS=!LEIN_LIBS!;%%~fj
    set LEIN_LIBS=!LEIN_LIBS!"

    if "x!LEIN_LIBS!" == "x" if not exist "%LEIN_JAR%" goto NO_DEPENDENCIES

    set CLASSPATH="!LEIN_ROOT!\src";"!LEIN_ROOT!\resources";!LEIN_LIBS!;%CLASSPATH%;"%LEIN_JAR%"
) else (
    rem Not running from a checkout.
    if not exist "%LEIN_JAR%" goto NO_LEIN_JAR
    set CLASSPATH="%LEIN_JAR%";%CLASSPATH%
)

if not "x%DEBUG%" == "x" echo CLASSPATH=%CLASSPATH%
rem ##################################################

if not "x%INSIDE_EMACS%" == "x" goto SKIP_JLINE
if "x%1" == "xrepl"             goto SET_JLINE
if "x%1" == "xinteractive"      goto SET_JLINE
if "x%1" == "xint"              goto SET_JLINE
goto :SKIP_JLINE

:SET_JLINE
set JLINE=jline.ConsoleRunner
:SKIP_JLINE

if "x%JAVA_CMD%" == "x" set JAVA_CMD="java"
set CLOJURE_JAR=%USERPROFILE%\.m2\repository\org\clojure\clojure\1.2.0\clojure-1.2.0.jar
goto RUN


:NO_LEIN_JAR
echo.
echo "%LEIN_JAR%" can not be found.
echo You can try running "lein self-install"
echo or change LEIN_JAR environment variable
echo or edit lein.bat to set appropriate LEIN_JAR path.
echo.
goto EOF

:NO_DEPENDENCIES
echo.
echo Leiningen is missing its dependencies.
echo Please see "Building" in the README.
echo.
goto EOF

:SELF_INSTALL
if exist "%LEIN_JAR%" (
    echo %LEIN_JAR% already exists. Delete and retry.
    goto EOF
)
set HTTP_CLIENT=wget --no-check-certificate -O
wget>nul 2>&1
if ERRORLEVEL 9009 (
    curl>nul 2>&1
    if ERRORLEVEL 9009 goto NO_HTTP_CLIENT
    set HTTP_CLIENT=curl --insecure -f -L -o
)
set LEIN_JAR_URL=https://github.com/downloads/technomancy/leiningen/leiningen-%LEIN_VERSION%-standalone.jar
%HTTP_CLIENT% "%LEIN_JAR%" %LEIN_JAR_URL%
if ERRORLEVEL 1 (
    del "%LEIN_JAR%">nul 2>&1
    goto DOWNLOAD_FAILED
)
goto EOF

:DOWNLOAD_FAILED
echo.
echo *** DOWNLOAD FAILED! Check URL/Version. ***
echo.
goto EOF

:NO_HTTP_CLIENT
echo.
echo ERROR: Wget/Curl not found. Make sure at least either of Wget and Curl is
echo        installed and is in PATH. You can get them from URLs below:
echo.
echo Wget: "http://users.ugent.be/~bpuype/wget/"
echo Curl: "http://curl.haxx.se/dlwiz/?type=bin&os=Win32&flav=-&ver=2000/XP"
echo.
goto EOF

:NO_UPGRADE
echo.
echo Upgrade feature is not available on Windows. Please edit the value of
echo variable LEIN_VERSION in file %~f0
echo then run "lein self-install".
echo.
goto EOF


:SET_LEIN_ROOT
set LEIN_ROOT=%~f1
goto EOF

rem Find directory containing filename supplied in first argument
rem looking in current directory, and looking up the parent
rem chain until we find it, or run out
rem returns result in %DIR_CONTAINING%
rem empty string if we don't find it
:FIND_DIR_CONTAINING_UPWARDS
set DIR_CONTAINING=%CD%
set LAST_DIR=

:LOOK_AGAIN
if "%DIR_CONTAINING%" == "%LAST_DIR%" (
    rem didn't find it
    set DIR_CONTAINING=
    goto :EOF
)

if EXIST "%DIR_CONTAINING%\%1" (
    rem found it - use result in DIR_CONTAINING
    goto :EOF
)

set LAST_DIR=%DIR_CONTAINING%
call :GET_PARENT_PATH "%DIR_CONTAINING%\.."
set DIR_CONTAINING=%PARENT_PATH%
goto :LOOK_AGAIN

:GET_PARENT_PATH
set PARENT_PATH=%~f1
goto :EOF


:RUN
rem Need to disable delayed expansion because the %* variable
rem may contain bangs (as in test!).
setLocal DisableDelayedExpansion

%JAVA_CMD% -client %JAVA_OPTS% -Xbootclasspath/a:"%CLOJURE_JAR%" ^
 -Dleiningen.original.pwd="%ORIGINAL_PWD%" ^
 -cp %CLASSPATH% %JLINE% clojure.main -e "(use 'leiningen.core)(-main)" NUL %*
goto EOF

:EOF
