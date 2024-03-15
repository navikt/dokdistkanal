package no.nav.dokdistkanal.exceptions.functional;

public abstract class DokdistkanalFunctionalException extends RuntimeException {

	public DokdistkanalFunctionalException(String message) {
		super(message);
	}

	public DokdistkanalFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
