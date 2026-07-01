package no.nav.dokdistkanal.consumer.brreg;

import java.time.LocalDate;

public record HentUnderenhetResponse (String organisasjonsnummer, String overordnetEnhet, LocalDate slettedato){
}
