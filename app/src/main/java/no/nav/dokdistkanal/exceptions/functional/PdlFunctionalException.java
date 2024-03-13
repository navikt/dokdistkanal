package no.nav.dokdistkanal.exceptions.functional;

public class PdlFunctionalException extends DokdistkanalFunctionalException {

    public PdlFunctionalException(String message) {
        super(message);
    }

    public PdlFunctionalException(String message, Throwable cause) {
        super(message, cause);
    }
}
