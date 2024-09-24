package no.nav.dokdistkanal.consumer.pdl;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
public class PDLHentPersonResponse {

	private PDLHentPerson data;
	private List<PdlError> errors;

	@Data
	public static class PDLHentPerson {
		private HentPerson hentPerson;
	}

	@Data
	static class HentPerson {
		private List<Foedselsdato> foedselsdato;
		private List<Doedsfall> doedsfall;
	}

	@Data
	static class Foedselsdato {
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
		private String code;
		private ErrorDetails details;
		private String classification;
	}

	@Data
	static class ErrorDetails {
		private String type;
		private String cause;
		private String policy;
	}
}
