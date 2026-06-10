# ===========================
# Stage 1: Build (Maven)
# ===========================
FROM ghcr.io/docker-library/eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copiar o POM e baixar dependências (cache de layer)
COPY pom.xml .
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -q

# Copiar fontes e fazer build do fat JAR
COPY src ./src
RUN mvn package -DskipTests -q

# ===========================
# Stage 2: Runtime (JRE)
# ===========================
FROM ghcr.io/docker-library/eclipse-temurin:21-jre-alpine

WORKDIR /app

# Criar usuário não-root por segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiar o fat JAR do estágio de build
COPY --from=build /app/target/gestor-negocio-*.jar app.jar

# Definir dono do arquivo
RUN chown appuser:appgroup app.jar

USER appuser

# Porta padrão do Javalin
EXPOSE 7070

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:7070/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
