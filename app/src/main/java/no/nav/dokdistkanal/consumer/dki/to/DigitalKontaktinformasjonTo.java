package no.nav.dokdistkanal.consumer.dki.to;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
public class DigitalKontaktinformasjonTo {
	private String epostadresse;
	private String mobiltelefonnummer;
	private boolean reservasjon;
	private String leverandoerAdresse;
	private String brukerAdresse;
	private String sertifikat;

	public boolean verifyAddress() {
		boolean hasSertifikat = StringUtils.isNotBlank(getSertifikat());
		boolean hasLeverandorAdresse = StringUtils.isNotBlank(getLeverandoerAdresse());
		boolean hasBrukerAdresse = StringUtils.isNotBlank(getBrukerAdresse());

		return (hasSertifikat && hasLeverandorAdresse && hasBrukerAdresse);
	}

}
