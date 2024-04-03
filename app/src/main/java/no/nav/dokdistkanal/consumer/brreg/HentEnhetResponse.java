package no.nav.dokdistkanal.consumer.brreg;

public record HentEnhetResponse(
		String organisasjonsnummer,
		boolean konkurs
) {
}
