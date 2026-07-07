package no.nav.dokdistkanal.consumer.brreg;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record HovedenhetResponse(
		String organisasjonsnummer,
		boolean konkurs,
		LocalDate slettedato
) {
}
