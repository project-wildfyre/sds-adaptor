FROM openjdk:11-slim
VOLUME /tmp

COPY target/sds-adaptor.jar sds-adaptor.jar

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/sds-adaptor.jar"]

