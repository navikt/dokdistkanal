package no.nav.dokdistkanal.consumer.tps;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.dokdistkanal.consumer.tps.to.TpsHentPersoninfoForIdentTo;
import no.nav.dokdistkanal.exceptions.functional.TpsHentNavnFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.TpsHentNavnTechnicalException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.LocalDate;
import java.time.ZoneId;

public class TpsConsumerTest {
	private static final String FNR = "***gammelt_fnr***";
	private static final LocalDate FOEDSELSDATO = LocalDate.of(1899, 12, 31);
	private static final LocalDate DOEDSDATO = LocalDate.of(1999, 12, 31);
	private static final String PRINCIPAL = "SRVDOKPROD";

	private final TpsConsumer tpsConsumer = mock(TpsConsumer.class);

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void shouldHentPersonOK() {
		when(tpsConsumer.tpsHentPersoninfoForIdent(any(String.class), any(String.class))).thenReturn(createResponse());

		TpsHentPersoninfoForIdentTo personinfoForIdentTo = tpsConsumer.tpsHentPersoninfoForIdent(FNR, PRINCIPAL);

		assertThat(personinfoForIdentTo.getDoedsdato(), is(DOEDSDATO));
		assertThat(personinfoForIdentTo.getFoedselsdato(), is(FOEDSELSDATO));

	}

	@Test
	public void shouldThrowFunctionalException() throws Exception {
		when(tpsConsumer.tpsHentPersoninfoForIdent(any(String.class), any(String.class))).thenThrow(new TpsHentNavnFunctionalException("Fant ikke person"));
		expectedException.expect(TpsHentNavnFunctionalException.class);

		TpsHentPersoninfoForIdentTo personinfoForIdentTo = tpsConsumer.tpsHentPersoninfoForIdent(FNR, PRINCIPAL);

		assertThat(personinfoForIdentTo, nullValue());
	}

	@Test
	public void shouldThrowTechnicalException() throws Exception {
		when(tpsConsumer.tpsHentPersoninfoForIdent(any(String.class), any(String.class))).thenThrow(new TpsHentNavnTechnicalException("Feil oppstått"));
		expectedException.expect(TpsHentNavnTechnicalException.class);

		tpsConsumer.tpsHentPersoninfoForIdent(FNR, PRINCIPAL);
	}

	private TpsHentPersoninfoForIdentTo createResponse() {
		TpsHentPersoninfoForIdentTo response = new TpsHentPersoninfoForIdentTo();
		LocalDate doedsdato;
		LocalDate foedselsDato;

		doedsdato = LocalDate.from(DOEDSDATO.atStartOfDay(ZoneId.systemDefault()));
		foedselsDato = LocalDate.from(FOEDSELSDATO.atStartOfDay(ZoneId.systemDefault()));

		response.setDoedsdato(doedsdato);
		response.setFoedselsdato(foedselsDato);

		return response;
	}


}
