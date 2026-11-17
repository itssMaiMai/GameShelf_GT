@echo off
cd /d "%~dp0"
if not exist GameShelf.jar call construir.bat
java -jar GameShelf.jar
pause
