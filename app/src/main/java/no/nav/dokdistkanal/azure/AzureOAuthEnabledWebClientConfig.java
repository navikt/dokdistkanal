package no.nav.dokdistkanal.azure;

import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;
import static no.nav.dokdistkanal.azure.AzureProperties.CLIENT_REGISTRATION_DIGDIR_KRR_PROXY;
import static no.nav.dokdistkanal.azure.AzureProperties.CLIENT_REGISTRATION_PDL;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;
import static org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC;

@Configuration
public class AzureOAuthEnabledWebClientConfig {

	@Bean
	WebClient webClient(JsonMapper jsonMapper) {
		HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(20))
				.proxyWithSystemProperties();
		return WebClient.builder()
				.exchangeStrategies(jacksonStrategies(jsonMapper))
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}

	@Bean
	WebClient azureOauth2WebClient(JsonMapper jsonMapper, ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
		ServerOAuth2AuthorizedClientExchangeFilterFunction oAuth2AuthorizedClientExchangeFilterFunction = new ServerOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);

		var nettyHttpClient = HttpClient.create()
				.responseTimeout(Duration.of(20, SECONDS));
		var clientHttpConnector = new ReactorClientHttpConnector(nettyHttpClient);

		return WebClient.builder()
				.exchangeStrategies(jacksonStrategies(jsonMapper))
				.clientConnector(clientHttpConnector)
				.filter(oAuth2AuthorizedClientExchangeFilterFunction)
				.build();
	}

	private ExchangeStrategies jacksonStrategies(JsonMapper jsonMapper) {
		return ExchangeStrategies.builder()
				.codecs(configurer -> {
					configurer.defaultCodecs().jacksonJsonDecoder(new JacksonJsonDecoder(jsonMapper));
					configurer.defaultCodecs().jacksonJsonEncoder(new JacksonJsonEncoder(jsonMapper));
				})
				.build();
	}

	@Bean
	ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager(
			ReactiveClientRegistrationRepository clientRegistrationRepository,
			ReactiveOAuth2AuthorizedClientService oAuth2AuthorizedClientService
	) {
		ClientCredentialsReactiveOAuth2AuthorizedClientProvider authorizedClientProvider = new ClientCredentialsReactiveOAuth2AuthorizedClientProvider();
		var nettyHttpClient = HttpClient.create()
				.proxyWithSystemProperties()
				.responseTimeout(Duration.of(20, SECONDS));
		var clientHttpConnector = new ReactorClientHttpConnector(nettyHttpClient);

		WebClient webClientWithProxy = WebClient.builder()
				.clientConnector(clientHttpConnector)
				.build();

		var client = new WebClientReactiveClientCredentialsTokenResponseClient();
		client.setWebClient(webClientWithProxy);

		authorizedClientProvider.setAccessTokenResponseClient(client);

		var authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
		return authorizedClientManager;
	}

	@Bean
	ReactiveOAuth2AuthorizedClientService oAuth2AuthorizedClientService(ReactiveClientRegistrationRepository clientRegistrationRepository) {
		return new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
	}

	@Bean
	ReactiveClientRegistrationRepository clientRegistrationRepository(List<ClientRegistration> clientRegistration) {
		return new InMemoryReactiveClientRegistrationRepository(clientRegistration);
	}

	@Bean
	List<ClientRegistration> clientRegistration(AzureProperties azureProperties, DokdistkanalProperties dokdistkanalProperties) {
		return List.of(
				ClientRegistration.withRegistrationId(CLIENT_REGISTRATION_DIGDIR_KRR_PROXY)
						.tokenUri(azureProperties.openidConfigTokenEndpoint())
						.clientId(azureProperties.appClientId())
						.clientSecret(azureProperties.appClientSecret())
						.clientAuthenticationMethod(CLIENT_SECRET_BASIC)
						.authorizationGrantType(CLIENT_CREDENTIALS)
						.scope(dokdistkanalProperties.getEndpoints().getDigdirKrrProxy().getScope())
						.build(),
				ClientRegistration.withRegistrationId(CLIENT_REGISTRATION_PDL)
						.tokenUri(azureProperties.openidConfigTokenEndpoint())
						.clientId(azureProperties.appClientId())
						.clientSecret(azureProperties.appClientSecret())
						.clientAuthenticationMethod(CLIENT_SECRET_BASIC)
						.authorizationGrantType(CLIENT_CREDENTIALS)
						.scope(dokdistkanalProperties.getEndpoints().getPdl().getScope())
						.build()
		);
	}
}
