package no.nav.dokdistkanal.consumer.dki.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DigitalKontaktinfo {

	private String epostadresse;
	private boolean kanVarsles;
	private String mobiltelefonnummer;
	private boolean reservert;
	private SikkerDigitalPostkasse sikkerDigitalPostkasse;

	@Data
	@Builder
	public static class SikkerDigitalPostkasse {
		private String adresse;
		private String leverandoerAdresse;
		private String leverandoerSertifikat;
	}
}