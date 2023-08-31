package no.nav.dokdistkanal.consumer.dokmet.map;

import no.nav.dokdistkanal.consumer.dokmet.DokumentTypeInfoTo;
import no.nav.dokdistkanal.consumer.dokmet.to.DokumentTypeInfoToV4;

import static no.nav.dokdistkanal.common.DistribusjonKanalCode.SDP;

public class DokumenttypeInfoMapper {

	public static DokumentTypeInfoTo mapTo(DokumentTypeInfoToV4 dokumentTypeInfoToV4) {
		String predefinertDistribusjonskanal = null;

		if (distribusjonsinfoErSatt(dokumentTypeInfoToV4)) {

			predefinertDistribusjonskanal = dokumentTypeInfoToV4.getDokumentProduksjonsInfo()
					.getDistribusjonInfo()
					.getPredefinertDistKanal();
		}

		if (manglerDistribusjonsvarsler(dokumentTypeInfoToV4)) {

			return DokumentTypeInfoTo.builder()
					.arkivsystem(dokumentTypeInfoToV4.getArkivSystem())
					.predefinertDistKanal(predefinertDistribusjonskanal)
					.isVarslingSdp(false)
					.build();
		} else {

			return DokumentTypeInfoTo.builder()
					.arkivsystem(dokumentTypeInfoToV4.getArkivSystem())
					.predefinertDistKanal(predefinertDistribusjonskanal)
					.isVarslingSdp(dokumentTypeInfoToV4.getDokumentProduksjonsInfo()
							.getDistribusjonInfo()
							.getDistribusjonVarsels()
							.stream()
							.anyMatch(distribusjonVarselTo -> SDP.toString()
									.equals(distribusjonVarselTo.getVarselForDistribusjonKanal()))).build();
		}
	}

	private static boolean manglerDistribusjonsvarsler(DokumentTypeInfoToV4 dokumentTypeInfoToV4) {
		return dokumentTypeInfoToV4.getDokumentProduksjonsInfo() == null ||
			   dokumentTypeInfoToV4.getDokumentProduksjonsInfo().getDistribusjonInfo() == null ||
			   dokumentTypeInfoToV4.getDokumentProduksjonsInfo().getDistribusjonInfo().getDistribusjonVarsels() == null;
	}

	private static boolean distribusjonsinfoErSatt(DokumentTypeInfoToV4 dokumentTypeInfoToV4) {
		return dokumentTypeInfoToV4.getDokumentProduksjonsInfo() != null &&
			   dokumentTypeInfoToV4.getDokumentProduksjonsInfo().getDistribusjonInfo() != null;
	}

}
