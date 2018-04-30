package no.nav.dokdistkanal.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.service.DokDistKanalService;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;

public class DokDistKanalRestControllerTest {

	private final static String FNR = "***gammelt_fnr***";
	private final static String DOKUMENTTYPEID = "DokumentType";
	private DokDistKanalRequest request;
	DokDistKanalService dokDistKanalService = mock(DokDistKanalService.class);
	DokDistKanalRestController dokDistKanalRestController = new DokDistKanalRestController(dokDistKanalService);

	@Test
	public void bestemKanalOK() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		request = DokDistKanalRequest.builder().dokumentTypeId(DOKUMENTTYPEID).mottakerId(FNR).build();
		DokDistKanalResponse response = DokDistKanalResponse.builder()
				.distribusjonsKanal(DistribusjonKanalCode.DITT_NAV)
				.build();
		when(dokDistKanalService.velgKanal(DOKUMENTTYPEID, FNR)).thenReturn(response);
		DokDistKanalResponse actualResponse = dokDistKanalRestController.bestemKanal(request, null);
		assertEquals(DistribusjonKanalCode.DITT_NAV, actualResponse.getDistribusjonsKanal());
		Mockito.verify(dokDistKanalService, Mockito.times(1)).velgKanal(anyString(), anyString());
	}
}
