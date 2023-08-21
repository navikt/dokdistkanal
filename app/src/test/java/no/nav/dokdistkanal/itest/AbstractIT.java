package no.nav.dokdistkanal.itest;

import no.nav.dokdistkanal.Application;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(
		classes = {Application.class},
		webEnvironment = RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
@EnableAutoConfiguration
public abstract class AbstractIT {

	private final static String CONSUMER_ID = "srvdokdistfordeling";
	@Value("${local.url}")
	protected String LOCAL_ENDPOINT_URL;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	protected RestTemplate restTemplate;

	@BeforeEach
	public void setUp() {
		clearCachene();
	}

	private void clearCachene() {
		cacheManager.getCacheNames().forEach(names -> cacheManager.getCache(names).clear());
	}

}
