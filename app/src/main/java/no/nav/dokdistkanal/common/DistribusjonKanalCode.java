package no.nav.dokdistkanal.common;

public enum DistribusjonKanalCode {
	PRINT(UtsendingkanalCode.S),
	SDP(UtsendingkanalCode.SDP),
	DITT_NAV(UtsendingkanalCode.NAV_NO),
	LOKAL_PRINT(UtsendingkanalCode.L),
	INGEN_DISTRIBUSJON(UtsendingkanalCode.INGEN_DISTRIBUSJON);

	private final UtsendingkanalCode utsendingskanalCode;

	DistribusjonKanalCode(UtsendingkanalCode code) {
		this.utsendingskanalCode = code;
	}

	public UtsendingkanalCode getUtsendingskanalCode() {
		return utsendingskanalCode;
	}
}
