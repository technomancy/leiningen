@echo off

rem this script works after downloading Leiningen standalone jar
rem from http://repo.technomancy.us/
rem and copying it on %LEIN_JAR% path
rem There is needed also Clojure jar from http://build.clojure.org/
rem and it should be copied on %CLOJURE_JAR% path


set CLOJURE_VERSION=1.1.0
set LEIN_VERSION=1.1.0-SNAPSHOT
set LEIN_JAR=%HOMEDRIVE%%HOMEPATH%\.m2\repository\leiningen\leiningen\%LEIN_VERSION%\leiningen-%LEIN_VERSION%-standalone.jar
set CLOJURE_JAR=%HOMEDRIVE%%HOMEPATH%\.m2\repository\org\clojure\clojure\%CLOJURE_VERSION%\clojure-%CLOJURE_VERSION%.jar

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
if "%DEBUG%" == "" goto :RUN
echo CLASSPATH=%CLASSPATH%
rem ##################################################

:RUN
if "%ARGCOUNT%" == "2" goto :RUN_ARG2
if "%ARGCOUNT%" == "3" goto :RUN_ARG3
java -Xbootclasspath/a:"%CLOJURE_JAR%" -client -cp %CLASSPATH% clojure.main -e "(use 'leiningen.core) (-main \"%1\")"
goto :EOF

:RUN_ARG2
java -Xbootclasspath/a:"%CLOJURE_JAR%" -client -cp %CLASSPATH% clojure.main -e "(use 'leiningen.core) (-main \"%1\" \"%2\")"
goto :EOF

:RUN_ARG3
java -Xbootclasspath/a:"%CLOJURE_JAR%" -client -cp %CLASSPATH% clojure.main -e "(use 'leiningen.core) (-main \"%1\" \"%2\" \"%3\")"
goto :EOF

:EOF
