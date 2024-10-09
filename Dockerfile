# docker build -t pierre-yves-monnet/processautomator:1.5.0 .
# JDK 17: openjdk:17-alpine
# JDK 21: alpine/java:21-jdk
FROM eclipse-temurin:21-jdk-alpine
EXPOSE 9081
COPY target/process-execution-automator-*-exec.jar /app.jar
COPY src/main/resources /app/scenarii
COPY src/test/resources /app/scenarii
ENTRYPOINT ["java","-jar","/app.jar"]

