package no.nav.dokdistkanal.rest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.common.MottakerTypeCode;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.service.DokDistKanalService;
import org.junit.Test;
import org.mockito.Mockito;

public class DokDistKanalRestControllerTest {

	private final static String FNR = "***gammelt_fnr***";
	private final static String DOKUMENTTYPEID = "DokumentType";
	private final static MottakerTypeCode MOTTAKERTYPE_PERSON = MottakerTypeCode.PERSON;
	private final static Boolean ER_ARKIVERT_TRUE = Boolean.TRUE;

	private DokDistKanalRequest request;
	DokDistKanalService dokDistKanalService = mock(DokDistKanalService.class);
	DokDistKanalRestController dokDistKanalRestController = new DokDistKanalRestController(dokDistKanalService);

	@Test
	public void bestemKanalOK() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		request = DokDistKanalRequest.builder().dokumentTypeId(DOKUMENTTYPEID).mottakerId(FNR).mottakerType(MOTTAKERTYPE_PERSON).brukerId(FNR).build();
		DokDistKanalResponse response = DokDistKanalResponse.builder()
				.distribusjonsKanal(DistribusjonKanalCode.DITT_NAV)
				.build();
		when(dokDistKanalService.velgKanal(DOKUMENTTYPEID, FNR, MOTTAKERTYPE_PERSON, FNR, ER_ARKIVERT_TRUE)).thenReturn(response);
		DokDistKanalResponse actualResponse = dokDistKanalRestController.bestemKanal(request, null);
		assertEquals(DistribusjonKanalCode.DITT_NAV, actualResponse.getDistribusjonsKanal());
		Mockito.verify(dokDistKanalService, Mockito.times(1)).velgKanal(anyString(), anyString(), any(MottakerTypeCode.class), anyString(), ER_ARKIVERT_TRUE);
	}
}
