package no.nav.dokdistkanal.consumer.pdl;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class HentPersoninfo {
	private LocalDate doedsdato;
	private LocalDate foedselsdato;
}