package no.nav.dokdistkanal.consumer.tps;

import no.nav.dokdistkanal.consumer.tps.to.TpsHentPersoninfoForIdentTo;
import no.nav.dokdistkanal.exceptions.technical.TpsHentNavnTechnicalException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TpsConsumerTest {
    private static final String FNR = "99999999999";
    private static final LocalDate FOEDSELSDATO = LocalDate.of(1899, 12, 31);
    private static final LocalDate DOEDSDATO = LocalDate.of(1999, 12, 31);

    private final TpsConsumer tpsConsumer = mock(TpsConsumer.class);

    @Test
    public void shouldHentPersonOK() {
        when(tpsConsumer.tpsHentPersoninfoForIdent(any(String.class))).thenReturn(createResponse());

        TpsHentPersoninfoForIdentTo personinfoForIdentTo = tpsConsumer.tpsHentPersoninfoForIdent(FNR);

        assertThat(personinfoForIdentTo.getDoedsdato(), is(DOEDSDATO));
        assertThat(personinfoForIdentTo.getFoedselsdato(), is(FOEDSELSDATO));

    }

    @Test
    public void shouldThrowFunctionalException() throws Exception {
        when(tpsConsumer.tpsHentPersoninfoForIdent(any(String.class))).thenReturn(null);

        TpsHentPersoninfoForIdentTo personinfoForIdentTo = tpsConsumer.tpsHentPersoninfoForIdent(FNR);

        assertNull(personinfoForIdentTo);
    }

    @Test
    public void shouldThrowTechnicalException() throws Exception {
        when(tpsConsumer.tpsHentPersoninfoForIdent(any(String.class))).thenThrow(new TpsHentNavnTechnicalException("Feil oppstått"));
        assertThrows(TpsHentNavnTechnicalException.class, () -> tpsConsumer.tpsHentPersoninfoForIdent(FNR));
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
