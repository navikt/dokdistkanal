package no.nav.dokdistkanal.service;

import java.time.LocalDate;

public record InternEnhet (String organisasjonsnummer,
						   boolean konkurs,
						   LocalDate slettedato) {
}
