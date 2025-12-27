 # Build stage
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# List the built jar to verify
RUN ls -la /build/target/

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from build stage
COPY keystore.p12 /app/keystore.p12
COPY --from=build /build/target/*.jar app.jar

# Expose port
EXPOSE ${BACKEND_PORT}

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

