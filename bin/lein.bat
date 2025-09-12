@echo off

setLocal EnableExtensions EnableDelayedExpansion

set LEIN_VERSION=2.12.0

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
:: Only set LEIN_JAR manually if you know what you are doing.
:: Having LEIN_JAR pointing to one version of Leiningen as well as
:: having a different version in PATH has been known to cause problems.

if "x%LEIN_HOME%" == "x" (
    set LEIN_HOME=!USERPROFILE!\.lein
)
SET RC=1

if "x%LEIN_JAR%" == "x" set "LEIN_JAR=!LEIN_HOME!\self-installs\leiningen-!LEIN_VERSION!-standalone.jar"

if "%1" == "self-install" goto SELF_INSTALL
if "%1" == "upgrade"      goto UPGRADE
if "%1" == "downgrade"    goto UPGRADE

if not exist "%~dp0..\src\leiningen\version.clj" goto RUN_NO_CHECKOUT

    :: Running from source checkout.
    call :SET_LEIN_ROOT "%~dp0.."


	set "bootstrapfile=!LEIN_ROOT!\leiningen-core\.lein-bootstrap"
	rem in .lein-bootstrap there is only one line where each path is concatenated to each other via a semicolon, there's no semicolon at the end
	rem each path is NOT inside double quotes and may contain spaces (even semicolons but this is not supported here) in their names, 
	rem  but they won't/cannot contain double quotes " or colons :  in their names (at least on windows it's not allowed/won't work)
	
	rem tested when folders contain spaces and when LEIN_ROOT contains semicolon
	
	
	if not "x%DEBUG%" == "x" echo LEIN_ROOT=!LEIN_ROOT!
	
	rem if not "%LEIN_ROOT:;=%" == "%LEIN_ROOT%" (

	
	rem oddly enough /G:/ should've worked but doesn't where / they say it's console
	rem findstr is C:\Windows\System32\findstr.exe
	echo.!LEIN_ROOT! | findstr /C:";" >nul 2>&1 && (
		rem aka errorlevel is 0 aka the string ";" was found
		echo Your folder structure !LEIN_ROOT! contains at least one semicolon in its name
		echo This is not allowed and would break things with the generated bootstrap file
		echo Please correct this by renaming the folders to not contain semicolons in their name
		del !bootstrapfile! >nul 2>&1
		echo You'll also have to recreate the bootstrap file just to be sure it has semicolon-free names inside
		echo the bootstrap file ^(which was just deleted^) is: !bootstrapfile!
		echo  and the info on how to do that is:
		goto RUN_BOOTSTRAP
	)

	if not exist !bootstrapfile! goto NO_DEPENDENCIES

	findstr \^" "!bootstrapfile!" >nul 2>&1
	if errorlevel 1 goto PARSE_BOOTSTRAPFILE
		echo double quotes detected inside file: !bootstrapfile!
		echo this should not be happening
		goto RUN_BOOTSTRAP

:PARSE_BOOTSTRAPFILE
rem will proceed to set LEIN_LIBS and surround each path from bootstrap file in double quotes and separate it from others with a semicolon
rem the paths inside the bootstrap file do not already contain double quotes but may contain spaces
	rem note worthy: the following won't work due to a hard 1022bytes limit truncation in the variable that was set
	rem set /p LEIN_LIBS=<!bootstrapfile!
	rem so this will work instead:
	rem for /f "usebackq delims=" %%j in (!bootstrapfile!) do set LEIN_LIBS=%%j
	rem just  set LEIN_LIBS="%%j"  is uglier/hacky but would also work here instead of the below:
	for /f "usebackq delims=" %%j in ("!bootstrapfile!") do (
		set tmpline=%%j
		call :PROCESSPATH
	)

	rem remove trailing semicolon, if any
	if "!LEIN_LIBS:~-1!x" == ";x" SET LEIN_LIBS=!LEIN_LIBS:~0,-1!
	if not "x%DEBUG%" == "x" echo LEIN_LIBS=!LEIN_LIBS!

    if "x!LEIN_LIBS!" == "x" goto NO_DEPENDENCIES


	rem semicolons in pathes are not supported, spaces are supported by quoting CLASSPATH as a whole
	rem (no end semicolon required)
    set CLASSPATH=!LEIN_LIBS!;!LEIN_ROOT!\src;!LEIN_ROOT!\resources

    :: Apply context specific CLASSPATH entries
    if exist "%~dp0..\.lein-classpath" (
        for /f "tokens=* delims= " %%i in ("%~dp0..\.lein-classpath") do (
            set CONTEXT_CP=%%i
        )

        if NOT "x!CONTEXT_CP!"=="x" (
            set CLASSPATH=!CONTEXT_CP!;!CLASSPATH!
        )
    )
    goto SETUP_JAVA

:RUN_NO_CHECKOUT

    :: Not running from a checkout.
    if not exist "%LEIN_JAR%" goto NO_LEIN_JAR
    set CLASSPATH=%LEIN_JAR%
  
    if exist ".lein-classpath" (
        for /f "tokens=* delims= " %%i in (.lein-classpath) do (
            set CONTEXT_CP=%%i
        )

        if NOT "x!CONTEXT_CP!"=="x" (
            set CLASSPATH=!CONTEXT_CP!;!CLASSPATH!
        )
    )

:SETUP_JAVA

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
set LAST_HTTP_CLIENT=
rem parameters: TargetFileName Address
if "x%HTTP_CLIENT%" == "x" goto TRY_POWERSHELL
    %HTTP_CLIENT% %1 %2
    SET RC=%ERRORLEVEL%
    goto EXITRC

:TRY_POWERSHELL
call powershell -? >nul 2>&1
if NOT ERRORLEVEL 0 goto TRY_WGET
    set LAST_HTTP_CLIENT=powershell
    rem By default: Win7 = PS2, Win 8.0 = PS3 (maybe?), Win 8.1 = PS4, Win10 = PS5
    powershell -Command "& {param($a,$f) if (($PSVersionTable.PSVersion | Select-Object -ExpandProperty Major) -lt 4) { exit 111; } else { $client = New-Object System.Net.WebClient; [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $client.Proxy.Credentials = [System.Net.CredentialCache]::DefaultNetworkCredentials; $client.DownloadFile($a, $f); }}" ""%2"" ""%1""
    SET RC=%ERRORLEVEL%
    goto EXITRC

:TRY_WGET
call wget --help >nul 2>&1
if NOT ERRORLEVEL 0 goto TRY_CURL
    set LAST_HTTP_CLIENT=wget
    call wget -O %1 %2
    SET RC=%ERRORLEVEL%
    goto EXITRC

:TRY_CURL
call curl --help >nul 2>&1
if NOT ERRORLEVEL 0 GOTO NO_HTTP_CLIENT
    rem We set CURL_PROXY to a space character below to pose as a no-op argument
    set LAST_HTTP_CLIENT=curl
    set CURL_PROXY= 
    if NOT "x%HTTPS_PROXY%" == "x" set CURL_PROXY="-x %HTTPS_PROXY%"
    call curl %CURL_PROXY% -f -L -o  %1 %2
    SET RC=%ERRORLEVEL%
    goto EXITRC

:NO_LEIN_JAR
echo.
echo %LEIN_JAR% can not be found.
echo You can try running "lein self-install"
echo or change LEIN_JAR environment variable
echo or edit lein.bat to set appropriate LEIN_JAR path.
echo.
goto EXITRC

:NO_DEPENDENCIES
echo.
echo Leiningen is missing its dependencies.
:RUN_BOOTSTRAP
echo Please run "lein bootstrap" in the leiningen-core/ directory
echo with a stable release of Leiningen. See CONTRIBUTING.md for details.
echo.
goto EXITRC

:SELF_INSTALL
if exist "%LEIN_JAR%" (
    echo %LEIN_JAR% already exists. Delete and retry.
    goto EXITRC
)

for %%f in ("%LEIN_JAR%") do set LEIN_INSTALL_DIR="%%~dpf"
if not exist %LEIN_INSTALL_DIR% mkdir %LEIN_INSTALL_DIR%

echo Downloading Leiningen now...

set LEIN_JAR_URL=https://github.com/technomancy/leiningen/releases/download/%LEIN_VERSION%/leiningen-%LEIN_VERSION%-standalone.jar
call :DownloadFile "%LEIN_JAR%.pending" "%LEIN_JAR_URL%"
SET RC=%ERRORLEVEL%
if not %RC% == 0 goto DOWNLOAD_FAILED
if not exist "%LEIN_JAR%.pending" goto DOWNLOAD_FAILED
move /y "%LEIN_JAR%.pending" "%LEIN_JAR%" >nul 2>&1
SET RC=%ERRORLEVEL%
goto EXITRC

:DOWNLOAD_FAILED
SET RC=3
if "%ERRORLEVEL%" == "111" (
    echo.
    echo You seem to be using an old version of PowerShell that
    echo can't download files via TLS 1.2.
    echo Please upgrade your PowerShell to at least version 4.0, e.g. via
    echo https://www.microsoft.com/en-us/download/details.aspx?id=50395
    echo.
    echo Alternatively you can manually download
    echo %LEIN_JAR_URL%
    echo and save it as
    echo %LEIN_JAR%
    echo.
    echo If you have "curl" or "wget" you can try setting the HTTP_CLIENT
    echo variable, but the TLS problem might still persist.
    echo.
    echo   a^) set HTTP_CLIENT=wget -O
    echo   b^) set HTTP_CLIENT=curl -f -L -o
    echo.
    echo NOTE: Make sure to *not* add double quotes when setting the value
    echo       of HTTP_CLIENT
    goto EXITRC
)
SET RC=3
del "%LEIN_JAR%.pending" >nul 2>&1
echo.
echo Failed to download %LEIN_JAR_URL%
echo.
echo It is possible that the download failed due to "powershell", 
echo "curl" or "wget"'s inability to retrieve GitHub's security certificate.
echo.

