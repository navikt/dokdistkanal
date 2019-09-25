package no.nav.dokdistkanal.consumer.tps.to;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TpsHentPersoninfoForIdentResponseTo {

	private String foedselsdato;
	private Doedsdato doedsdato;

	@Data
	public static class Doedsdato {
		private String dato;
	}
}

