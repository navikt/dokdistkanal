package no.nav.dokdistkanal.consumer.dki.to;

import static no.nav.dokdistkanal.common.FunctionalUtils.isNotEmpty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
	private boolean sertifikat;

	public boolean verifyAddress() {
		boolean hasLeverandorAdresse = isNotEmpty(getLeverandoerAdresse());
		boolean hasBrukerAdresse = isNotEmpty(getBrukerAdresse());
		return (sertifikat && hasLeverandorAdresse && hasBrukerAdresse);
	}
}
