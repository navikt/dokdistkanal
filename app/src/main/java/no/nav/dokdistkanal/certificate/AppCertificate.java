package no.nav.dokdistkanal.certificate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import no.nav.dokdistkanal.exceptions.technical.KeystoreProviderException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

import static java.lang.String.format;

@Getter
public class AppCertificate {

	private static final String ERR_MISSING_PRIVATE_KEY_OR_PASS = "Feil ved tilgang til PrivateKey med alias \"%s\": tilgang nektet eller feil passord";
	private static final String ERR_MISSING_PRIVATE_KEY = "Ingen PrivateKey med alias \"%s\" ble funnet i KeyStore";
	private static final String ERR_MISSING_CERTIFICATE = "Ingen AppCertificate med alias \"%s\" ble funnet i KeyStore";
	private static final String ERR_GENERAL = "Uventet feil oppstod ved operasjon på KeyStore.";

	private final KeyStoreProperties properties;
	private final KeyStore keyStore;
	private final X509Certificate x509Certificate;
	private final PrivateKey privateKey;

	public AppCertificate(KeyStoreProperties properties) {
		this.properties = properties;
		KeyStoreCredentials keyStoreCredentials = loadKeyStoreCredentialsJson(properties.credentials());
		try {
			this.keyStore = KeystoreProvider.loadKeyStoreData(properties, keyStoreCredentials);
		} catch (KeystoreProviderException e) {
			throw new IllegalStateException(e);
		}
		this.x509Certificate = loadX509Certificate(keyStoreCredentials);
		this.privateKey = loadPrivateKey(keyStoreCredentials);
	}

	private static KeyStoreCredentials loadKeyStoreCredentialsJson(String credentials) {
		Path credentialsJsonPath = Paths.get(credentials);
		if (!Files.exists(credentialsJsonPath)) {
			throw new IllegalArgumentException("credentials med path=" + credentials + " finnes ikke");
		}
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.readValue(credentialsJsonPath.toFile(), KeyStoreCredentials.class);
		} catch (IOException e) {
			// Rethrower ikke exception for å ikke risikere at innhold dumpes til loggen
			throw new IllegalArgumentException("Klarte ikke lese credentials json");
		}
	}

	private PrivateKey loadPrivateKey(KeyStoreCredentials keyStoreCredentials) {
		String alias = keyStoreCredentials.alias();
		try {
			char[] password = keyStoreCredentials.password().toCharArray();

			PrivateKey key = (PrivateKey) keyStore.getKey(alias, password);
			if (key == null) {
				throw new IllegalStateException(format(ERR_MISSING_PRIVATE_KEY, alias));
			}
			return key;
		} catch (KeyStoreException | NoSuchAlgorithmException e) {
			throw new IllegalStateException(ERR_GENERAL, e);
		} catch (UnrecoverableKeyException e) {
			throw new IllegalStateException(format(ERR_MISSING_PRIVATE_KEY_OR_PASS, alias), e);
		}
	}

	private X509Certificate loadX509Certificate(KeyStoreCredentials keyStoreCredentials) {
		String alias = keyStoreCredentials.alias();
		try {
			X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
			if (certificate == null) {
				throw new IllegalStateException(format(ERR_MISSING_CERTIFICATE, alias));
			}
			return certificate;
		} catch (KeyStoreException e) {
			throw new IllegalStateException(ERR_GENERAL, e);
		}
	}
}
