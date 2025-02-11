FROM openjdk:17-jdk
COPY ./build/libs/course-service.jar course-service.jar
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-jar", "course-service.jar"]