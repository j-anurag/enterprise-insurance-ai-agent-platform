package com.vinay.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
public class AppConfig {
	
	public static final String[] PUBLIC_API= {
			"/v3/api-docs/**",
			"/api/v1/auth/**",
			"/v2/api-docs/**",
			"/swagger-resources/**",
			"/swagger-ui/**",
			"/swagger-ui.html",
			"/webjars/**"
	};
	
	@Bean
	public SecurityFilterChain securityConfigrationChain(HttpSecurity http) throws Exception {
		http
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/swagger-ui*/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/clients/", "/login", "/register").permitAll()
				.requestMatchers("/welcome").permitAll()
				.anyRequest().authenticated()
			)
			.addFilterAfter(new JwtGenraterTokanFilter(), BasicAuthenticationFilter.class)
			.addFilterBefore(new JwtTokanValideterFilter(), BasicAuthenticationFilter.class)
			.formLogin(Customizer.withDefaults())
			.httpBasic(Customizer.withDefaults());
		
		return http.build();
	}
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public ModelMapper mapper() {
		return new ModelMapper();
	}

}