if "%LAST_HTTP_CLIENT%" == "powershell" (
  echo The PowerShell failed to download the latest Leiningen version.
  echo Try to use "curl" or "wget" to download Leiningen by setting up
  echo the HTTP_CLIENT environment variable with one of the following 
  echo values:
  echo.
  echo   a^) set HTTP_CLIENT=wget -O
  echo   b^) set HTTP_CLIENT=curl -f -L -o
  echo.
  echo NOTE: Make sure to *not* add double quotes when setting the value
  echo       of HTTP_CLIENT
)

if "%LAST_HTTP_CLIENT%" == "curl" (
  echo Curl failed to download the latest Leiningen version.
  echo Try to use "wget" to download Leiningen by setting up
  echo the HTTP_CLIENT environment variable with one of the following 
  echo values:
  echo.
  echo   a^) set HTTP_CLIENT=wget -O
  echo.
  echo NOTE: Make sure to *not* add double quotes when setting the value
  echo       of HTTP_CLIENT
  echo. 
  echo If neither curl nor wget can download Leiningen, please seek
  echo for help on Leiningen's GitHub project issues page.
)

if "%LAST_HTTP_CLIENT%" == "wget" (
  echo Curl failed to download the latest Leiningen version.
  echo Try to use "wget" to download Leiningen by setting up
  echo the HTTP_CLIENT environment variable with one of the following 
  echo values:
  echo.
  echo.   a^) set HTTP_CLIENT=curl -f -L -o
  echo.
  echo NOTE: make sure *not* to add double quotes to set the value of 
  echo       HTTP_CLIENT
  echo. 
  echo If neither curl nor wget can download Leiningen, please seek
  echo for help on Leiningen's GitHub project issues page.
)

