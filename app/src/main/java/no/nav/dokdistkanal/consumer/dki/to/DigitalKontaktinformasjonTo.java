package no.nav.dokdistkanal.consumer.dki.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DigitalKontaktinformasjonTo {

	private String epostadresse;
	private String mobiltelefonnummer;
	private boolean reservasjon;
	private String leverandoerAdresse;
	private String brukerAdresse;
	private boolean gyldigSertifikat;

	public boolean verifyAddressAndSertificate() {
		boolean hasLeverandorAdresse = isNotEmpty(getLeverandoerAdresse());
		boolean hasBrukerAdresse = isNotEmpty(getBrukerAdresse());
		return (gyldigSertifikat && hasLeverandorAdresse && hasBrukerAdresse);
	}
}
