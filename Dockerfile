# docker build -t zeebe-cherry-officepdf:1.0.0 .
FROM openjdk:17-alpine
EXPOSE 9081
COPY target/process-execution-automator-*-exec.jar /app.jar
COPY src/main/resources /app/scenarii
COPY src/test/resources /app/scenarii
ENTRYPOINT ["java","-jar","/app.jar"]

