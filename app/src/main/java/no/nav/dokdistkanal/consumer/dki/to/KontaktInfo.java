package no.nav.dokdistkanal.consumer.dki.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KontaktInfo {

	private Feil feil;
	private DigitalKontaktinfo digitalKontaktinfo;

	@Data
	public static class Feil {
		private String melding;
	}

	@Data
	public static class DigitalKontaktinfo {
		private String epostadresse;
		private String mobiltelefonnummer;
		private boolean reservert;
		private SikkerDigitalPostkasse sikkerDigitalPostkasse;
	}

	@Data
	public static class SikkerDigitalPostkasse {
		private String adresse;
		private String leverandoerAdresse;
		private String leverandoerSertifikat;
	}
}