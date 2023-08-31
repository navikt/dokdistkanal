# Dokdistkanal
Dokdistkanal blir benyttet av dokumentproduksjon og er ansvarlig for å bestemme distribusjonskanalen. 
Følgende distribusjonskanaler kan bli returnert:
- PRINT
- LOKAL_PRINT
- INGEN_DISTRIBUSJON
- SDP
- DITT_NAV
- TRYGDERETTEN
- DPV (kommer)

Mer informasjon om hvordan appen fungerer finner du på [Confluence-siden for dokdistkanal](https://confluence.adeo.no/display/BOA/DokdistKanal-+BestemDistribusjonskanal).

## Tjenester
Appen tilbyr en REST-tjeneste for henting av bestemt distribusjonskanal. Se [swagger-link for tjenesten](https://dokdistkanal.dev.intern.nav.no/swagger-ui/index.html) for detaljer, eller om du ønsker å prøve den ut.

### Autentisering
Kall mot denne tjenesten krever autentisering med token fra Azure AD. Enten med _client-credentials_ (maskin-til-maskin) eller _on-behalf-of_ (på vegne av bruker) flow.

### Tilgangsstyring
Har du behov for tilgang? 

- Lag en ny branch hvor du legger til et innslag for din app i `AZURE_IAC_RULES`
    - [dev](https://github.com/navikt/dokdistkanal/blob/master/nais/) (legg til i respektiv *-config.json fil)
    - [produksjon](https://github.com/navikt/dokdistkanal/blob/master/nais/p-config.json)
- Push endringene og lag en pull request.
- Pull requesten vil bli gjennomgått og merget av noen i Team Dokumentløsninger.

## Kjøre prosjektet lokalt
For å kjøre opp applikasjonen lokalt, bruk profile `nais` og systemvariabler hentet fra vault: [System variabler](https://vault.adeo.no/ui/vault/secrets/secret/show/dokument/dokdistkanal) 

### NB!
Testene MapDigitalKontaktinformasjonTest og DokDistKanalIT bruker testsertifikater utsendt fra DigiPost. 
Disse sertifikatene har utløpsdato i februar 2023. Nytt sertifikat må genereres innen den tid, hvis ikke vil testene feile. 
Sertifikatene er knyttet til testbrukerne utsendt av DigDir som kan finnes [her](https://confluence.adeo.no/display/BOA/QDIST011+-+DistribuerForsendelseTilDPI-2.+Testing).
Ved hjelp av [dkif](https://dkif-u1.dev.adeo.no/swagger-ui.html) i u1 kan sertifikatene knyttet til testbrukerne hentes ut.

## Andre spørsmål?
Spørsmål om koden eller prosjektet kan stilles på [Slack-kanalen for \#Team Dokumentløsninger](https://nav-it.slack.com/archives/C6W9E5GPJ)