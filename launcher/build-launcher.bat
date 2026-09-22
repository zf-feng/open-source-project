@echo off
rem =====================================================================
rem Rebuild the one-click launcher StardustDungeon exe.
rem Uses the .NET compiler bundled with Windows (csc.exe). No install needed.
rem Usage: double-click this file, or run it from a terminal.
rem =====================================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-launcher.ps1"
pause
