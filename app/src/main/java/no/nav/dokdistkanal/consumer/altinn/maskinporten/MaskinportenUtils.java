package no.nav.dokdistkanal.consumer.altinn.maskinporten;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class MaskinportenUtils {

	static final Pattern ISO6523_PATTERN = Pattern.compile("^([0-9]{4}:)([0-9]{9})$");
	public static final String ISO6523_PREFIX = "0192:";

	public static SignedJWT createSignedJWT(RSAKey rsaJwk, JWTClaimsSet claimsSet) {
		try {
			JWSHeader.Builder header = new JWSHeader.Builder(JWSAlgorithm.RS256)
					.keyID(rsaJwk.getKeyID())
					.type(JOSEObjectType.JWT);
			SignedJWT signedJWT = new SignedJWT(header.build(), claimsSet);
			JWSSigner signer = new RSASSASigner(rsaJwk.toPrivateKey());
			signedJWT.sign(signer);
			return signedJWT;
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}
	}

	public static String asIso6523(final String orgNummer) {
		if(isIso6523(orgNummer)) {
			return orgNummer;
		}
		return ISO6523_PREFIX + orgNummer;
	}

	public static boolean isIso6523(final String iso6523Orgnr) {
		return ISO6523_PATTERN.matcher(iso6523Orgnr).matches();
	}
}
