package no.nav.dokkanalvalg.consumer.dki.to;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DigitalKontaktinformasjonTo {
	private String epostadresse;
	private String mobiltelefon;
	private String reservasjon;
}
