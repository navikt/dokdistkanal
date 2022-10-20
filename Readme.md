# dokdistkanal

* [Funksjonelle krav](#funksjonelle-krav)
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
### Kjøre prosjektet lokalt
For å kjøre opp applikasjonen lokalt, bruk profile `nais` og systemvariabler hentet fra vault: [System variabler](https://vault.adeo.no/ui/vault/secrets/secret/show/dokument/dokdistkanal) 

### Kjøre tester og bygge app.jar

`mvn clean verify`/`mvn clean package`

####NB!
Testene MapDigitalKontaktinformasjonTest og DokDistKanalIT bruker testsertifikater utsendt fra DigiPost. 
Disse sertifikatene har utløpsdato i februar 2023. Nytt sertifikat må generes innen den tid, hvis ikke vil testene feile. 
Sertifikatene er knyttet til testbrukerne utsendt av DigDir som kan finnes [her](https://confluence.adeo.no/display/BOA/QDIST011+-+DistribuerForsendelseTilDPI-2.+Testing).
Ved hjelp av [dkif](https://dkif-u1.dev.adeo.no/swagger-ui.html) i u1 kan sertifikatene knyttet til testbrukerne hentes ut.

### Henvendelser
Spørsmål til koden eller prosjektet kan rettes til Team Dokumentløsninger på:
* [\#Team Dokumentløsninger](https://nav-it.slack.com/client/T5LNAMWNA/C6W9E5GPJ)
