Filene er selvsignerte og generert opp for bruk i enhetstest og integrasjonstest.

Kommando:
openssl req -x509 -newkey rsa:2048 -nodes -keyout key.pem -out cert.pem -days 3650

Genererte:
prkey.key - RSA private key
cert.pem - Selvsignert PEM sertifikat

-------

Kommando:
$ openssl pkcs12 -export -out cert.p12 -inkey key.pem -in cert.pem

Genererte:
cert.p12 - Selvsignert PKCS12 sertifikat

Passord finnes man i CertTestUtils.java

----

Kommando:
$ base64 -i cert.p12 -o cert.p12.b64

Genererte:
cert.p12.b64 - Selvsignert PKCS12 sertifikat som er base64 enkodet
