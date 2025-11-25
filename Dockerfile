FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar arquivos do Maven
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY src ./src

# Build da aplicação
RUN ./mvnw clean package -DskipTests

# Stage final - imagem menor
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Criar usuário não-root para segurança
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar apenas o JAR compilado
COPY --from=builder /app/target/*.jar app.jar

# Expor porta
EXPOSE 8080

# Variáveis de ambiente padrão
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Executar aplicação
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
