package no.nav.dokdistkanal.consumer.tps.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TpsHentPersoninfoForIdentTo {
	private LocalDate doedsdato;
	private LocalDate foedselsdato;
}