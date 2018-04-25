package no.nav.dokdistkanal.consumer.personv3.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
public class PersonV3To {
	private LocalDate doedsdato;
	private LocalDate foedselsdato;
}
