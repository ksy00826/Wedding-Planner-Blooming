package com.ssafy.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	@Override
	public void addCorsMappings(final CorsRegistry registry) {
		registry.addMapping("/**")
			.allowedOrigins("https://i9e104.p.ssafy.io","http://43.200.254.50", "http://192.168.30.207:5173", "http://127.0.0.1:5173",
				"http://localhost:5173") // 프론트 ip로 변경하기
			.allowedMethods("GET", "POST", "PUT", "DELETE", "FETCH", "HEAD", "OPTIONS")
			.allowedHeaders("Authorization", "Authorization_refresh")
			.exposedHeaders("Authorization", "Authorization_refresh")
			.allowCredentials(true)
			.maxAge(3600);
	}
}
