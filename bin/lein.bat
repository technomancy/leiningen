@echo off

rem WORKS ONLY with Leiningen 1.1.0 or newer

rem this script works after downloading Leiningen standalone jar
rem from http://github.com/downloads/technomancy/leiningen/leiningen-VERSION-standalone.jar
rem and copying it on %LEIN_JAR% path

rem optionally can be downloaded also Clojure jar 
rem (stable release, 1.1.0 or newer is recommended)
rem from http://build.clojure.org/releases/
rem and copied on %CLOJURE_JAR% path
rem this step is not necessary, because Leiningen standalone jar
rem contains Clojure as well

set CLOJURE_VERSION=1.2.0-beta1
set LEIN_VERSION=1.2.0

rem uncomment this and set paths explicitly 
rem set LEIN_JAR=C:\Documents and Settings\wojcirob\.m2\repository\leiningen\leiningen\%LEIN_VERSION%\leiningen-%LEIN_VERSION%-standalone.jar
rem set CLOJURE_JAR=C:\Documents and Settings\wojcirob\.m2\repository\org\clojure\clojure\%CLOJURE_VERSION%\clojure-%CLOJURE_VERSION%.jar


if "x%1" == "xself-install" goto NO_SELF_INSTALL

rem it is possible to set LEIN_JAR and CLOJURE_JAR variables manually
rem so we don't overwrite them
if "x%LEIN_JAR%" == "x" goto SET_LEIN
goto ARGS_HANDLING
if "x%CLOJURE_JAR%" == "x" goto SET_CLOJURE
goto ARGS_HANDLING

:SET_LEIN
set LEIN_JAR=%HOMEDRIVE%%HOMEPATH%\.m2\repository\leiningen\leiningen\%LEIN_VERSION%\leiningen-%LEIN_VERSION%-standalone.jar

:SET_CLOJURE
set CLOJURE_JAR=%HOMEDRIVE%%HOMEPATH%\.m2\repository\org\clojure\clojure\%CLOJURE_VERSION%\clojure-%CLOJURE_VERSION%.jar

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
java -Xbootclasspath/a:"%CLOJURE_JAR%" -client -cp %CLASSPATH% clojure.main -e "(use 'leiningen.core) (-main \"%1\")"
goto EOF

:RUN_ARG2
java -Xbootclasspath/a:"%CLOJURE_JAR%" -client -cp %CLASSPATH% clojure.main -e "(use 'leiningen.core) (-main \"%1\" \"%2\")"
goto EOF

:RUN_ARG3
java -Xbootclasspath/a:"%CLOJURE_JAR%" -client -cp %CLASSPATH% clojure.main -e "(use 'leiningen.core) (-main \"%1\" \"%2\" \"%3\")"
goto EOF

:RUN_REPL
%RLWRAP% java -client %JAVA_OPTS% -cp src;classes;%CLASSPATH% clojure.main %2 %3 %4
goto EOF

:NO_LEIN_JAR
echo.
echo "%LEIN_JAR%" can not be found.
echo Please change LEIN_JAR environment variable
echo or edit lein.bat to set appropriate LEIN_JAR path.
echo. 
goto EOF

:NO_SELF_INSTALL
echo.
echo SELF_INSTALL functionality is not available on Windows
echo Please download needed JARs manually:
echo 1. http://github.com/downloads/technomancy/leiningen/leiningen-%LEIN_VERSION%-standalone.jar
echo 2. clojure.jar from http://build.clojure.org/releases/
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
