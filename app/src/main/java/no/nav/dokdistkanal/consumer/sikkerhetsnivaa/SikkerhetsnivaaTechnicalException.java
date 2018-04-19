package no.nav.dokdistkanal.consumer.sikkerhetsnivaa;

import no.nav.dokdistkanal.exceptions.DokDistKanalTechnicalException;

public class SikkerhetsnivaaTechnicalException extends DokDistKanalTechnicalException {
	public SikkerhetsnivaaTechnicalException(String message) {
		super(message);
	}

	public SikkerhetsnivaaTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
