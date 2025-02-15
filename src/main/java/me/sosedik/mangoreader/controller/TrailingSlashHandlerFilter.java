package me.sosedik.mangoreader.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.UrlHandlerFilter;

@Configuration
public class TrailingSlashHandlerFilter {

	@Bean
	public UrlHandlerFilter urlHandlerFilter() {
		return UrlHandlerFilter
				.trailingSlashHandler("/**").redirect(HttpStatus.PERMANENT_REDIRECT)
				.build();
	}

}
