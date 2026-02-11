# Build stage
FROM maven:3.9.12-eclipse-temurin-21 AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
COPY --from=build /home/app/target/stealth-0.0.1-SNAPSHOT.jar /usr/local/lib/stealth.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/local/lib/stealth.jar"]