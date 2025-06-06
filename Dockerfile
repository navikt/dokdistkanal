FROM ghcr.io/navikt/baseimages/temurin:21

COPY app/target/app.jar app.jar

ENV MAIN_CLASS="org.springframework.boot.loader.launch.JarLauncher"
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom \
               -XX:MaxRAMPercentage=75 \
               -Dspring.profiles.active=nais"
