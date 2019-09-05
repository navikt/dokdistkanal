package no.nav.dokdistkanal.consumer.dki.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DigitalKontaktinformasjonTo implements Serializable {

	static final long serialVersionUID = 1L;
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
