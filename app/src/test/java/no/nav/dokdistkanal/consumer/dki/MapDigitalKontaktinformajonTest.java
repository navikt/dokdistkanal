package no.nav.dokdistkanal.consumer.dki;

import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinfoMapper;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dki.to.DkifResponseTo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class MapDigitalKontaktinformajonTest {

    private static final String EPOSTADRESSE = "epostadresse";
    private static final String MOBILTELEFONNUMMER = "mobiltelefonnummer";
    private static final String LEVERANDORADRESSE = "leverandøradresse";
    private static final String BRUKERADRESSE = "brukeradresse";
    private static final String LEVERANDOER_SERTIFIKAT_GYLDIG = "MIIFDTCCA/WgAwIBAgILAjRkHHb8c+nu3qcwDQYJKoZIhvcNAQELBQAwUTELMAkGA1UEBhMCTk8xHTAbBgNVBAoMFEJ1eXBhc3MgQVMtOTgzMTYzMzI3MSMwIQYDVQQDDBpCdXlwYXNzIENsYXNzIDMgVGVzdDQgQ0EgMzAeFw0xOTExMTUxMDQ4MjJaFw0yMjExMTUyMjU5MDBaMFQxCzAJBgNVBAYTAk5PMRUwEwYDVQQKDAxFLUJPS1MgTk9SR0UxGjAYBgNVBAMMEUUtQk9LUyBOT1JHRSBURVNUMRIwEAYDVQQFEwk5MjIwMjAxNzUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC8WhXBQUdmssRJA6LPKhaSmP9t3gtR9lDdeXzEkl4PB7upgt+fSUxrnwkQkSjOr7FiWt8luv419CgKMqTHiC4WGkVZgFTcfPZTGStUvJNng5VnwxiLxOwowovmClzuLOD18/+6KdDp97EMvbURLD34pJ/2g19QJTeH4F9NC7ZHVsGoB+mQeI90NBoW7NnqAeXgPjWUWztzLeNEgUIqkTrjvx+Cskzq2EbOHZlf04+h0+IUtri0IzyzzOBlmif+fvxEpVHLp3FyGDSayc5nEgf42p4PgeXoTt3r2EFbQlyZF+yZLoxfdoZfyt1hXPhNL+1glhdlMqyFUvDnZkL02hw1AgMBAAGjggHhMIIB3TAJBgNVHRMEAjAAMB8GA1UdIwQYMBaAFD+u9XgLkqNwIDVfWvr3JKBSAfBBMB0GA1UdDgQWBBRJ14pzsjAzgkieqWa3PWlpB7qGkTAOBgNVHQ8BAf8EBAMCBLAwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMEMBYGA1UdIAQPMA0wCwYJYIRCARoBAAMCMIG7BgNVHR8EgbMwgbAwN6A1oDOGMWh0dHA6Ly9jcmwudGVzdDQuYnV5cGFzcy5uby9jcmwvQlBDbGFzczNUNENBMy5jcmwwdaBzoHGGb2xkYXA6Ly9sZGFwLnRlc3Q0LmJ1eXBhc3Mubm8vZGM9QnV5cGFzcyxkYz1OTyxDTj1CdXlwYXNzJTIwQ2xhc3MlMjAzJTIwVGVzdDQlMjBDQSUyMDM/Y2VydGlmaWNhdGVSZXZvY2F0aW9uTGlzdDCBigYIKwYBBQUHAQEEfjB8MDsGCCsGAQUFBzABhi9odHRwOi8vb2NzcC50ZXN0NC5idXlwYXNzLm5vL29jc3AvQlBDbGFzczNUNENBMzA9BggrBgEFBQcwAoYxaHR0cDovL2NydC50ZXN0NC5idXlwYXNzLm5vL2NydC9CUENsYXNzM1Q0Q0EzLmNlcjANBgkqhkiG9w0BAQsFAAOCAQEAVaU1bi58EXBE+ljM2SCv9Q0eGAYREenzPmri03N3DhW5exfBFNI8tCFOiudJGqOSMCfbMLycK+TkIcap27vXGSE+Wuudsd9yNNZ5eHBxdlEjQaMiElkyU/rJQLf8xwVFLbbGsksifeqeKTGvZKCOJ7lx9u2ydf9jCcIOGCJUP/GDMdEqlYRLYSJX1HBKCNMevbAcQrwej3w40GPV31FIRPC2urfAM8LMGxxOhk9xjDusP7Bb+SXtiJ0KywSDrv6ypePyTIdXHSr7XttlK01jak9yqKbfWPTTWsC1d/P20aHrr2IYVsEkzb9oOT8nPBoCNpV/L+Bitw/KuSOxJzZtJw==";
    private static final String LEVERANDOER_SERTIFIKAT_UGYLDIG = "MIIE+TCCA+GgAwIBAgILAUhKqNpiy3zXb/YwDQYJKoZIhvcNAQELBQAwUTELMAkGA1UEBhMCTk8xHTAbBgNVBAoMFEJ1eXBhc3MgQVMtOTgzMTYzMzI3MSMwIQYDVQQDDBpCdXlwYXNzIENsYXNzIDMgVGVzdDQgQ0EgMzAeFw0xNzA2MDkxMzQyMTFaFw0yMDA2MTIyMTU5MDBaMF8xCzAJBgNVBAYTAk5PMRIwEAYDVQQKDAlFLUJPS1MgQVMxFDASBgNVBAsMC09wZXJhdGlvbiAxMRIwEAYDVQQDDAlFLUJPS1MgQVMxEjAQBgNVBAUTCTk5NjQ2MDMyMDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJhF7jKFz1H5mfm2NO5jcVOsHGe6etEcBZzTlbKbqzarkcsBW5Xf7/DCcxtzzkUQyiO9yxhAdg6uIpmStlruZcntLtIvZpsWrV60D4gJAUzyHcynxedKoGjUob5qQ6FIlAaqxMZ4kGQvHbzYJ1N9OMWGISENUP4JPYkojFEHwRswORrvmWglj3RU4QbO4Ggg1pvrEddoEafsgnLZOKxz1DSAKwLbv2y0FNhs3akSozbuWC+tmnMNi6y8ufZnrHo099Tl3Uj37EzPY6g1qqqdDEKEhGrrrMzNvf1muGcChWKnDffUuWS4i8MH2tP1raO8dl4vjIkM8Uh9E9Z/hY134OUCAwEAAaOCAcIwggG+MAkGA1UdEwQCMAAwHwYDVR0jBBgwFoAUP671eAuSo3AgNV9a+vckoFIB8EEwHQYDVR0OBBYEFGFGK3fHTWkdBSvGz+SuZVl3rESFMA4GA1UdDwEB/wQEAwIEsDAWBgNVHSAEDzANMAsGCWCEQgEaAQADAjCBuwYDVR0fBIGzMIGwMDegNaAzhjFodHRwOi8vY3JsLnRlc3Q0LmJ1eXBhc3Mubm8vY3JsL0JQQ2xhc3MzVDRDQTMuY3JsMHWgc6Bxhm9sZGFwOi8vbGRhcC50ZXN0NC5idXlwYXNzLm5vL2RjPUJ1eXBhc3MsZGM9Tk8sQ049QnV5cGFzcyUyMENsYXNzJTIwMyUyMFRlc3Q0JTIwQ0ElMjAzP2NlcnRpZmljYXRlUmV2b2NhdGlvbkxpc3QwgYoGCCsGAQUFBwEBBH4wfDA7BggrBgEFBQcwAYYvaHR0cDovL29jc3AudGVzdDQuYnV5cGFzcy5uby9vY3NwL0JQQ2xhc3MzVDRDQTMwPQYIKwYBBQUHMAKGMWh0dHA6Ly9jcnQudGVzdDQuYnV5cGFzcy5uby9jcnQvQlBDbGFzczNUNENBMy5jZXIwDQYJKoZIhvcNAQELBQADggEBAElnmjuY+gPxzLVsjqGAbW0fEZXhbYRaQKS/65+6Gh0STfArBma62CdIvgq5JQMmr47URdkxDhB12SYaMHUyOee9+d3hTpmvplxd4xz/gqSisHJq5wNZfgtf5y4vtxM4X1LNrf93i6plNWpesWWznON8MWRvmm+K+3uSC6trD0o5dv6ax/cuCffgGP4qJ9z59qKFDZrNNEfA1In3ij6V3Gebo7oSJxSlR5enOJTxJOjO2De5k1ObQvPAD9yc2a+eWgkMygsBT3ay5BKCtQcSHfJm3CRNN6bfD3D4kxP3ZvAirvKADcUlS71FAnWqPPoXgxiSJGMPkwjuZJsQeeVd5JA=";
    private static final boolean KAN_VARSLES_TRUE = true;
    private static final boolean KAN_VARSLES_FALSE = false;
    private static final boolean RESERVERT = false;

    private final DigitalKontaktinfoMapper digitalKontaktinfoMapper = new DigitalKontaktinfoMapper();


    @Test
    public void shouldMapOk() {
        DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinfoMapper.mapDigitalKontaktinformasjon(createDigitalKontaktinfo(KAN_VARSLES_TRUE, LEVERANDOER_SERTIFIKAT_GYLDIG));
        assertEquals(BRUKERADRESSE, digitalKontaktinformasjonTo.getBrukerAdresse());
        assertEquals(EPOSTADRESSE, digitalKontaktinformasjonTo.getEpostadresse());
        assertEquals(LEVERANDORADRESSE, digitalKontaktinformasjonTo.getLeverandoerAdresse());
        assertEquals(MOBILTELEFONNUMMER, digitalKontaktinformasjonTo.getMobiltelefonnummer());
        assertTrue(digitalKontaktinformasjonTo.isSertifikat());
    }

    @Test
    public void mapWithoutKanVarsles() {
        DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinfoMapper.mapDigitalKontaktinformasjon(createDigitalKontaktinfo(KAN_VARSLES_FALSE, LEVERANDOER_SERTIFIKAT_GYLDIG));
        assertEquals(BRUKERADRESSE, digitalKontaktinformasjonTo.getBrukerAdresse());
        assertEquals(LEVERANDORADRESSE, digitalKontaktinformasjonTo.getLeverandoerAdresse());
        assertNull(digitalKontaktinformasjonTo.getEpostadresse());
        assertNull(digitalKontaktinformasjonTo.getMobiltelefonnummer());
    }

    @Test
    public void mapWithoutGyldigSertifikat() {
        DigitalKontaktinformasjonTo digitalKontaktinformasjonTo = digitalKontaktinfoMapper.mapDigitalKontaktinformasjon(createDigitalKontaktinfo(KAN_VARSLES_TRUE, LEVERANDOER_SERTIFIKAT_UGYLDIG));
        assertEquals(BRUKERADRESSE, digitalKontaktinformasjonTo.getBrukerAdresse());
        assertEquals(EPOSTADRESSE, digitalKontaktinformasjonTo.getEpostadresse());
        assertEquals(LEVERANDORADRESSE, digitalKontaktinformasjonTo.getLeverandoerAdresse());
        assertEquals(MOBILTELEFONNUMMER, digitalKontaktinformasjonTo.getMobiltelefonnummer());
        assertFalse(digitalKontaktinformasjonTo.isSertifikat());
    }

    private DkifResponseTo.DigitalKontaktinfo createDigitalKontaktinfo(boolean kanVarsles, String leverandoerSertifikat) {
        return DkifResponseTo.DigitalKontaktinfo.builder()
                .epostadresse(EPOSTADRESSE)
                .mobiltelefonnummer(MOBILTELEFONNUMMER)
                .kanVarsles(kanVarsles)
                .reservert(RESERVERT)
                .sikkerDigitalPostkasse(DkifResponseTo.SikkerDigitalPostkasse.builder()
                        .adresse(BRUKERADRESSE)
                        .leverandoerAdresse(LEVERANDORADRESSE)
                        .leverandoerSertifikat(leverandoerSertifikat)
                        .build())
                .build();
    }
}
