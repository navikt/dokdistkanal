FROM ghcr.io/navikt/baseimages/temurin:21

COPY app/target/app.jar app.jar
COPY export-vault-secrets.sh /init-scripts/30-export-vault-secrets.sh

USER root
# Brukes for å hente config fra json filer
RUN apt-get install -y --no-install-recommends jq
USER apprunner

ENV MAIN_CLASS="org.springframework.boot.loader.launch.JarLauncher"
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom \
               -XX:MaxRAMPercentage=75 \
               -Dspring.profiles.active=nais"
