package no.nav.dokdistkanal.consumer.brreg;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record EnhetsRolleResponse(List<Roller> rollegrupper) {

	public record Roller(Type type, List<Rolle> roller) {
	}

	public record Rolle(Type type, Person person) {

	}

	public record Type(String kode, String beskrivelse) {
	}

	public record Person(LocalDate fodselsdato, boolean erDoed) {
	}
}
