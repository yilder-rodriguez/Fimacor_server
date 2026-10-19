FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /opt/fimacor
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre

ENV TZ=America/Bogota

WORKDIR /opt/fimacor
COPY --from=build /opt/fimacor/target/fimacor-spring.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
