package no.nav.dokdistkanal.consumer.brreg;

import java.time.LocalDate;

public record HovedenhetResponse(
		String organisasjonsnummer,
		boolean konkurs,
		LocalDate slettedato
) {
}
