#!/bin/sh
# Publica la base de datos de esta carpeta para que otras PCs de la red se conecten. Puerto por defecto: 9092.
cd "$(dirname "$0")"
[ -f GameShelf.jar ] || ./construir.sh || exit 1
java -jar GameShelf.jar --servidor "$@"
