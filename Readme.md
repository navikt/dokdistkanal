Dokdistkanal
================

## Bygge app.jar og kjøre tester

`mvn clean package`

## Kjøre systemtester

Denne applikasjonen har ingen automatiske systemtester

## Hvordan kjøre lokalt med mvn spring-boot plugin

Det ligger profil for t8 i `dokdistkanal/src/main/resources`:

* application-t8.properties

Noen secrets må settes - se https://fasit.adeo.no/instances/4712125

```

# Systembruker
export SRVDOKDISTKANAL_USERNAME=srvdokdistkanal
export SRVDOKDISTKANAL_***passord=gammelt_passord***>

# System cert
export SRVDOKDISTKANAL_CERT_KEYSTORE=<keystorepath>
export SRVDOKDISTKANAL_CERT_KEYSTOREALIAS=<keystorealias>   # Default app-key
export SRVDOKDISTKANAL_CERT_***passord=gammelt_passord***>
```

Kjøre appen med mvn spring boot plugin. Truststore finnes på Fasit som `nav_truststore` alias. 

```
mvn spring-boot:run -Drun.profiles=t8 -Drun.jvmArguments="-DSRVDOKDISTKANAL_CERT_KEYSTORE=<path>/srvdokdistkanal_t.jks -DSRVDOKDISTKANAL_CERT_***passord=gammelt_passord***>"
```
## Cache ved lokal kjøring

Denne applikasjonen bruker Redis cache som er avhengig av en ekstern cache server som den kan koble seg til. 
Når applikasjonen kjøres lokalt vil det istedenfor settes opp cache som kjører lokalt på applikasjonen. Konfigurasjon av denne cachen ligger i `LokalCacheConfig` klassen og vil bare kjøres når Activeprofiles settes `local`.

## Hvordan kjøre lokalt med IntelliJ

Start `Application.java` som en Spring Boot/Java Application. På denne måten kan man kjøre lokalt og få full debug-støtte. 

Skriv inn passordet for `srvrdokdistkanal` servicebrukene i `serviceuser.password` i application-t8.properties filen. 

VM Options: `-Dsrvdokdistkanal_cert_keystore=/path/til/cert.jks -Dsrvdokdistkanal_cert_***passord=gammelt_passord***>`

Active profiles: `t8`.

---

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan rettes mot:

* Applikasjonsansvarlig Paul Magne Lunde <Paul.Magne.Lunde@nav.no> 

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #dokumenthåndtering.
