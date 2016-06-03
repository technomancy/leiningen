@echo off
setlocal
set ps1=%~dpn0.ps1
shift
powershell -File "%ps1%" %*