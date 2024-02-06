package no.nav.dokdistkanal.exceptions.functional;

public abstract class DokDistKanalFunctionalException extends RuntimeException {

	public DokDistKanalFunctionalException(String message) {
		super(message);
	}

	public DokDistKanalFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
