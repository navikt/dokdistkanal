package no.nav.dokdistkanal.consumer.dki.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DkifResponseTo {

	private Map<String, Melding> feil;
	private Map<String, DigitalKontaktinfo> kontaktinfo;

	@Data
	public static class Melding {
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