package no.nav.dokdistkanal.itest;

import no.nav.dokdistkanal.itest.config.ApplicationTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static no.nav.dokdistkanal.constants.DomainConstants.HAL_JSON_VALUE;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.ACCEPT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(
		classes = {ApplicationTestConfig.class},
		webEnvironment = RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
@EnableAutoConfiguration
@AutoConfigureWebClient
//Er noe krøll med stubs og concurrency tror jeg, "permidlertidig" fix. Mistenker at Wiremock holder connections til
//HttpClient åpen litt for lenge, men usikker på om det finnes en god løsning.
//Kan også løses med Thread.sleep(50) før eller etter hver test.
@DirtiesContext
public abstract class AbstractIT extends AbstractOauth2Test {

	protected static final String DOKMET_URL = "/DOKUMENTTYPEINFO_V4(.*)";
	private static final String DIGDIR_KRR_PROXY_URL = "/DIGDIR_KRR_PROXY/rest/v1/personer?inkluderSikkerDigitalPost=true";
	private static final String MASKINPORTEN_URL = "/maskinporten";
	private static final String AZURE_TOKEN_URL = "/azure_token";
	private static final String ALTINN_URL = "/altinn/serviceowner/notifications/validaterecipient";
	private static final String ALTINN_URL_FOR_ORGANISASJON_UTEN_VARSLINGSINFORMASJON = "/altinn/serviceowner/notifications/validaterecipient?organizationNumber=889640782";
	private static final String PDL_GRAPHQL_URL = "/graphql";

	protected static final String DOKMET_HAPPY_FILE_PATH = "dokmet/happy-response.json";
	private static final String PDL_HAPPY_FILE_PATH = "pdl/pdl_ok_response.json";
	private static final String DIGDIR_KRR_PROXY_HAPPY_FILE_PATH = "dki/happy-responsebody.json";
	private static final String MASKINPORTEN_HAPPY_FILE_PATH = "altinn/maskinporten_happy_response.json";
	private static final String AZURE_TOKEN_HAPPY_FILE_PATH = "azure/token_response_dummy.json";

	public static final String BESTEM_DISTRIBUSJONSKANAL_URL = "/rest/bestemDistribusjonskanal";
	public static final String HENT_ENHET_OK_PATH = "enhetsregisteret/ikke_konkurs_enhetsregisteret.json";
	public static final String GRUPPEROLLER_OK_PATH = "enhetsregisteret/enhets_grupperoller.json";
	public static final String GRUPPEROLLER_PERSON_ER_DOED_PATH = "enhetsregisteret/grupperoller_person_er_doed.json";
	public static final String KONKURS_ENHET_PATH = "enhetsregisteret/konkurs_enhet.json";
	public static final String SLETTET_ENHET_PATH = "enhetsregisteret/slettet_enhet.json";
	public static final String UNDERENHET_ORGNR = "916007922";
	public static final String HOVEDENHET_ORGNR = "974761076";


	@Value("${local.url}")
	protected String LOCAL_ENDPOINT_URL;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	protected RestTemplate restTemplate;

	@Autowired
	public WebTestClient webTestClient;

	@BeforeEach
	public void setUp() {
		clearCachene();
	}

	private void clearCachene() {
		cacheManager.getCacheNames().forEach(names -> cacheManager.getCache(names).clear());
	}

	protected void stubDigdirKrrProxy() {
		stubDigdirKrrProxy(DIGDIR_KRR_PROXY_HAPPY_FILE_PATH);
	}

	protected void stubPdl() {
		stubPdl(PDL_HAPPY_FILE_PATH);
	}

	protected void stubMaskinporten() {
		stubFor(post(urlMatching(MASKINPORTEN_URL))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(MASKINPORTEN_HAPPY_FILE_PATH)));
	}

	protected void stubAltinn() {
		var RESPONS_MED_VARSLINGSINFORMASJON = "altinn/serviceowner_happy_response.json";
		var RESPONS_UTEN_VARSLINGSINFORMASJON = "altinn/serviceowner_with_false_response.json";

		var response = aResponse()
				.withHeader(CONTENT_TYPE, HAL_JSON_VALUE)
				.withHeader(ACCEPT_ENCODING, "gzip");

		stubFor(get(urlPathEqualTo(ALTINN_URL))
				.willReturn(response
						.withBodyFile(RESPONS_MED_VARSLINGSINFORMASJON)));

		stubFor(get(urlEqualTo(ALTINN_URL_FOR_ORGANISASJON_UTEN_VARSLINGSINFORMASJON))
				.willReturn(response
						.withBodyFile(RESPONS_UTEN_VARSLINGSINFORMASJON)));
	}

	protected void stubEnhetsregisteret(HttpStatus status, String path, String organisasjonsnummer) {
		stubFor(get(urlEqualTo("/enhetsregisteret/enheter/" + organisasjonsnummer))
				.willReturn(aResponse()
						.withStatus(status.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(path)));
	}

	protected void stubSecondEnhetsregisteret(String path, String organisasjonsnummer) {
		stubFor(get(urlEqualTo("/enhetsregisteret/enheter/" + organisasjonsnummer))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(path)));
	}

	protected void stubUnderenhetsregisteret(HttpStatus status, String path, String organisasjonsnummer) {
		stubFor(get(urlEqualTo("/enhetsregisteret/underenheter/" + organisasjonsnummer))
				.willReturn(aResponse()
						.withStatus(status.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(path)));
	}

	protected void stubEnhetsGruppeRoller(String path, String organisasjonsnummer) {
		stubFor(any(urlMatching("/enhetsregisteret/enheter/" + organisasjonsnummer + "/roller"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(path))
		);
	}

	protected void stubAzure() {
		stubFor(post(AZURE_TOKEN_URL)
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(AZURE_TOKEN_HAPPY_FILE_PATH)));
	}

	protected void stubDokmet() {
		stubDokmet(DOKMET_HAPPY_FILE_PATH);
	}

	protected void stubPdl(String bodyFilePath) {
		stubFor(post(urlMatching(PDL_GRAPHQL_URL))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(bodyFilePath)));
	}

	protected void stubDigdirKrrProxy(String bodyFilePath) {
		//leverandoerSertifikat som ligger under mappene dokmet/... er utsendt av DigDir og har utløpsdato februar 2023.
		//Det må byttes ut innen den tid hvis ikke vil testene feile. Mer info i README.
		stubFor(post(DIGDIR_KRR_PROXY_URL)
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(bodyFilePath)));
	}

	protected void stubDokmet(String bodyFilePath) {
		stubFor(get(urlPathMatching(DOKMET_URL))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile(bodyFilePath)));
	}

	protected void stubDokmet(HttpStatus httpStatus) {
		stubFor(get(urlPathMatching(DOKMET_URL))
				.willReturn(aResponse()
						.withStatus(httpStatus.value())));
	}

}
