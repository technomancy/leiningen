<#
.Synopsis
    Leiningen bootstrap.

.Parameter Command
    The command to pass to leiningen.

.Notes
    This is a very literal port of lein.bat to PowerShell.

    TODO:
    - Determine which (if any) environment variables are used from within Leiningen/Clojure
      and convert the rest to local- or script-scoped variables.
    - Probably should parse the version from the newest .jar filename, rather than hard-code
      it and search for the .jar file from it.
    - Further reduce/simplify/refactor away from batch-file idioms toward idiomatic PowerShell.

.Link
    https://leiningen.org/
#>

#require -version 3
[CmdletBinding(SupportsShouldProcess=$true)] Param(
[Parameter(Position=0,ValueFromRemainingArguments=$true)][string[]]$Command = 'help'
)

function Set-ParentLocation([string]$file)
{
    for($dir = [IO.DirectoryInfo]"$PWD"; $dir.Parent; $dir = $dir.Parent)
    {
        if(Test-Path (Join-Path $dir.FullName $file) -PathType Leaf) { cd $dir }
    }
}

function Initialize-Environment
{
    $env:LEIN_VERSION = '2.8.1'
    $env:SNAPSHOT = if($env:LEIN_VERSION -like '*-SNAPSHOT'){'YES'}else{'NO'} #TODO: Still needed?
    $env:ORIGINAL_PWD = $PWD -replace '\\$','\\'
    Set-ParentLocation project.clj
    if(!$env:LEIN_HOME) {$env:LEIN_HOME = "$env:USERPROFILE\.lein"}
    if(!$env:LEIN_JAR) {$env:LEIN_JAR = "$env:LEIN_HOME\self-installs\leiningen-$env:LEIN_VERSION-standalone.jar"}
    if(!([Net.WebRequest]::DefaultWebProxy.IsBypassed('https://github.com/')))
    {
        $proxy = [Net.WebRequest]::DefaultWebProxy.GetProxy('https://github.com/')
        Write-Verbose "Using proxy: $proxy"
        $Script:PSBoundParameters = @{
            'Invoke-WebRequest:Proxy' = $proxy
            'Invoke-WebRequest:ProxyUseDefaultCredentials' = $true
        }
    }
}

function Use-ClassPath([string]$Value)
{
    $env:CLASSPATH =
        if(!(Test-Path .lein-classpath -PathType Leaf)) {$Value}
        else {"$(gc .lein-classpath |? {$_} |select -Last 1);$Value"}
}

function Initialize-Binary
{
    if(!(Test-Path $env:LEIN_JAR -PathType Leaf))
    {throw "$env:LEIN_JAR cannot be found. Try running 'lein self-install' or change the LEIN_JAR environment variable."}
    Use-ClassPath $env:LEIN_JAR
}

