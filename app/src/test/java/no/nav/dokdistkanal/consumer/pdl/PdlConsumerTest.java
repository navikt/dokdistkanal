package no.nav.dokdistkanal.consumer.pdl;

import no.nav.dokdistkanal.exceptions.functional.PdlFunctionalException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PdlConsumerTest {
    private static final String FNR = "99999999999";
    private static final LocalDate FOEDSELSDATO = LocalDate.of(1899, 12, 31);
    private static final LocalDate DOEDSDATO = LocalDate.of(1999, 12, 31);

    private final PdlGraphQLConsumer pdlConsumer = mock(PdlGraphQLConsumer.class);

    @Test
    public void shouldHentPersonOK() {
        when(pdlConsumer.hentPerson(anyString(), anyString())).thenReturn(createHentPersonInfo());
        HentPersoninfo hentPersoninfo = pdlConsumer.hentPerson(FNR, "PEN");

        assertNull(hentPersoninfo.getDoedsdato());
        assertThat(hentPersoninfo.getFoedselsdato(), is(FOEDSELSDATO));
    }

    @Test
    public void shouldHentFoedselsOGDoedsDato() {
        when(pdlConsumer.hentPerson(anyString(), anyString())).thenReturn(createHentPersonInfoMedDatoe());

        HentPersoninfo hentPersoninfo = pdlConsumer.hentPerson(FNR, "PEN");

        assertThat(hentPersoninfo.getDoedsdato(), is(DOEDSDATO));
        assertThat(hentPersoninfo.getFoedselsdato(), is(FOEDSELSDATO));
    }

    @Test
    public void shouldThrowTechnicalException() {
        when(pdlConsumer.hentPerson(anyString(), anyString())).thenThrow(new PdlFunctionalException("Kunne ikke hente person fra Pdl"));
        assertThrows(PdlFunctionalException.class, () -> pdlConsumer.hentPerson(FNR, "PEN"));
    }


    private HentPersoninfo createHentPersonInfo() {
        return HentPersoninfo.builder().foedselsdato(FOEDSELSDATO).doedsdato(null).build();

    }

    private HentPersoninfo createHentPersonInfoMedDatoe() {
        return HentPersoninfo.builder().foedselsdato(FOEDSELSDATO).doedsdato(DOEDSDATO).build();

    }

}
