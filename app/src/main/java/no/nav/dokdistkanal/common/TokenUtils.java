package no.nav.dokdistkanal.common;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class TokenUtils {

	public static final String BEARER_TOKEN_PREFIX = "Bearer ";

	public static String getAccessTokenFromRequest(HttpServletRequest request) {
		return Optional.ofNullable(request.getHeader(AUTHORIZATION))
				.filter(e -> e.startsWith(BEARER_TOKEN_PREFIX))
				.map(e -> e.replaceFirst(BEARER_TOKEN_PREFIX, ""))
				.orElse(null);
	}

}
