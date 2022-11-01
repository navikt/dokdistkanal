package no.nav.dokdistkanal.itest;

import no.nav.dokdistkanal.Application;
import no.nav.dokdistkanal.azure.TokenConsumer;
import no.nav.dokdistkanal.azure.TokenResponse;
import no.nav.dokdistkanal.constants.MDCConstants;
import no.nav.dokdistkanal.service.PopulateMDCHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.client.RestTemplate;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */

@SpringBootTest(
        classes = {Application.class, AbstractIT.Config.class},
        webEnvironment = RANDOM_PORT
)
@ContextConfiguration(classes = {PopulateMDCHandler.class})
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("itest")
@ImportAutoConfiguration
public abstract class AbstractIT {

    private final static String CONSUMER_ID = "srvdokdistfordeling";
    @Value("${local.server.port}")
    protected String LOCALPORT;

    protected String LOCAL_ENDPOINT_URL;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    protected RestTemplate restTemplate;


    @BeforeEach
    public void setUp() {
        LOCAL_ENDPOINT_URL = "http://localhost:" + LOCALPORT;
        clearCachene();
    }

    static class Config {
        @Bean
        @Primary
        TokenConsumer azureTokenConsumer() {
            return (String) -> TokenResponse.builder()
                    .access_token("dummy")
                    .build();
        }
    }

    private void clearCachene() {
        cacheManager.getCacheNames().forEach(names -> cacheManager.getCache(names).clear());
    }

}