function Initialize-Source
{
    $env:LEIN_ROOT = $PSScriptRoot
    $env:bootstrapfile = "$env:LEIN_ROOT\leiningen-core\.lein-bootstrap"
    Write-Verbose "LEIN_ROOT=$env:LEIN_ROOT"
    if($env:bootstrapfile -like '*;*') #TODO: Still important?
    {throw "bootstrap file ($env:bootstrapfile) should not contain semicolons!"}
    if(!(Test-Path $env:bootstrapfile -PathType Leaf)) 
    {throw @'
Leiningen is missing its dependencies. Run 'lein bootstrap' in the leiningen-core/ directory with a stable release.
See CONTRIBUTING.md for details.
'@}
    if((Get-Content $env:bootstrapfile -raw) -like '*"*')
    {throw "Double quotes detected inside bootstrap file $env:bootstrapfile!?"}
    $env:LEIN_LIBS = (Get-Content $env:bootstrapfile |% {$_ -split ';'} |% {"$_"}) -join ';'
    Write-Verbose "LEIN_LIBS=$env:LEIN_LIBS"
    Use-ClassPath "$env:LEIN_LIBS;$env:LEIN_ROOT\src;$env:LEIN_ROOT\resources"
}

function Install-Self
{
    if(Test-Path $env:LEIN_JAR -PathType Leaf) {throw "$env:LEIN_JAR already exists. Delete and retry."}
    $jardir = ([IO.FileInfo]$env:LEIN_JAR).Directory.FullName
    if(!(Test-Path $jardir -PathType Container)) {mkdir $jardir |Out-Null}
    @{ # splatting Invoke-WebRequest due to long URI
        Uri = "https://github.com/technomancy/leiningen/releases/download/$env:LEIN_VERSION/leiningen-$env:LEIN_VERSION-standalone.zip"
        OutFile = $env:LEIN_JAR
    } |% {Write-Progress 'Install-Self' $_.Uri -CurrentOperation "Downloading to $env:LEIN_JAR" ; Invoke-WebRequest @_}
    Write-Progress 'Install-Self' -Completed
}

function Update-Self
{
    $targetVersion = if($Command.Length -gt 1) {$Command[1]} else {'stable'}
    if(!$PSCmdlet.ShouldProcess($PSCommandPath,"upgrade to $targetVersion")) {throw 'Cancelled'}
    @{ # splatting Invoke-WebRequest due to long URI
        Uri = "https://github.com/technomancy/leiningen/raw/$targetVersion/bin/lein.cmd"
        OutFile = "$PSScriptRoot\lein.cmd.pending"
    } |% {Write-Progress 'Update-Self' $_.Uri -CurrentOperation "Downloading to $PSScriptRoot\lein.cmd.pending" ; Invoke-WebRequest @_}
    @{ # splatting Invoke-WebRequest due to long URI
        Uri = "https://github.com/technomancy/leiningen/raw/$targetVersion/bin/lein.ps1"
        OutFile = "$PSCommandPath.pending"
    } |% {Write-Progress 'Update-Self' $_.Uri -CurrentOperation "Downloading to $PSCommandPath.pending" -PercentComplete 50 ; Invoke-WebRequest @_}
    Write-Progress 'Update-Self' -Completed
    Install-Self
    Register-EngineEvent -SourceIdentifier PowerShell.Exiting -SupportEvent -Action {
        rm "$PSScriptRoot\lein.cmd"
        mv "$PSScriptRoot\lein.cmd.pending" "$PSScriptRoot\lein.cmd"
        rm "$PSCommandPath.pending"
        mv "$PSCommandPath" "$PSCommandPath.pending"
    }
}

function Invoke-Java
{
    Write-Verbose "CLASSPATH=$env:CLASSPATH"
    $env:JAVA_CMD = if($env:JAVA_CMD){$env:JAVA_CMD -replace '\A"|"\Z',''}else{'java'}
    $env:LEIN_JAVA_CMD = if($env:LEIN_JAVA_CMD){$env:LEIN_JAVA_CMD -replace '\A"|"\Z',''}else{$env:JAVA_CMD}
    if(!$env:JVM_OPTS){$env:JVM_OPTS = $env:JAVA_OPTS}
    $JavaArgs = @(
        '-client',$env:LEIN_JVM_OPTS,
        "`"-Dclojure.compile.path=$PWD/target/classes`"", #TODO: Add this line only when we're initializing from source
        "`"-Dleiningen.original.pwd=$env:ORIGINAL_PWD`"",
        '-cp',$env:CLASSPATH,
        'clojure.main',
        '-m','leiningen.core.main'
    )
    &$env:LEIN_JAVA_CMD @JavaArgs @Command
}

function Invoke-Leiningen
{
    Initialize-Environment
    switch($Command[0])
    {
        self-install {Install-Self}
        upgrade      {Update-Self }
        downgrade    {Update-Self }
        default
        {
            if(Test-Path "$PSCommandPath\..\src\leiningen\version.clj" -PathType Leaf) {Initialize-Source}
            else {Initialize-Binary}
            Invoke-Java
        }
    }
}

Invoke-Leiningen
