package no.nav.dokdistkanal.certificate;

import lombok.Data;
import no.nav.dokdistkanal.exceptions.technical.KeystoreProviderException;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

import static java.lang.String.format;

@Data
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
		try {
			this.keyStore = KeystoreProvider.loadKeyStoreData(properties);
		} catch (KeystoreProviderException e) {
			throw new IllegalStateException(e);
		}
		this.x509Certificate = loadX509Certificate();
		this.privateKey = loadPrivateKey();
	}

	public PrivateKey loadPrivateKey() {
		String alias = properties.alias();
		try {
			char[] password = properties.password().toCharArray();

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

	public X509Certificate loadX509Certificate() {
		String alias = properties.alias();
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
