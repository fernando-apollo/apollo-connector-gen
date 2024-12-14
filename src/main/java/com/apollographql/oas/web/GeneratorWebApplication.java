package com.apollographql.oas.web;

import com.apollographql.oas.web.storage.StorageProperties;
import com.apollographql.oas.web.storage.StorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class GeneratorWebApplication {

	@Configuration
	public class CorsConfig {
		@Bean
		public CorsFilter corsFilter() {
			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
			CorsConfiguration config = new CorsConfiguration();
			config.setAllowCredentials(true);
			config.addAllowedOriginPattern("*"); // Allow all origins
			config.addAllowedHeader("*"); // Allow all headers
			config.addAllowedMethod("*"); // Allow all methods (GET, POST, etc.)
			source.registerCorsConfiguration("/**", config);
			return new CorsFilter(source);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(GeneratorWebApplication.class, args);
	}

	@Bean
	CommandLineRunner initStorageService(StorageService storageService) {
		return (args) -> {
			storageService.deleteAll();
			storageService.init();
		};
	}
	@Bean
	CommandLineRunner initGeneratorService(GeneratorService generatorService) {
		return (args) -> {
			generatorService.init();
		};
	}
}