if %SNAPSHOT% == YES echo See README.md for SNAPSHOT build instructions.
echo.
goto EOF


:UPGRADE
set LEIN_BAT=%~dp0%~nx0
set TARGET_VERSION=%2
if "x%2" == "x" set TARGET_VERSION=stable
echo The script at %LEIN_BAT% will be upgraded to the latest %TARGET_VERSION% version.
set /P ANSWER=Do you want to continue (Y/N)?
if /i {%ANSWER%}=={y}   goto YES_UPGRADE
if /i {%ANSWER%}=={yes} goto YES_UPGRADE
echo Aborted.
goto EXITRC


:YES_UPGRADE
echo Downloading latest Leiningen batch script...

set LEIN_BAT_URL=https://github.com/technomancy/leiningen/raw/%TARGET_VERSION%/bin/lein.bat
set TEMP_BAT=%~dp0temp-lein-%RANDOM%%RANDOM%.bat
call :DownloadFile "%LEIN_BAT%.pending" "%LEIN_BAT_URL%"
if ERRORLEVEL 0 goto EXEC_UPGRADE
    del "%LEIN_BAT%.pending" >nul 2>&1
    echo Failed to download %LEIN_BAT_URL%
    goto EXITRC
:EXEC_UPGRADE
move /y "%LEIN_BAT%.pending" "%TEMP_BAT%" >nul 2>&1
echo.
echo Upgrading...
set LEIN_JAR=
call "%TEMP_BAT%" self-install
(
   rem This is self-modifying batch code. Use brackets to pre-load the exit command.
   rem This way, script execution does not depend on whether the replacement script
   rem has that command at the *very same* file position as the calling batch file.
   move /y "%TEMP_BAT%" "%LEIN_BAT%" >nul 2>&1
   exit /B %ERRORLEVEL%
)

