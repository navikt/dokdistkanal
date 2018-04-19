package no.nav.dokdistkanal.consumer.sikkerhetsnivaa;

import no.nav.dokdistkanal.exceptions.DokKanalvalgTechnicalException;

public class SikkerhetsnivaaTechnicalException extends DokKanalvalgTechnicalException{
	public SikkerhetsnivaaTechnicalException(String message) {
		super(message);
	}

	public SikkerhetsnivaaTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
