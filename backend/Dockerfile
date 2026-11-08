FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 10001 espirix
COPY --from=build /build/target/*.jar /app/app.jar
USER espirix
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
