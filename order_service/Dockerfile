# Multi-stage build for Order Service
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Copier Maven wrapper files explicitly
COPY mvnw .
COPY mvnw.cmd .
COPY pom.xml .
# Copier explicitement les fichiers Maven Wrapper pour éviter les problèmes avec les dossiers cachés
COPY .mvn/wrapper/maven-wrapper.jar .mvn/wrapper/
COPY .mvn/wrapper/maven-wrapper.properties .mvn/wrapper/

# Rendre mvnw exécutable
RUN chmod +x ./mvnw

# Télécharger les dépendances (cette étape sera mise en cache si pom.xml n'a pas changé)
RUN ./mvnw dependency:go-offline -B

# Copier le code source
COPY src ./src

# Construire l'application (skip tests)
RUN ./mvnw clean package -DskipTests

# ==============================
# Stage production
# ==============================
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Installer curl pour le healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copier le JAR généré depuis le builder
COPY --from=builder /app/target/order-*.jar app.jar

# Créer un utilisateur non-root
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Exposer le port de l'application
EXPOSE 8080

# Healthcheck pour vérifier que l'application est prête
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Lancer l'application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
