package no.nav.dokdistkanal.azure;

import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;

public class AzureTokenException extends DokDistKanalTechnicalException {
	public AzureTokenException(String message, Throwable cause) {
		super(message, cause);
	}
}
