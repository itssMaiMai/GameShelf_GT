@echo off
rem Publica la base de datos de esta carpeta para que otras PCs de la red se conecten. Puerto por defecto: 9092.
cd /d "%~dp0"
if not exist GameShelf.jar call construir.bat
java -jar GameShelf.jar --servidor %*
pause
