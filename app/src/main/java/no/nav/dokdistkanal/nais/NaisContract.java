package no.nav.dokdistkanal.nais;

import static no.nav.dokdistkanal.metrics.PrometheusMetrics.isReady;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.nais.checks.DigitalKontaktinfoV1Check;
import no.nav.dokdistkanal.nais.checks.DokumenttypeInfoV3Check;
import no.nav.dokdistkanal.nais.checks.PersonV3Check;
import no.nav.dokdistkanal.nais.checks.SikkerhetsnivaaV1Check;
import no.nav.dokdistkanal.nais.selftest.support.Result;
import no.nav.dokdistkanal.nais.selftest.support.SelftestCheck;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */

@Slf4j
@RestController
public class NaisContract {
	
	private static final String APPLICATION_ALIVE = "Application is alive!";
	private static final String APPLICATION_READY = "Application is ready for traffic!";
	private static final String APPLICATION_NOT_READY = "Application is not ready for traffic :-(";

	private final PersonV3Check personV3Check;
	private final DokumenttypeInfoV3Check dokumenttypeInfoV3Check;
	private final DigitalKontaktinfoV1Check digitalKontaktinfoV1Check;
	private final SikkerhetsnivaaV1Check sikkerhetsnivaaV1Check;

	@Inject
	public NaisContract(PersonV3Check personV3Check, DokumenttypeInfoV3Check dokumenttypeInfoV3Check, DigitalKontaktinfoV1Check digitalKontaktinfoV1Check, SikkerhetsnivaaV1Check sikkerhetsnivaaV1Check)
	{
		this.personV3Check = personV3Check;
		this.dokumenttypeInfoV3Check = dokumenttypeInfoV3Check;
		this.digitalKontaktinfoV1Check = digitalKontaktinfoV1Check;
		this.sikkerhetsnivaaV1Check = sikkerhetsnivaaV1Check;
	}

	@GetMapping("/isAlive")
	public String isAlive() {
		return APPLICATION_ALIVE;
	}

	@ResponseBody
	@RequestMapping(value = "/isReady", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity isReady() throws Exception {
		try {
			List<SelftestCheck> results = new ArrayList<>();

			results.add(personV3Check.check());
			results.add(dokumenttypeInfoV3Check.check());
			results.add(digitalKontaktinfoV1Check.check());
			results.add(sikkerhetsnivaaV1Check.check());

			if (isAnyDependencyUnhealthy(results.stream().map(SelftestCheck::getResult).collect(Collectors.toList()))) {
				isReady.dec();
				String responseBody = APPLICATION_NOT_READY + "/n +  " +  results.stream().map(SelftestCheck::getErrorMessage).collect(Collectors.toList());
				return new ResponseEntity<>(responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
			}

			isReady.set(1);

			return new ResponseEntity<>(APPLICATION_READY, HttpStatus.OK);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}


	private boolean isAnyDependencyUnhealthy(List<Result> results) {
		return results.stream().anyMatch((result) -> result.equals(Result.ERROR) || result.equals(Result.WARNING));
	}

}
