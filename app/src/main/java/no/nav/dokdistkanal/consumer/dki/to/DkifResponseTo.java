package no.nav.dokdistkanal.consumer.dki.to;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Data
@Builder
@Jacksonized
public class DkifResponseTo {

	private Map<String, String> feil;
	private Map<String, DigitalKontaktinfo> personer;


	@Data
	@Builder
	@Jacksonized
	public static class DigitalKontaktinfo {
		private String epostadresse;
		private boolean kanVarsles;
		private String mobiltelefonnummer;
		private boolean reservert;
		private SikkerDigitalPostkasse sikkerDigitalPostkasse;
	}

	@Data
	@Builder
	@Jacksonized
	public static class SikkerDigitalPostkasse {
		private String adresse;
		private String leverandoerAdresse;
		private String leverandoerSertifikat;
	}
}