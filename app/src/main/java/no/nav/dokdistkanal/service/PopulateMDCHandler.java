package no.nav.dokdistkanal.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.TokenUtils;
import no.nav.dokdistkanal.exceptions.functional.CouldNotDecodeBasicAuthToken;
import no.nav.security.token.support.core.jwt.JwtToken;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static no.nav.dokdistkanal.constants.MDCConstants.CONSUMER_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.USER_ID;
import static no.nav.dokdistkanal.constants.NavHeaders.NAV_CONSUMER_ID;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.MDC.put;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
public class PopulateMDCHandler implements HandlerInterceptor {

	private static final String CHARSET = UTF_8.name();
	private static final String NAV_CUSTOM_CLAIM_AZP_NAME = "azp_name";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		populateConsumerId(request);
		populateUserId(request);
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		MDC.clear();
	}


	private void populateConsumerId(HttpServletRequest request) {
		Optional<String> consumerIdFromToken = getConsumerIdFromToken(request);

		if (consumerIdFromToken.isPresent()) {
			put(CONSUMER_ID, consumerIdFromToken.get());
		} else {
			String navConsumerId = isNotBlank(request.getHeader(NAV_CONSUMER_ID)) ? request.getHeader(NAV_CONSUMER_ID) : request.getHeader(CONSUMER_ID);
			String username = getUsernameFromBasicAuth(request);
			if (isNotBlank(navConsumerId)) {
				put(CONSUMER_ID, navConsumerId);
			}

			if (isNotBlank(username)) {
				put(CONSUMER_ID, username);
			}
		}
	}

	private void populateUserId(HttpServletRequest request) {
		final String navUserId = request.getHeader(USER_ID);
		if (isNotBlank(navUserId)) {
			put(USER_ID, navUserId);
		}
	}

	private Optional<String> getConsumerIdFromToken(HttpServletRequest request) {
		return TokenUtils.getAccessTokenFromRequest(request)
				.map(it -> {
					try {
						var claims = new JwtToken(it).getJwtTokenClaims();
						return extractConsumerId(claims.getStringClaim(NAV_CUSTOM_CLAIM_AZP_NAME));
					} catch (RuntimeException e) {
						//Ugyldig JWT håndteres av Token Support.
						log.error("Kunne ikke hente consumerId fra token", e);
					}
					return null;
				});
	}

	private String extractConsumerId(String claim) {
		var split = claim.split(":");
		if (split.length == 3) {
			return split[1] + ":" + split[2];
		}
		return claim;
	}

	private String getUsernameFromBasicAuth(HttpServletRequest request) {
		String authorizationHeader = request.getHeader(AUTHORIZATION);
		if (isNotBlank(authorizationHeader) && authorizationHeader.startsWith("Basic")) {
			try {
				String[] strings = decodeBasicAuth(authorizationHeader);
				return strings[0];
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	public static String[] decodeBasicAuth(String header) {
		byte[] decoded;
		try {
			byte[] base64Token = header.substring(6).getBytes(CHARSET);
			decoded = Base64.getDecoder().decode(base64Token);
			String token = new String(decoded, CHARSET);
			int delim = token.indexOf(':');
			if (delim == -1) {
				throw new CouldNotDecodeBasicAuthToken("Decode av basicAuthToken feilet");
			}
			return new String[]{token.substring(0, delim), token.substring(delim + 1)};
		} catch (IllegalArgumentException | UnsupportedEncodingException e) {
			throw new CouldNotDecodeBasicAuthToken("Decode av basicAuthToken feilet");
		}
	}
}
