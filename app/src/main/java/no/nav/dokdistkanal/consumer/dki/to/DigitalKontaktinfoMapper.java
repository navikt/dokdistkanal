package no.nav.dokdistkanal.consumer.dki.to;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Slf4j
public class DigitalKontaktinfoMapper {

	public static DigitalKontaktinformasjonTo mapDigitalKontaktinformasjon(DkifResponseTo.DigitalKontaktinfo digitalKontaktinfo) {

		if (digitalKontaktinfo == null) {
			return null;
		} else {
			return DigitalKontaktinformasjonTo.builder()
					.brukerAdresse(digitalKontaktinfo.getSikkerDigitalPostkasse() != null ? digitalKontaktinfo.getSikkerDigitalPostkasse()
							.getAdresse() : null)
					.epostadresse(digitalKontaktinfo.isKanVarsles() ? digitalKontaktinfo.getEpostadresse() : null)
					.leverandoerAdresse(digitalKontaktinfo.getSikkerDigitalPostkasse() != null ? digitalKontaktinfo.getSikkerDigitalPostkasse()
							.getLeverandoerAdresse() : null)
					.mobiltelefonnummer(digitalKontaktinfo.isKanVarsles() ? digitalKontaktinfo.getMobiltelefonnummer() : null)
					.reservasjon(digitalKontaktinfo.isReservert())
					.gyldigSertifikat(digitalKontaktinfo.getSikkerDigitalPostkasse() != null && isSertifikatPresentAndValid(digitalKontaktinfo.getSikkerDigitalPostkasse()
							.getLeverandoerSertifikat()))
					.build();
		}
	}

	private static boolean isSertifikatPresentAndValid(String leverandoerSertifikat) {
		return leverandoerSertifikat != null && !leverandoerSertifikat.isEmpty() && isSertifikatValid(leverandoerSertifikat);
	}

	private static boolean isSertifikatValid(String leverandoerSertifikat) {
		try {
			byte[] encodedCert = Base64.getDecoder().decode(leverandoerSertifikat);
			ByteArrayInputStream inputStream = new ByteArrayInputStream(encodedCert);

			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			X509Certificate cert = (X509Certificate) certFactory.generateCertificate(inputStream);
			cert.checkValidity();
			return true;
		} catch (CertificateExpiredException e) {
			log.warn("Leverandørsertifikatet har utløpt. Feilmelding: {}", e.getMessage(), e);
			return false;
		} catch (CertificateNotYetValidException e) {
			log.warn("Leverandørsertifikatet er ikke gyldig enda. Feilmelding: {}", e.getMessage(), e);
			return false;
		} catch (Exception e) {
			log.warn("Ukjent feil ved validering av leverandørsertifikatets gyldighet. Feilmelding: {}.", e.getMessage(), e);
			return false;
		}
	}
}
