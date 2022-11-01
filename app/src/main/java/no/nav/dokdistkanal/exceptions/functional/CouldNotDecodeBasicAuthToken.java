package no.nav.dokdistkanal.exceptions.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ResponseStatus(value = UNAUTHORIZED)
public class CouldNotDecodeBasicAuthToken extends DokDistKanalFunctionalException{
	public CouldNotDecodeBasicAuthToken(String message){
		super(message);
	}
}
