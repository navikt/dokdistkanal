package no.nav.dokdistkanal.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DistribusjonKanalCode {
	PRINT(UtsendingkanalCode.S),
	SDP(UtsendingkanalCode.SDP),
	DITT_NAV(UtsendingkanalCode.NAV_NO),
	LOKAL_PRINT(UtsendingkanalCode.L),
	INGEN_DISTRIBUSJON(UtsendingkanalCode.INGEN_DISTRIBUSJON),
	TRYGDERETTEN(UtsendingkanalCode.TRYGDERETTEN),
	DPVT(UtsendingkanalCode.DPVT);

	private final UtsendingkanalCode utsendingskanalCode;
}
