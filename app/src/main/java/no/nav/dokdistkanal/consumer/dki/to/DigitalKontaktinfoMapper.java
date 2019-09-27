package no.nav.dokdistkanal.consumer.dki.to;

public class DigitalKontaktinfoMapper {

	public DigitalKontaktinformasjonTo mapDigitalKontaktinformasjon(DkifResponseTo.DigitalKontaktinfo digitalKontaktinfo) {

		if (digitalKontaktinfo == null) {
			return null;
		} else {
			return DigitalKontaktinformasjonTo.builder()
					.brukerAdresse(digitalKontaktinfo.getSikkerDigitalPostkasse() != null ? digitalKontaktinfo.getSikkerDigitalPostkasse()
							.getAdresse() : null)
					.epostadresse(digitalKontaktinfo.isKanVarsles() ? digitalKontaktinfo.getEpostadresse() : null)
					.leverandoerAdresse(digitalKontaktinfo.getSikkerDigitalPostkasse() != null ? digitalKontaktinfo.getSikkerDigitalPostkasse()
							.getLeverandoerAdresse() : null)
					.mobiltelefonnummer(digitalKontaktinfo.isKanVarsles() ? digitalKontaktinfo.getMobiltelefonnummer() : null)
					.reservasjon(digitalKontaktinfo.isReservert())
					.sertifikat(digitalKontaktinfo.getSikkerDigitalPostkasse() != null && isSertifikat(digitalKontaktinfo.getSikkerDigitalPostkasse()
							.getLeverandoerSertifikat()))
					.build();
		}
	}

	private boolean isSertifikat(String leverandoerSertifikat) {
		return leverandoerSertifikat != null && !leverandoerSertifikat.isEmpty();
	}
}
