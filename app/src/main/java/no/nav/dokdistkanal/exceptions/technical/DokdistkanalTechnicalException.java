package no.nav.dokdistkanal.exceptions.technical;

public abstract class DokdistkanalTechnicalException extends RuntimeException {

	public DokdistkanalTechnicalException(String message) {
		super(message);
	}

	public DokdistkanalTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
