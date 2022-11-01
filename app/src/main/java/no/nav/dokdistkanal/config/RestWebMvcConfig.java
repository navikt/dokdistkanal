package no.nav.dokdistkanal.config;

import no.nav.dokdistkanal.service.PopulateMDCHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RestWebMvcConfig implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new PopulateMDCHandler()).addPathPatterns("/rest/**");
	}
}
