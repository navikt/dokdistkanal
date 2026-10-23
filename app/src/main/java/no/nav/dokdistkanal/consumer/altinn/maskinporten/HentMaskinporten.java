package no.nav.dokdistkanal.consumer.altinn.maskinporten;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Protected
@RestController
@RequestMapping("/rest")
public class HentMaskinporten {

	private final MaskinportenConsumer maskinportenConsumer;

	public HentMaskinporten(MaskinportenConsumer maskinportenConsumer) {
		this.maskinportenConsumer = maskinportenConsumer;
	}

	@GetMapping("/maskinporten/token")
	public ResponseEntity<String> get() {
		try {
			return ResponseEntity.ok(maskinportenConsumer.getMaskinportenTokenDpo());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Feil ved henting av maskinporten token: " + e.getMessage());
		}
	}
}
