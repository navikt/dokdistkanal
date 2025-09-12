package no.nav.dokdistkanal.consumer.altinn.maskinporten;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.certificate.AppCertificate;
import no.nav.dokdistkanal.exceptions.technical.JwtSerializeException;

import java.security.cert.CertificateEncodingException;
import java.text.ParseException;
import java.util.List;

@Slf4j
public class MaskinportenUtils {

	private static final String SERTIFIKAT_ENCODING_FEIL = "Kunne ikke enkode sertifikat";
	private static final String SIGNERING_FEIL = "Feil ved signering av JWT";

	public static String createSignedJWTFromJwk(String rsaJwk, JWTClaimsSet claimsSet) {
		try {
			var rsaKey = RSAKey.parse(rsaJwk);
			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
					.keyID(rsaKey.getKeyID())
					.type(JOSEObjectType.JWT)
					.build();
			SignedJWT signedJWT = new SignedJWT(header, claimsSet);
			JWSSigner signer = new RSASSASigner(rsaKey);
			signedJWT.sign(signer);
			return signedJWT.serialize();
		} catch (ParseException | JOSEException e) {
			throw new JwtSerializeException("feilet å parse JWT", e);
		}
	}

	public static String generateSignedJWTFromCertificate(AppCertificate appCertificate, JWTClaimsSet claims) {
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
				.x509CertChain(List.of(encodeCertificate(appCertificate)))
				.build();

		RSASSASigner signer = new RSASSASigner(appCertificate.getPrivateKey());

		SignedJWT signedJWT = new SignedJWT(header, claims);
		try {
			signedJWT.sign(signer);
			return signedJWT.serialize();
		} catch (JOSEException e) {
			log.error(SIGNERING_FEIL, e);
			throw new JwtSerializeException(SIGNERING_FEIL, e);
		}
	}

	private static Base64 encodeCertificate(AppCertificate appCertificate) {
		try {
			return Base64.encode(appCertificate.getX509Certificate().getEncoded());
		} catch (CertificateEncodingException e) {
			log.error(SERTIFIKAT_ENCODING_FEIL, e);
			throw new JwtSerializeException(SERTIFIKAT_ENCODING_FEIL, e);
		}
	}

	private MaskinportenUtils() {
	}
}
