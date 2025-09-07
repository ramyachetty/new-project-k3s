FROM maven:3.8.5-openjdk-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean install -DskipTests

CMD mvn exec:java -Dexec.args="${CLI_ARGS_STRING}"
