package no.nav.dokdistkanal.consumer.dokmet.map;

import no.nav.dokdistkanal.consumer.dokmet.DokumentTypeInfoTo;
import no.nav.dokdistkanal.consumer.dokmet.to.DistribusjonInfoTo;
import no.nav.dokdistkanal.consumer.dokmet.to.DistribusjonVarselTo;
import no.nav.dokdistkanal.consumer.dokmet.to.DokumentProduksjonsInfoToV4;
import no.nav.dokdistkanal.consumer.dokmet.to.DokumentTypeInfoToV4;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class DokumenttypeInfoMapperTest {

	private static final String ARKIVSYSTEM = "arkivsystem";
	private static final String PREDEFINERT_DISTRIBUSJONSKANAL = "EMAIL";
	private static final String DISTRIBUSJONSKANAL = "SDP";

	@ParameterizedTest
	@ValueSource(strings = {PREDEFINERT_DISTRIBUSJONSKANAL})
	@NullSource
	public void skalMappePredefinertDistribusjonskanal(String predefinertDistribusjonskanal) {
		var input = DokumentTypeInfoToV4.builder()
				.arkivSystem(ARKIVSYSTEM)
				.dokumentProduksjonsInfo(DokumentProduksjonsInfoToV4.builder()
						.distribusjonInfo(predefinertDistribusjonskanal == null ? null :
								DistribusjonInfoTo.builder()
										.predefinertDistKanal(predefinertDistribusjonskanal)
										.build())
						.build())
				.build();

		DokumentTypeInfoTo result = DokumenttypeInfoMapper.mapTo(input);

		assertThat(result.getPredefinertDistKanal()).isEqualTo(predefinertDistribusjonskanal);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void skalMappeVarslingSDP(boolean isVarslingSdp) {
		var input = DokumentTypeInfoToV4.builder()
				.arkivSystem(ARKIVSYSTEM)
				.dokumentProduksjonsInfo(DokumentProduksjonsInfoToV4.builder()
						.distribusjonInfo(DistribusjonInfoTo.builder()
								.predefinertDistKanal(PREDEFINERT_DISTRIBUSJONSKANAL)
								.distribusjonVarsels(isVarslingSdp ? singletonList(DistribusjonVarselTo.builder()
										.varselForDistribusjonKanal(DISTRIBUSJONSKANAL)
										.build()) : null)
								.build())
						.build())
				.build();

		DokumentTypeInfoTo result = DokumenttypeInfoMapper.mapTo(input);

		assertThat(result.getArkivsystem()).isEqualTo(ARKIVSYSTEM);
		assertThat(result.getPredefinertDistKanal()).isEqualTo(PREDEFINERT_DISTRIBUSJONSKANAL);
		assertThat(result.isVarslingSdp()).isEqualTo(isVarslingSdp);
	}

}