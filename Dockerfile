FROM eclipse-temurin:21-jre as builder
WORKDIR build
COPY app/target/app.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM ghcr.io/navikt/baseimages/temurin:21-appdynamics
WORKDIR app
COPY --from=builder build/dependencies/ ./
COPY --from=builder build/snapshot-dependencies/ ./
COPY --from=builder build/spring-boot-loader/ ./
COPY --from=builder build/application/ ./
COPY export-vault-secrets.sh /init-scripts/10-export-vault-secrets.sh
COPY run-java.sh /
USER root
RUN chmod +x /run-java.sh
USER apprunner

ENV APPD_ENABLED=true
ENV MAIN_CLASS="org.springframework.boot.loader.launch.JarLauncher"
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=nais"
