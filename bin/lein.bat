@echo off

set LEIN_VERSION=1.7.1

setLocal EnableExtensions EnableDelayedExpansion

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
    if exist "%CD%\.lein" (
        if /I NOT "%CD%"=="%USERPROFILE%" echo Running in bundled mode.
        set LEIN_HOME=%CD%\.lein
    ) else (
        set LEIN_HOME=%USERPROFILE%\.lein
    )
)

if "x%LEIN_JAR%" == "x" set LEIN_JAR="!LEIN_HOME!\self-installs\leiningen-!LEIN_VERSION!-standalone.jar"

if "%1" == "self-install" goto SELF_INSTALL
if "%1" == "upgrade"      goto NO_UPGRADE

set DEV_PLUGINS="
for %%j in (".\lib\dev\*.jar") do (
    set DEV_PLUGINS=!DEV_PLUGINS!;%%~fj
)
set DEV_PLUGINS=!DEV_PLUGINS!"

call :BUILD_UNIQUE_USER_PLUGINS
set CLASSPATH="%CLASSPATH%";%DEV_PLUGINS%;%UNIQUE_USER_PLUGINS%;test;src;resources

:: Apply context specific CLASSPATH entries
set CONTEXT_CP=
if exist ".lein-classpath" set /P CONTEXT_CP=<.lein-classpath
if NOT "%CONTEXT_CP%"=="" set CLASSPATH="%CONTEXT_CP%";%CLASSPATH%

if exist "%~f0\..\..\src\leiningen\core.clj" (
    :: Running from source checkout.
    call :SET_LEIN_ROOT "%~f0\..\.."

    set LEIN_LIBS="
    for %%j in ("!LEIN_ROOT!\lib\*") do set LEIN_LIBS=!LEIN_LIBS!;%%~fj
    set LEIN_LIBS=!LEIN_LIBS!"

    if "x!LEIN_LIBS!" == "x" if not exist %LEIN_JAR% goto NO_DEPENDENCIES

    set CLASSPATH=%CLASSPATH%;!LEIN_LIBS!;"!LEIN_ROOT!\src";"!LEIN_ROOT!\resources";%LEIN_JAR%
) else (
    :: Not running from a checkout.
    if not exist %LEIN_JAR% goto NO_LEIN_JAR
    set CLASSPATH=%CLASSPATH%;%LEIN_JAR%
)

if not "x%DEBUG%" == "x" echo CLASSPATH=%CLASSPATH%
:: ##################################################

if not "x%INSIDE_EMACS%" == "x" goto SKIP_JLINE
if "%1" == "repl"             goto SET_JLINE
if "%1" == "interactive"      goto SET_JLINE
if "%1" == "int"              goto SET_JLINE
goto SKIP_JLINE

:SET_JLINE
set JLINE=jline.ConsoleRunner
:SKIP_JLINE

if "x%JAVA_CMD%" == "x" set JAVA_CMD="java"
if "x%JVM_OPTS%" == "x" set JVM_OPTS=%JAVA_OPTS%
set CLOJURE_JAR=%USERPROFILE%\.m2\repository\org\clojure\clojure\1.2.1\clojure-1.2.1.jar
goto RUN


:: Builds a classpath fragment consisting of user plugins
:: which aren't already present as a dev dependency.
:BUILD_UNIQUE_USER_PLUGINS
call :BUILD_PLUGIN_SEARCH_STRING %DEV_PLUGINS%
set UNIQUE_USER_PLUGINS="
for %%j in ("%LEIN_HOME%\plugins\*.jar") do (
    call :MAKE_SEARCH_TOKEN %%~nj
    echo %PLUGIN_SEARCH_STRING%|findstr ;!SEARCH_TOKEN!; > NUL
    if !ERRORLEVEL! == 1 (
        set UNIQUE_USER_PLUGINS=!UNIQUE_USER_PLUGINS!;%%~fj
    )
)
set UNIQUE_USER_PLUGINS=!UNIQUE_USER_PLUGINS!"
goto EOF

