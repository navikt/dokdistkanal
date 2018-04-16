FROM navikt/java:8

COPY target/app.jar /app/app.jar

ENV NAIS_SECRETS="/var/run/secrets/naisd.io/"

ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom \
               -Dspring.profiles.active=nais"