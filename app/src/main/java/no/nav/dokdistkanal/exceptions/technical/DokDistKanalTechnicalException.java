package no.nav.dokdistkanal.exceptions.technical;

public abstract class DokDistKanalTechnicalException extends RuntimeException {

	public DokDistKanalTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
