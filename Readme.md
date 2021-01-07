# dokdistkanal

* [Funksjonelle Krav](#funksjonelle-krav)
* [Distribusjon av tjenesten (deployment)](#distribusjon-av-tjenesten-deployment)
* [Utviklingsmiljø](#utviklingsmilj)
* [Drift og støtte](#drift-og-sttte)


## Funksjonelle krav
Dokdistkanal har ansvar for å bestemme distribusjonskanali DokSys.
                              
For mer informasjon: [confluence](https://confluence.adeo.no/display/BOA/DokdistKanal-+BestemDistribusjonskanaln)

## Distribusjon av tjenesten (deployment)
Distribusjon av tjenesten er gjort av Jenkins:
[dokdistkanal CI / CD](https://dok-jenkins.adeo.no/job/dokdistkanal/job/master/)
Push/merge til master branch vil teste, bygge og deploye til produksjonsmiljø og testmiljø.

## Utviklingsmiljø
### Forutsetninger
* Java 11
* Kubectl
* Maven

### Kjøre prosjektet lokalt
For å kjøre opp applikasjonen lokal, bruk profile `nais` og systemvariabler hentet fra vault: [System variabler](https://vault.adeo.no/ui/vault/secrets/secret/show/dokument/dokdistkanal) 

### Bygge app.jar og kjøre tester

`mvn clean package`/`mvn clean install`

####NB!
Testene MapDigitalKontaktinformasjonTest og DokDistKanalIT bruker testsertifikater utsendt fra DigiPost. 
Disse sertifikatene har utløpsdato i februar 2023. Nytt sertifikat må generes innen den tid, hvis ikke vil testene feile. 
Sertifikatene er knyttet til testbrukerne utsendt av DigDir som kan finnes [her](https://confluence.adeo.no/display/BOA/QDIST011+-+DistribuerForsendelseTilDPI-2.+Testing).
Ved hjelp av [dkif](https://dkif-u1.dev.adeo.no/swagger-ui.html) i u1 kan sertifikatene knyttet til testbrukerne hentes ut.

## Drift og støtte
### Logging
Loggene til tjenesten kan leses på to måter:

### Kibana
For [dev-fss](https://logs.adeo.no/goto/9e5bbcaa4a99b73f76baa6a7db15cbde)

For [prod-fss](https://logs.adeo.no/goto/9289856c2e60456049eee47cd86efc97)

### Kubectl
For dev-fss:
```shell script
kubectl config use-context dev-fss
kubectl get pods -n q1 -l app=dokdistkanal
kubectl logs -f dokdistkanal-<POD-ID> -n teamdokumenthandtering -c dokdistkanal
```

For prod-fss:
```shell script
kubectl config use-context prod-fss
kubectl get pods -l app=dokdistkanal
kubectl logs -f dokdistkanal-<POD-ID> -n teamdokumenthandtering -c dokdistkanal
```

### Henvendelser
Spørsmål til koden eller prosjektet kan rettes til Team Dokumentløsninger på:
* [\#Team Dokumentløsninger](https://nav-it.slack.com/client/T5LNAMWNA/C6W9E5GPJ)
