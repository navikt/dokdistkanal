package no.nav.dokdistkanal.consumer.dki.to;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Slf4j
public class DigitalKontaktinfoMapper {

    public DigitalKontaktinformasjonTo mapDigitalKontaktinformasjon(DkifResponseTo.DigitalKontaktinfo digitalKontaktinfo) {

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
                    .sertifikat(digitalKontaktinfo.getSikkerDigitalPostkasse() != null && isSertifikat(digitalKontaktinfo.getSikkerDigitalPostkasse()
                            .getLeverandoerSertifikat()))
                    .build();
        }
    }

    private boolean isSertifikat(String leverandoerSertifikat) {
        return leverandoerSertifikat != null && !leverandoerSertifikat.isEmpty() && isSertifikatValid(leverandoerSertifikat);
    }

    private boolean isSertifikatValid(String leverandoerSertifikat) {
        try {
            byte[] encodedCert = Base64.getDecoder().decode(leverandoerSertifikat);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(encodedCert);

            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(inputStream);
            cert.checkValidity();
            return true;
        } catch (CertificateException e) {
            log.warn("Leverandørsertifikatet har utløpt eller er ikke gyldig enda. Feilmelding: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Ukjent feil ved validering av leverandørsertifikatets gyldighet. Feilmelding: {}.", e.getMessage());
            return false;
        }
    }
}
