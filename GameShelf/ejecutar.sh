#!/bin/sh
cd "$(dirname "$0")"
[ -f GameShelf.jar ] || ./construir.sh || exit 1
java -jar GameShelf.jar
