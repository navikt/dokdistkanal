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
import lombok.SneakyThrows;

public class MaskinportenUtils {

	@SneakyThrows
	public static SignedJWT createSignedJWT(String rsaJwk, JWTClaimsSet claimsSet) {
		try {
			var rsaKey = RSAKey.parse(rsaJwk);
			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
					.keyID(rsaKey.getKeyID())
					.type(JOSEObjectType.JWT)
					.build();
			SignedJWT signedJWT = new SignedJWT(header, claimsSet);
			JWSSigner signer = new RSASSASigner(rsaKey);
			signedJWT.sign(signer);
			return signedJWT;
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}
	}
}
