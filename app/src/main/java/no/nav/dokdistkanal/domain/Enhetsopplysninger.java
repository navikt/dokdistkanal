package no.nav.dokdistkanal.domain;

import java.time.LocalDate;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public record Enhetsopplysninger(
		Underenhet underenhet,
		Hovedenhet hovedenhet
) {
	public static Enhetsopplysninger fraUnderenhet(Underenhet underenhet) {
		return new Enhetsopplysninger(underenhet, null);
	}

	public static Enhetsopplysninger fraHovedenhet(Hovedenhet hovedenhet) {
		return new Enhetsopplysninger(null, hovedenhet);
	}

	public boolean harHovedenhet() {
		return hovedenhet != null;
	}

	public boolean erHovedenhetKonkurs() {
		return hovedenhet != null && hovedenhet.konkurs();
	}

	public boolean erHovedenhetSlettet() {
		return hovedenhet != null && hovedenhet.erSlettet();
	}

	public boolean erUnderenhetSlettet() {
		return underenhet != null && underenhet.erSlettet();
	}

	public record Hovedenhet(String organisasjonsnummer, boolean konkurs, LocalDate slettedato) {

		public boolean erSlettet() {
			return slettedato != null;
		}
	}

	public record Underenhet(String organisasjonsnummer, String overordnetEnhet, LocalDate slettedato) {

		public boolean erSlettet() {
			return slettedato != null;
		}

		public String hovedenhetOrganisasjonsnummer() {
			if (isNotBlank(overordnetEnhet)) {
				return overordnetEnhet;
			}
			return organisasjonsnummer;
		}
	}
}
