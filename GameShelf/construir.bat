@echo off
rem Compila GameShelf y arma GameShelf.jar (incluye el controlador de la base de datos H2). Requiere un JDK 17 o superior.
cd /d "%~dp0"
if exist build rmdir /s /q build
mkdir build
pushd build
jar xf ..\lib\h2.jar
jar xf ..\lib\mariadb-java-client.jar
del /q META-INF\MANIFEST.MF META-INF\*.SF META-INF\*.RSA META-INF\*.DSA 2>nul
popd
dir /s /b src\*.java > fuentes.txt
javac --release 17 -cp lib\h2.jar;lib\mariadb-java-client.jar -encoding UTF-8 -d build @fuentes.txt || (del fuentes.txt & pause & exit /b 1)
del fuentes.txt
jar cfe GameShelf.jar gameshelf.Main -C build .
rmdir /s /q build
echo Listo: GameShelf.jar
