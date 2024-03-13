package no.nav.dokdistkanal.consumer.brreg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EnhetsRolleResponse(List<Roller> rollegrupper) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Roller(Type type, List<Rolle> roller) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Rolle(Type type, Person person) {

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Type(String kode, String beskrivelse) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Person(String fodselsdato, boolean erDoed) {
	}
}
