package no.nav.dokdistkanal.consumer.personv3.to;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class PersonV3To {
	private LocalDate doedsdato;
	private LocalDate foedselsdato;
}
