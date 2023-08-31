package no.nav.dokdistkanal.consumer.pdl;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class HentPersoninfo {
	private LocalDate doedsdato;
	private LocalDate foedselsdato;

	public boolean erDoed() {
		return doedsdato != null;
	}

	public boolean harUkjentAlder() {
		return foedselsdato == null;
	}

	public boolean erUnderAtten() {
		return foedselsdato != null && foedselsdato.plusYears(18).isAfter(LocalDate.now());
	}
}