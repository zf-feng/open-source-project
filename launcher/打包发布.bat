@echo off
rem Create a clean source-release zip that includes the one-click launcher.
rem The zip is written to the project root folder.
rem Usage: double-click this file (it lives in launcher\, next to package-release.ps1).
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0package-release.ps1"
pause
