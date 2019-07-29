package no.nav.dokdistkanal.exceptions;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public abstract class DokDistKanalFunctionalException extends RuntimeException {
	public DokDistKanalFunctionalException(String message) {
		super(message);
	}

	public DokDistKanalFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}

	public DokDistKanalFunctionalException(Throwable cause) {
		super(cause);
	}
}
