@echo off
setlocal
set ps1=%~dpn0.ps1
shift
powershell -NoProfile -ExecutionPolicy Bypass -File "%ps1%" %*
