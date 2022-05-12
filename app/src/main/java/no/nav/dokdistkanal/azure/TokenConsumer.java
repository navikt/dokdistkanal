package no.nav.dokdistkanal.azure;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
public interface TokenConsumer {
	TokenResponse getClientCredentialToken();
}
