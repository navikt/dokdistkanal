#!/usr/bin/env sh

if test -f /var/run/secrets/nais.io/srvdokdistkanal/username;
then
    echo "Setting SERVICEUSER_USERNAME"
    export SERVICEUSER_USERNAME=$(cat /var/run/secrets/nais.io/srvdokdistkanal/username)
fi

if test -f /var/run/secrets/nais.io/srvdokdistkanal/password;
then
    echo "Setting SERVICEUSER_PASSWORD"
    export SERVICEUSER_***passord=gammelt_passord***)
fi
