#!/usr/bin/env sh

if test -f /var/run/secrets/nais.io/srvdokdistkanal/username;
then
    echo "Setting DOKDISTKANAL_SERVICEUSER_USERNAME"
    export DOKDISTKANAL_SERVICEUSER_USERNAME=$(cat /var/run/secrets/nais.io/srvdokdistkanal/username)
fi

if test -f /var/run/secrets/nais.io/srvdokdistkanal/password;
then
    echo "Setting DOKDISTKANAL_SERVICEUSER_PASSWORD"
    export DOKDISTKANAL_SERVICEUSER_PASSWORD=$(cat /var/run/secrets/nais.io/srvdokdistkanal/password)
fi

echo "Exporting appdynamics environment variables"
if test -f /var/run/secrets/nais.io/appdynamics/appdynamics.env;
then
    export $(cat /var/run/secrets/nais.io/appdynamics/appdynamics.env)
    echo "Appdynamics environment variables exported"
else
    echo "No such file or directory found at /var/run/secrets/nais.io/appdynamics/appdynamics.env"
fi
