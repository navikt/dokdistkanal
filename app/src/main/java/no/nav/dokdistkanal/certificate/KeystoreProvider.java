package no.nav.dokdistkanal.certificate;

import no.nav.dokdistkanal.exceptions.technical.KeystoreProviderException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;

import static java.util.Objects.requireNonNull;

public class KeystoreProvider {

	public static KeyStore loadKeyStoreData(KeyStoreProperties properties) {
		try {
			String type = properties.type();
			char[] password = properties.password().toCharArray();
			Resource path = new FileSystemResource(properties.key());

			KeyStore keyStore = KeyStore.getInstance(type);

			try (var inputStream = path.getInputStream()) {
				keyStore.load(isBase64Empty(path) ? Base64.getDecoder().wrap(inputStream) : inputStream, password);
			}

			return keyStore;
		} catch (IOException e) {
			throw new KeystoreProviderException("Could not open keystore file", e);
		} catch (KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
			throw new KeystoreProviderException("Failed to load keystore", e);
		}
	}

	private static boolean isBase64Empty(Resource path) {
		return requireNonNull(path.getFilename()).endsWith(".b64");
	}

	private KeystoreProvider() {
		//noop
	}
}
