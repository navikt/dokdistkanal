package no.nav.dokdistkanal.consumer.dokmet.map;

import no.nav.dokdistkanal.consumer.dokmet.DokumentTypeInfoTo;
import no.nav.dokdistkanal.consumer.dokmet.to.DokumentTypeInfoToV4;

import static no.nav.dokdistkanal.common.DistribusjonKanalCode.SDP;

public class DokumenttypeInfoMapper {

	public static DokumentTypeInfoTo mapTo(DokumentTypeInfoToV4 dokumentTypeInfoToV4) {
		String predefinertDistribusjonKanal = null;

		if (dokumentTypeInfoToV4.getDokumentProduksjonsInfo() != null &&
			dokumentTypeInfoToV4.getDokumentProduksjonsInfo().getDistribusjonInfo() != null) {

			predefinertDistribusjonKanal = dokumentTypeInfoToV4.getDokumentProduksjonsInfo()
					.getDistribusjonInfo()
					.getPredefinertDistKanal();
		}

		if (dokumentTypeInfoToV4.getDokumentProduksjonsInfo() == null ||
			dokumentTypeInfoToV4.getDokumentProduksjonsInfo().getDistribusjonInfo() == null ||
			dokumentTypeInfoToV4.getDokumentProduksjonsInfo().getDistribusjonInfo().getDistribusjonVarsels() == null) {

			return DokumentTypeInfoTo.builder()
					.arkivsystem(dokumentTypeInfoToV4.getArkivSystem())
					.predefinertDistKanal(predefinertDistribusjonKanal)
					.isVarslingSdp(false)
					.build();
		} else {

			return DokumentTypeInfoTo.builder()
					.arkivsystem(dokumentTypeInfoToV4.getArkivSystem())
					.predefinertDistKanal(predefinertDistribusjonKanal)
					.isVarslingSdp(dokumentTypeInfoToV4.getDokumentProduksjonsInfo()
							.getDistribusjonInfo()
							.getDistribusjonVarsels()
							.stream()
							.anyMatch(distribusjonVarselTo -> SDP.toString()
									.equals(distribusjonVarselTo.getVarselForDistribusjonKanal()))).build();
		}
	}

}
