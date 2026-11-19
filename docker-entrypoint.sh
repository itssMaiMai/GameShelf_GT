#!/bin/sh
set -e

DATA_DIR="/data"
DB_FILE="$DATA_DIR/gameshelf-db.mv.db"
SEED_FILE="/seed/gameshelf-db.mv.db"
PORT="${H2_PORT:-9092}"

mkdir -p "$DATA_DIR"

# Si el volumen persistente está vacío en el primer arranque, copiamos la base inicial con los datos cargados.
if [ ! -f "$DB_FILE" ]; then
    echo "Inicializando base de datos en $DB_FILE desde la copia semilla..."
    cp "$SEED_FILE" "$DB_FILE"
else
    echo "Usando base de datos existente en $DB_FILE."
fi

exec java -Djava.awt.headless=true -jar /app/GameShelf.jar --servidor --puerto "$PORT" --carpeta "$DATA_DIR"
