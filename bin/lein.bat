@echo off

rem this script works after downloading Leiningen standalone jar
rem from http://repo.technomancy.us/
rem and copying it on %LEIN_JAR% path
rem There is needed also Clojure jar from http://build.clojure.org/
rem and it should be copied on %CLOJURE_JAR% path


set CLOJURE_VERSION=1.1.0
set LEIN_VERSION=1.1.0

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
if not exist "%CLOJURE_JAR%" goto NO_CLOJURE_JAR


rem ##################################################
rem count number of command line arguments
rem
set ARGCOUNT=0
for %%a in (%*) do set /a ARGCOUNT+=1
rem ##################################################


rem ##################################################
rem add jars found under "lib" directory to CLASSPATH
rem
setLocal EnableDelayedExpansion
set CLASSPATH="
for /R ./lib %%a in (*.jar) do (
   set CLASSPATH=!CLASSPATH!;%%a
)
set CLASSPATH=!CLASSPATH!"

set CLASSPATH=%CLASSPATH%;"%LEIN_JAR%"
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
java -Xbootclasspath/a:"%CLOJURE_JAR%" -client -cp src;classes;%CLASSPATH% clojure.main %2 %3 %4
goto EOF

:NO_LEIN_JAR
echo.
echo "%LEIN_JAR%" can not be found.
echo Please change LEIN_JAR environment variable
echo or edit lein.bat to set appropriate LEIN_JAR path.
echo. 
goto EOF

:NO_CLOJURE_JAR
echo.
echo "%CLOJURE_JAR%" can not be found.
echo Please change CLOJURE_JAR environment variable
echo or edit lein.bat to set appropriate CLOJURE_JAR path.
echo. 
goto EOF

:NO_SELF_INSTALL
echo.
echo SELF_INSTALL functionality is not available on Windows
echo Please download needed JARs manually:
echo 1. leiningen-%LEIN_VERSION%-standalone.jar from http://repo.technomancy.us/
echo 2. clojure.jar from http://build.clojure.org/
echo. 
goto EOF

:EOF
