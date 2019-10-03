package no.nav.dokdistkanal.consumer.dki;

import static junit.framework.TestCase.assertNull;
import static org.assertj.core.api.Assertions.assertThat;

import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinfoMapper;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dki.to.DkifResponseTo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MapDigitalKontaktinformajonTest {

	private static final String EPOSTADRESSE = "epostadresse";
	private static final String MOBILTELEFONNUMMER = "mobiltelefonnummer";
	private static final String LEVERANDORADRESSE = "leverandøradresse";
	private static final String BRUKERADRESSE = "brukeradresse";
	private static final String LEVERANDOER_SERTIFIKAT = "leverandoerSertifikat";
	private static final boolean KAN_VARSLES_TRUE = true;
	private static final boolean KAN_VARSLES_FALSE = false;
	private static final boolean RESERVERT = false;

	private final DigitalKontaktinfoMapper digitalKontaktinfoMapper = new DigitalKontaktinfoMapper();


	@Test
	public void shouldMapOk() {
		DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinfoMapper.mapDigitalKontaktinformasjon(createDigitalKontaktinfo(KAN_VARSLES_TRUE));
		assertThat(digitalKontaktinformasjonTo.getBrukerAdresse().equals(BRUKERADRESSE));
		assertThat(digitalKontaktinformasjonTo.getEpostadresse().equals(EPOSTADRESSE));
		assertThat(digitalKontaktinformasjonTo.getLeverandoerAdresse().equals(LEVERANDORADRESSE));
		assertThat(digitalKontaktinformasjonTo.getMobiltelefonnummer().equals(MOBILTELEFONNUMMER));
	}

	@Test
	public void mapWithoutKanVarsles() {
		DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinfoMapper.mapDigitalKontaktinformasjon(createDigitalKontaktinfo(KAN_VARSLES_FALSE));
		assertThat(digitalKontaktinformasjonTo.getBrukerAdresse().equals(BRUKERADRESSE));
		assertThat(digitalKontaktinformasjonTo.getLeverandoerAdresse().equals(LEVERANDORADRESSE));
		assertNull(digitalKontaktinformasjonTo.getEpostadresse());
		assertNull(digitalKontaktinformasjonTo.getMobiltelefonnummer());
	}

	private DkifResponseTo.DigitalKontaktinfo createDigitalKontaktinfo(boolean kanVarsles) {
		return DkifResponseTo.DigitalKontaktinfo.builder()
				.epostadresse(EPOSTADRESSE)
				.mobiltelefonnummer(MOBILTELEFONNUMMER)
				.kanVarsles(kanVarsles)
				.reservert(RESERVERT)
				.sikkerDigitalPostkasse(DkifResponseTo.SikkerDigitalPostkasse.builder()
						.adresse(BRUKERADRESSE)
						.leverandoerAdresse(LEVERANDORADRESSE)
						.leverandoerSertifikat(LEVERANDOER_SERTIFIKAT)
						.build())
				.build();
	}
}
