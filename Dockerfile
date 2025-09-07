# -------- Build Stage --------
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copy pom.xml first (so dependencies get cached)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the project (skip tests here)
RUN mvn clean install -DskipTests

# -------- Runtime Stage --------
FROM maven:3.8.5-openjdk-17
WORKDIR /app

# Copy compiled project from build stage
COPY --from=build /app /app

# Default command: run tests with optional tag
CMD ["mvn", "test", "-Dcucumber.filter.tags=${CUCUMBER_TAGS:-@smoke}"]

