package no.nav.dokdistkanal.rest;

import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.service.DokDistKanalService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
public class DokDistKanalRestController {

	public static final String REST = "/rest/";
	public static final String BESTEM_KANAL_URI_PATH = REST + "bestemKanal";

	private final DokDistKanalService dokDistKanalService;

	@Inject
	public DokDistKanalRestController (DokDistKanalService dokDistKanalService) {
		this.dokDistKanalService = dokDistKanalService;
	}

	@ResponseBody
	@PostMapping(value = BESTEM_KANAL_URI_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public DokDistKanalResponse bestemKanal(@RequestBody DokDistKanalRequest request) {
		return dokDistKanalService.velgKanal(request.getPersonIdent(), request.getDokumentTypeId());
	}
}
