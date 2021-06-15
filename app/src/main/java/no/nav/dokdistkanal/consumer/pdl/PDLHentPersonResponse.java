package no.nav.dokdistkanal.consumer.pdl;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PDLHentPersonResponse {

	private PDLHentPerson data;
	private List<PdlError> errors;

	@Data
	public static class PDLHentPerson {
		private HentPerson hentPerson;
	}

	@Data
	static class HentPerson {
		private List<Foedsel> foedsel;
		private List<Doedsfall> doedsfall;
	}

	@Data
	static class Foedsel {
		@ToString.Exclude
		private LocalDate foedselsdato;
		@ToString.Exclude
		private Integer foedselsaar;
	}

	@Data
	static class Doedsfall {
		@ToString.Exclude
		private LocalDate doedsdato;
	}

	@Data
	static class PdlError {
		private String message;
		private PdlErrorExtensionTo extensions;
	}

	@Data
	static class PdlErrorExtensionTo {
		private String classification;
	}
}
