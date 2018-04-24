package no.nav.dokdistkanal.consumer.dki.to;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@AllArgsConstructor(access= AccessLevel.PUBLIC)
@NoArgsConstructor(access= AccessLevel.PUBLIC)
public class DigitalKontaktinformasjonTo {
	private String epostadresse;
	private String mobiltelefonnummer;
	private boolean reservasjon;
	private String leverandoerAdresse;
	private String brukerAdresse;
	private boolean sertifikat;

	public boolean verifyAddress() {
		boolean hasLeverandorAdresse = StringUtils.isNotBlank(getLeverandoerAdresse());
		boolean hasBrukerAdresse = StringUtils.isNotBlank(getBrukerAdresse());
		return (sertifikat && hasLeverandorAdresse && hasBrukerAdresse);
	}

}
