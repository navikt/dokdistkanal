package no.nav.dokdistkanal.exceptions.functional;

public class PdlFunctionalException extends DokDistKanalFunctionalException {

    public PdlFunctionalException(String message) {
        super(message);
    }

    public PdlFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
