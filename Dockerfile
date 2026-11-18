# Imagen de ejecución. El jar lo construye la integración continua: ./mvnw clean package
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S cuotafacil && adduser -S cuotafacil -G cuotafacil
COPY target/cuotafacil.jar app.jar
USER cuotafacil
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
