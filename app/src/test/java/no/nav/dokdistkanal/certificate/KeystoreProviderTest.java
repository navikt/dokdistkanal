package no.nav.dokdistkanal.certificate;

import org.junit.jupiter.api.Test;

import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class KeystoreProviderTest {

	public static final String SELF_SIGNED_PKCS12 = "src/test/resources/secrets/cert.p12";
	public static final String SELF_SIGNED_PKCS12_BASE64 = "src/test/resources/secrets/cert.p12.b64";
	public static final String PKCS_12 = "PKCS12";
	public static final String SELF_SIGNED_PKCS12_ALIAS = "1";
	public static final String SELF_SIGNED_PKCS12_PASSWORD = "navdpo";

	@Test
	void shouldLoadPKCS12KeyStore() {
		KeyStoreProperties properties = testVirksomhetssertifikatProperties();
		KeyStore keyStore = KeystoreProvider.loadKeyStoreData(properties);
		assertNotNull(keyStore);
	}

	@Test
	void shouldLoadBase64KeyStore() {
		KeyStoreProperties properties = testVirksomhetssertifikatBase64Properties();
		KeyStore keyStore = KeystoreProvider.loadKeyStoreData(properties);
		assertNotNull(keyStore);
	}

	public static KeyStoreProperties testVirksomhetssertifikatProperties() {
		return new KeyStoreProperties(PKCS_12, SELF_SIGNED_PKCS12_ALIAS, SELF_SIGNED_PKCS12_PASSWORD, SELF_SIGNED_PKCS12);
	}

	public static KeyStoreProperties testVirksomhetssertifikatBase64Properties() {
		return new KeyStoreProperties(PKCS_12, SELF_SIGNED_PKCS12_ALIAS, SELF_SIGNED_PKCS12_PASSWORD, SELF_SIGNED_PKCS12_BASE64);
	}
}