:NO_HTTP_CLIENT
echo.
echo ERROR: Neither PowerShell, Wget, or Curl could be found.
echo        Make sure at least one of these tools is installed
echo        and is in PATH. You can get them from URLs below:
echo.
echo PowerShell: "http://www.microsoft.com/powershell"

rem echo Wget:       "http://users.ugent.be/~bpuype/wget/"
rem Note: Stale URL. HTTP 404.
rem Alternative: wget64.exe compiled by J. Simoncic, rename to wget.exe
rem MD5 1750c130c5daca8b347d3f7e34824c9b
rem Check: https://www.virustotal.com/en/file/abf507f8240ed41aac74c9df6de558c88c2f11d7770f02.8.4-SNAPSHOT5f1cc544b9c08b/analysis/
echo Wget:       "https://eternallybored.org/misc/wget/"

echo Curl:       "http://curl.haxx.se/dlwiz/?type=bin&os=Win32&flav=-&ver=2000/XP"
echo.
goto EXITRC


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
del "%TRAMPOLINE_FILE%" >nul 2>&1

set ERRORLEVEL=
set RC=0
"%LEIN_JAVA_CMD%" -client %LEIN_JVM_OPTS% ^
 -Dfile.encoding=UTF-8 ^
 -Dclojure.compile.path="%DIR_CONTAINING%/target/classes" ^
 -Dleiningen.original.pwd="%ORIGINAL_PWD%" ^
 -cp "%CLASSPATH%" clojure.main -m leiningen.core.main %*
SET RC=%ERRORLEVEL%
if not %RC% == 0 goto EXITRC

if not exist "%TRAMPOLINE_FILE%" goto EOF
call "%TRAMPOLINE_FILE%"
del "%TRAMPOLINE_FILE%" >nul 2>&1
goto EOF


:PROCESSPATH
rem will surround each path with double quotes before appending it to LEIN_LIBS
	for /f "tokens=1* delims=;" %%a in ("%tmpline%") do (
		set LEIN_LIBS=!LEIN_LIBS!"%%a";
		set tmpline=%%b
	)
	if not "%tmpline%" == "" goto PROCESSPATH
	goto EOF

:EXITRC
exit /B %RC%

:EOF

