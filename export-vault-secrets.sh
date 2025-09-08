#!/usr/bin/env sh

if test -f "$NAV_VIRKSOMHETSSERTIFIKAT_CREDENTIALS"
then
    echo "Setting nav_virksomhetssertifikat_alias"
    export nav_virksomhetssertifikat_alias="$(cat $NAV_VIRKSOMHETSSERTIFIKAT_CREDENTIALS | jq -r '.alias')"
    echo "Setting nav_virksomhetssertifikat_password"
    export nav_virksomhetssertifikat_password="$(cat $NAV_VIRKSOMHETSSERTIFIKAT_CREDENTIALS | jq -r '.password')"
    echo "Setting nav_virksomhetssertifikat_type"
    export nav_virksomhetssertifikat_type="$(cat $NAV_VIRKSOMHETSSERTIFIKAT_CREDENTIALS | jq -r '.type')"
fi
