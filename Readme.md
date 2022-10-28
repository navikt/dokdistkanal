# Dokdistkanal
Dokdistkanal blir benyttet av dokumentproduksjon og er ansvarlig for å bestemme distribusjonskanalen.
Følgende distribusjonskanaler kan bli returnert:
- PRINT
- LOKAL_PRINT
- INGEN_DISTRIBUSJON
- SDP
- DITT_NAV
- TRYGDERETTEN
- DPV (kommer senere)

Mer informasjon om hvordan appen fungerer finner du på [Confluence-siden for dokdistkanal](https://confluence.adeo.no/display/BOA/DokdistKanal-+BestemDistribusjonskanal).

## Kjøre prosjektet lokalt
For å kjøre opp applikasjonen lokalt, bruk profile `nais` og systemvariabler hentet fra vault: [System variabler](https://vault.adeo.no/ui/vault/secrets/secret/show/dokument/dokdistkanal) 

### NB!
Testene MapDigitalKontaktinformasjonTest og DokDistKanalIT bruker testsertifikater utsendt fra DigiPost. 
Disse sertifikatene har utløpsdato i februar 2023. Nytt sertifikat må genereres innen den tid, hvis ikke vil testene feile. 
Sertifikatene er knyttet til testbrukerne utsendt av DigDir som kan finnes [her](https://confluence.adeo.no/display/BOA/QDIST011+-+DistribuerForsendelseTilDPI-2.+Testing).
Ved hjelp av [dkif](https://dkif-u1.dev.adeo.no/swagger-ui.html) i u1 kan sertifikatene knyttet til testbrukerne hentes ut.

## Andre spørsmål?
Spørsmål om koden eller prosjektet kan stilles på [Slack-kanalen for \#Team Dokumentløsninger](https://nav-it.slack.com/archives/C6W9E5GPJ)