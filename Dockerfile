# ─── Build Stage ─────────────────────────────────────────────
FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /app

# Copier les fichiers Maven
COPY pom.xml .
COPY src ./src

# Compiler et packager l'application
RUN mvn clean package -DskipTests

# ─── Runtime Stage ───────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Argument pour le port (par défaut 8081)
ARG PORT=8081
ENV PORT=$PORT

# Copier le JAR depuis le stage builder
COPY --from=builder /app/target/iam-service-*.jar iam-service.jar

# Exposer le port
EXPOSE $PORT

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:${PORT}/actuator/health || exit 1

# Point d'entrée
ENTRYPOINT ["java", \
  "-Dspring.profiles.active=docker", \
  "-jar", \
  "iam-service.jar"]
