FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/connect4-1.0-SNAPSHOT.jar connect4.jar

EXPOSE 4444

ENTRYPOINT ["java", "-jar", "connect4.jar"]

# Run help command by default
CMD ["--help"]
