FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY GameShelf.jar /app/GameShelf.jar
COPY gameshelf-db.mv.db /seed/gameshelf-db.mv.db
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

RUN chmod +x /app/docker-entrypoint.sh && mkdir -p /data

VOLUME ["/data"]

EXPOSE 9092

ENTRYPOINT ["/app/docker-entrypoint.sh"]