:: Builds a search string to match against when ensuring
:: plugin uniqueness.
:BUILD_PLUGIN_SEARCH_STRING
for %%j in (".\lib\dev\*.jar") do (
    call :MAKE_SEARCH_TOKEN %%~nj
    set PLUGIN_SEARCH_STRING=!PLUGIN_SEARCH_STRING!;!SEARCH_TOKEN!
)
set PLUGIN_SEARCH_STRING=%PLUGIN_SEARCH_STRING%;
goto EOF

:: Takes a jar filename and returns a reversed jar name without version.
:: Example: lein-multi-0.1.1.jar -> itlum-niel
:MAKE_SEARCH_TOKEN
call :REVERSE_STRING %1
call :STRIP_VERSION !RSTRING!
set SEARCH_TOKEN=!VERSIONLESS!
goto EOF

:: Reverses a string.
:REVERSE_STRING
set NUM=0
set INPUTSTR=%1
set RSTRING=
:REVERSE_STRING_LOOP
call set TMPCHR=%%INPUTSTR:~%NUM%,1%%%
set /A NUM+=1
if not "x%TMPCHR%" == "x" (
    set RSTRING=%TMPCHR%%RSTRING%
    goto REVERSE_STRING_LOOP
)
goto EOF

:: Takes a string and removes everything from the beginning up to
:: and including the first dash character.
:STRIP_VERSION
set INPUT=%1
for /F "delims=- tokens=1*" %%a in ("%INPUT%") do set VERSIONLESS=%%b
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
echo Please see "Building" in the README.
echo.
goto EOF

:SELF_INSTALL
if exist %LEIN_JAR% (
    echo %LEIN_JAR% already exists. Delete and retry.
    goto EOF
)
for %%f in (%LEIN_JAR%) do set LEIN_INSTALL_DIR="%%~dpf"
if not exist %LEIN_INSTALL_DIR% mkdir %LEIN_INSTALL_DIR%

echo Downloading Leiningen now...

set HTTP_CLIENT=wget --no-check-certificate -O
wget>nul 2>&1
if ERRORLEVEL 9009 (
    curl>nul 2>&1
    if ERRORLEVEL 9009 goto NO_HTTP_CLIENT
    set HTTP_CLIENT=curl --insecure -f -L -o
)
set LEIN_JAR_URL=https://github.com/downloads/technomancy/leiningen/leiningen-%LEIN_VERSION%-standalone.jar
%HTTP_CLIENT% %LEIN_JAR% %LEIN_JAR_URL%
if ERRORLEVEL 1 (
    del %LEIN_JAR%>nul 2>&1
    goto DOWNLOAD_FAILED
)
goto EOF

:DOWNLOAD_FAILED
echo.
echo Failed to download %LEIN_JAR_URL%
if %SNAPSHOT% == YES echo See README.md for SNAPSHOT build instructions.
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

if "%1" == "trampoline" goto RUN_TRAMPOLINE else goto RUN_NORMAL

:RUN_TRAMPOLINE
set "TRAMPOLINE_FILE=%TEMP%\lein-trampoline-%RANDOM%.bat"

%JAVA_CMD% -client %LEIN_JVM_OPTS% -Xbootclasspath/a:"%CLOJURE_JAR%" ^
 -Dleiningen.original.pwd="%ORIGINAL_PWD%" ^
 -Dleiningen.trampoline-file="%TRAMPOLINE_FILE%" ^
 -cp %CLASSPATH% %JLINE% clojure.main -e "(use 'leiningen.core)(-main)" NUL %*

if not exist "%TRAMPOLINE_FILE%" goto EOF
call "%TRAMPOLINE_FILE%"
del "%TRAMPOLINE_FILE%"
goto EOF

:RUN_NORMAL
%JAVA_CMD% -client %LEIN_JVM_OPTS% -Xbootclasspath/a:"%CLOJURE_JAR%" ^
 -Dleiningen.original.pwd="%ORIGINAL_PWD%" ^
 -cp %CLASSPATH% %JLINE% clojure.main -e "(use 'leiningen.core)(-main)" NUL %*


:EOF
