package no.nav.dokdistkanal.exceptions.functional;

import java.time.LocalDate;

public class EnhetSlettetException extends DokdistkanalFunctionalException {
	public EnhetSlettetException(String organisasjonsnummer, LocalDate slettedato) {
		super("Virksomheten med organisasjonsnummer=%s er slettet per %s. Forsendelsen kan ikke distribueres."
				.formatted(organisasjonsnummer, slettedato));
	}
}
