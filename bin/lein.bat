@echo off

set LEIN_VERSION=1.3.1

if "x%1" == "xself-install" goto SELF_INSTALL
if "x%1" == "xupgrade"      goto NO_UPGRADE

rem it is possible to set LEIN_JAR variable manually
rem so we don't overwrite them
if "x%LEIN_JAR%" == "x" goto SET_LEIN
goto ARGS_HANDLING

:SET_LEIN
set LEIN_DIR=%~dp0
set LEIN_JAR=%LEIN_DIR%leiningen-%LEIN_VERSION%-standalone.jar

:ARGS_HANDLING
if not exist "%LEIN_JAR%" goto NO_LEIN_JAR


rem ##################################################
rem count number of command line arguments
rem
set ARGCOUNT=0
for %%a in (%*) do set /a ARGCOUNT+=1
rem ##################################################


rem ##################################################
rem add jars found under "lib" directory to CLASSPATH
rem

call :FIND_DIR_CONTAINING_UPWARDS project.clj

if "%DIR_CONTAINING%" neq "" cd "%DIR_CONTAINING%"

setLocal EnableDelayedExpansion
set CP="
for /R ./lib %%a in (*.jar) do (
   set CP=!CP!;%%a
)
set CP=!CP!"

set CLASSPATH="%LEIN_JAR%";%CP%;"%CLASSPATH%"
if "x%DEBUG%" == "x" goto RUN
echo CLASSPATH=%CLASSPATH%
rem ##################################################

:RUN
if "x%1" == "xrepl" goto RUN_REPL
if "%ARGCOUNT%" == "2" goto RUN_ARG2
if "%ARGCOUNT%" == "3" goto RUN_ARG3
java -client -cp %CLASSPATH% clojure.main -e "(use 'leiningen.core) (-main \"%1\")"
goto EOF

:RUN_ARG2
java -client -cp %CLASSPATH% clojure.main -e "(use 'leiningen.core) (-main \"%1\" \"%2\")"
goto EOF

:RUN_ARG3
java -client -cp %CLASSPATH% clojure.main -e "(use 'leiningen.core) (-main \"%1\" \"%2\" \"%3\")"
goto EOF

:RUN_REPL
%RLWRAP% java -client %JAVA_OPTS% -cp src;classes;%CLASSPATH% clojure.main %2 %3 %4
goto EOF

:NO_LEIN_JAR
echo.
echo "%LEIN_JAR%" can not be found.
echo You can try running "lein self-install"
echo or change LEIN_JAR environment variable
echo or edit lein.bat to set appropriate LEIN_JAR path.
echo. 
goto EOF

:SELF_INSTALL
if exist %LEIN_JAR% (
    echo %LEIN_JAR% already exists. Delete and retry.
    goto EOF
)
set HTTP_CLIENT=wget -O
wget>nul 2>&1
if ERRORLEVEL 9009 (
    curl>nul 2>&1
    if ERRORLEVEL 9009 goto NO_HTTP_CLIENT
    set HTTP_CLIENT=curl -f -L -o
)
set LEIN_DIR=%~dp0
set LEIN_JAR=%LEIN_DIR%leiningen-%LEIN_VERSION%-standalone.jar
set LEIN_JAR_URL=http://github.com/downloads/technomancy/leiningen/leiningen-%LEIN_VERSION%-standalone.jar
%HTTP_CLIENT% %LEIN_JAR% %LEIN_JAR_URL%
if ERRORLEVEL 1 (
    del %LEIN_JAR%>nul 2>&1
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

:EOF
