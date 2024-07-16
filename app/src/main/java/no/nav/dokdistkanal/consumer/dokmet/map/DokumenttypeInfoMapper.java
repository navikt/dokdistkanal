package no.nav.dokdistkanal.consumer.dokmet.map;

import no.nav.dokdistkanal.consumer.dokmet.DokumentTypeKanalInfo;
import no.nav.dokdistkanal.consumer.dokmet.to.DokumentTypeInfoTo;

import static no.nav.dokdistkanal.common.DistribusjonKanalCode.SDP;

public class DokumenttypeInfoMapper {

	public static DokumentTypeKanalInfo mapTo(DokumentTypeInfoTo dokumentTypeInfoTo) {
		String predefinertDistribusjonskanal = null;

		if (distribusjonsinfoErSatt(dokumentTypeInfoTo)) {

			predefinertDistribusjonskanal = dokumentTypeInfoTo.getDokumentProduksjonsInfo()
					.getDistribusjonInfo()
					.getPredefinertDistKanal();
		}

		if (manglerDistribusjonsvarsler(dokumentTypeInfoTo)) {

			return DokumentTypeKanalInfo.builder()
					.arkivsystem(dokumentTypeInfoTo.getArkivSystem())
					.predefinertDistKanal(predefinertDistribusjonskanal)
					.isVarslingSdp(false)
					.build();
		} else {

			return DokumentTypeKanalInfo.builder()
					.arkivsystem(dokumentTypeInfoTo.getArkivSystem())
					.predefinertDistKanal(predefinertDistribusjonskanal)
					.isVarslingSdp(dokumentTypeInfoTo.getDokumentProduksjonsInfo()
							.getDistribusjonInfo()
							.getDistribusjonVarsels()
							.stream()
							.anyMatch(distribusjonVarselTo -> SDP.toString()
									.equals(distribusjonVarselTo.getVarselForDistribusjonKanal()))).build();
		}
	}

	private static boolean manglerDistribusjonsvarsler(DokumentTypeInfoTo dokumentTypeInfoTo) {
		return dokumentTypeInfoTo.getDokumentProduksjonsInfo() == null ||
			   dokumentTypeInfoTo.getDokumentProduksjonsInfo().getDistribusjonInfo() == null ||
			   dokumentTypeInfoTo.getDokumentProduksjonsInfo().getDistribusjonInfo().getDistribusjonVarsels() == null;
	}

	private static boolean distribusjonsinfoErSatt(DokumentTypeInfoTo dokumentTypeInfoTo) {
		return dokumentTypeInfoTo.getDokumentProduksjonsInfo() != null &&
			   dokumentTypeInfoTo.getDokumentProduksjonsInfo().getDistribusjonInfo() != null;
	}

}
