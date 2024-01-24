package no.nav.dokdistkanal.domain;

import lombok.RequiredArgsConstructor;
import no.nav.dokdistkanal.common.DistribusjonKanalCode;

import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DITT_NAV;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DPVT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.INGEN_DISTRIBUSJON;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.LOKAL_PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.SDP;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.TRYGDERETTEN;

@RequiredArgsConstructor
public enum BestemDistribusjonskanalRegel {
	TEMA_HAR_BEGRENSET_INNSYN(PRINT, "Tema har begrenset innsyn"),
	SKAL_IKKE_ARKIVERES(PRINT, "Skal ikke arkiveres"),
	PREDEFINERT_LOKAL_PRINT(LOKAL_PRINT, "Brevets dokumenttype har predefinert distribusjonskanal som er Lokal Print"),
	PREDEFINERT_INGEN_DISTRIBUSJON(INGEN_DISTRIBUSJON, "Predefinert distribusjonskanal er Ingen Distribusjon"),
	PREDEFINERT_TRYGDERETTEN(TRYGDERETTEN, "Predefinert distribusjonskanal er Trygderetten"),
	ORGANISASJON_MED_INFOTRYGD_DOKUMENT(PRINT, "Mottaker er organisasjon og dokument er produsert i infotrygd"),
	ORGANISASJON_MED_ALTINN_INFO(DPVT, "Mottaker er organisasjon og er en gyldig altinn-serviceowner notifikasjonsmottaker"),
	ORGANISASJON_UTEN_ALTINN_INFO(PRINT, "Mottaker er organisasjon og mangler varslingsinformasjon for DPVT"),
	PERSON_ER_IKKE_I_PDL(PRINT, "Finner ikke personen i PDL"),
	PERSON_ER_DOED(PRINT, "Personen er død"),
	PERSON_HAR_UKJENT_ALDER(PRINT, "Personen har ukjent alder"),
	PERSON_ER_UNDER_18(PRINT, "Personen må være minst 18 år"),
	FINNER_IKKE_DIGITAL_KONTAKTINFORMASJON(PRINT, "Finner ikke digital kontaktinformasjon"),
	BRUKER_ER_RESERVERT(PRINT, "Bruker har reservert seg mot digital kommunikasjon"),
	BRUKER_SDP_MANGLER_VARSELINFO(PRINT, "Bruker skal varsles, men finner hverken mobiltelefonnummer eller e-postadresse"),
	BRUKER_HAR_GYLDIG_SDP_ADRESSE(SDP, "Bruker har gyldig digitalt postkassesertifikat, leverandøradresse og brukeradresse"),
	BRUKER_MANGLER_EPOST_OG_TELEFON(PRINT, "Bruker mangler mangler både e-post og telefonnummer"),
	BRUKER_OG_MOTTAKER_ER_FORSKJELLIG(PRINT, "Bruker og mottaker er forskjellig"),
	DOKUMENT_ER_IKKE_ARKIVERT(PRINT, "Dokumentet er ikke arkivert"),
	MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON(PRINT, "Mottaker er ikke person eller organisasjon"),
	BRUKER_HAR_GYLDIG_EPOST_ELLER_MOBILNUMMER(DITT_NAV, "Bruker har gyldig e-post og/eller mobilnummer"),
	PERSON_STANDARD_PRINT(PRINT, "Standard distribusjonskanal for personer er Print");

	public final DistribusjonKanalCode distribusjonKanal;
	public final String begrunnelse;

}