package no.nav.dokdistkanal.service;

import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.serviceregistry.DpoServiceRegistryService;
import no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel;
import no.nav.dokdistkanal.domain.Enhetsopplysninger;
import no.nav.dokdistkanal.domain.Enhetsopplysninger.Hovedenhet;
import no.nav.dokdistkanal.domain.Enhetsopplysninger.Underenhet;
import no.nav.dokdistkanal.exceptions.functional.EnhetSlettetException;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static no.nav.dokdistkanal.common.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_ER_KONKURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganisasjonDistribusjonKanalServiceTest {

	private static final String HOVEDENHET_ORGNR = "974761076";
	private static final String UNDERENHET_ORGNR = "916007922";
	private static final LocalDate SLETTEDATO = LocalDate.of(2026, 10, 1);

	private static final Enhetsopplysninger SLETTET_UNDERENHET = Enhetsopplysninger.fraUnderenhet(new Underenhet(UNDERENHET_ORGNR, null, SLETTEDATO));
	private static final Enhetsopplysninger SLETTET_HOVEDENHET = Enhetsopplysninger.fraHovedenhet(new Hovedenhet(HOVEDENHET_ORGNR, false, SLETTEDATO));
	private static final Enhetsopplysninger UNDERENHET_MED_SLETTET_HOVEDENHET = new Enhetsopplysninger(new Underenhet(UNDERENHET_ORGNR, HOVEDENHET_ORGNR, null), SLETTET_HOVEDENHET.hovedenhet());
	private static final Enhetsopplysninger KONKURS_HOVEDENHET = Enhetsopplysninger.fraHovedenhet(new Hovedenhet(HOVEDENHET_ORGNR, true, null));
	private static final Enhetsopplysninger KONKURS_OG_SLETTET_HOVEDENHET = Enhetsopplysninger.fraHovedenhet(new Hovedenhet(HOVEDENHET_ORGNR, true, SLETTEDATO));
	private static final Enhetsopplysninger UNDERENHET_MED_KONKURS_HOVEDENHET = new Enhetsopplysninger(new Underenhet(UNDERENHET_ORGNR, HOVEDENHET_ORGNR, null), KONKURS_HOVEDENHET.hovedenhet());
	private static final Enhetsopplysninger INGEN_ENHETER = new Enhetsopplysninger(null, null);
	private static final Enhetsopplysninger UNDERENHET_UTEN_HOVEDENHET = Enhetsopplysninger.fraUnderenhet(new Underenhet(UNDERENHET_ORGNR, HOVEDENHET_ORGNR, null));

	private BrregEnhetsregisterService brregEnhetsregisterService;
	private OrganisasjonDistribusjonKanalService organisasjonDistribusjonKanalService;

	@BeforeEach
	void setup() {
		brregEnhetsregisterService = mock(BrregEnhetsregisterService.class);
		organisasjonDistribusjonKanalService = new OrganisasjonDistribusjonKanalService(
				brregEnhetsregisterService,
				mock(DpoServiceRegistryService.class),
				new DokdistkanalProperties());
	}

	@ParameterizedTest
	@MethodSource
	void skalVelgeDistribusjonskanalBasertPaaEnhetsopplysninger(Enhetsopplysninger opplysninger, BestemDistribusjonskanalRegel regel) {
		var request = new BestemDistribusjonskanalRequest(UNDERENHET_ORGNR, UNDERENHET_ORGNR, "PEN", "000000", true, null, null, null);
		when(brregEnhetsregisterService.hentEnhetsopplysninger(UNDERENHET_ORGNR)).thenReturn(opplysninger);

		var response = organisasjonDistribusjonKanalService.validerOrgNrOgBestemKanal(request);

		assertThat(response.distribusjonskanal()).isEqualTo(PRINT);
		assertThat(response.regel()).isEqualTo(regel.name());

		verify(brregEnhetsregisterService, never()).harEnhetenGyldigRolletypeForDpvt(anyString());
	}

	private static Stream<Arguments> skalVelgeDistribusjonskanalBasertPaaEnhetsopplysninger() {
		return Stream.of(
				Arguments.of(KONKURS_HOVEDENHET, ORGANISASJON_ER_KONKURS),
				Arguments.of(KONKURS_OG_SLETTET_HOVEDENHET, ORGANISASJON_ER_KONKURS),
				Arguments.of(UNDERENHET_MED_KONKURS_HOVEDENHET, ORGANISASJON_ER_KONKURS),
				Arguments.of(INGEN_ENHETER, MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON),
				Arguments.of(UNDERENHET_UTEN_HOVEDENHET, MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON)
		);
	}

	@ParameterizedTest
	@MethodSource
	void skalFeileForSlettetVirksomhet(Enhetsopplysninger opplysninger, String mottakerId, String slettetOrgNr) {
		var request = new BestemDistribusjonskanalRequest(mottakerId, mottakerId, "PEN", "000000", true, null, null, null);
		when(brregEnhetsregisterService.hentEnhetsopplysninger(mottakerId)).thenReturn(opplysninger);

		assertThatThrownBy(() -> organisasjonDistribusjonKanalService.validerOrgNrOgBestemKanal(request))
				.isInstanceOf(EnhetSlettetException.class)
				.hasMessage("Virksomheten med organisasjonsnummer=%s er slettet per %s. Forsendelsen kan ikke distribueres."
						.formatted(slettetOrgNr, SLETTEDATO));

		verify(brregEnhetsregisterService, never()).harEnhetenGyldigRolletypeForDpvt(anyString());
	}

	private static Stream<Arguments> skalFeileForSlettetVirksomhet() {
		return Stream.of(
				Arguments.of(SLETTET_UNDERENHET, UNDERENHET_ORGNR, UNDERENHET_ORGNR),
				Arguments.of(SLETTET_HOVEDENHET, HOVEDENHET_ORGNR, HOVEDENHET_ORGNR),
				Arguments.of(UNDERENHET_MED_SLETTET_HOVEDENHET, UNDERENHET_ORGNR, HOVEDENHET_ORGNR)
		);
	}

}
