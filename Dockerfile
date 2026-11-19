# Stage 1: Build Application with Maven Wrapper
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Cache dependency layer
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code and package executable jar
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Production Distroless/Minimal Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-privileged system user for container security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built artifact from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
