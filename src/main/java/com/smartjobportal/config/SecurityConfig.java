package com.smartjobportal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.smartjobportal.security.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

	@Autowired
	private JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http

				.csrf(csrf -> csrf.disable())

				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				.exceptionHandling(exception -> exception
						.authenticationEntryPoint((request, response, authenticationException) -> {
							if (request.getRequestURI().startsWith(request.getContextPath() + "/api/career-agent/")) {
								response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
								response.setContentType("application/json");
								response.setCharacterEncoding("UTF-8");
								response.getWriter().write(
										"{\"message\":\"Authentication is required to use the Career Agent.\"}");
								return;
							}
							response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
						})
						.accessDeniedHandler((request, response, accessDeniedException) -> {
							Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
							if (request.getRequestURI().startsWith(request.getContextPath() + "/api/career-agent/")
									&& (authentication == null || !authentication.isAuthenticated()
											|| authentication instanceof AnonymousAuthenticationToken)) {
								response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
								response.setContentType("application/json");
								response.setCharacterEncoding("UTF-8");
								response.getWriter().write(
										"{\"message\":\"Authentication is required to use the Career Agent.\"}");
								return;
							}
							if (request.getRequestURI().startsWith(request.getContextPath() + "/api/resume-parser/")) {
								response.setStatus(HttpServletResponse.SC_FORBIDDEN);
								response.setContentType("application/json");
								response.setCharacterEncoding("UTF-8");
								response.getWriter().write(
										"{\"message\":\"Resume parsing is restricted to the signed-in candidate who owns this resume.\"}");
								return;
							}

							response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
						}))

				.authorizeHttpRequests(auth -> auth

						// ==========================================
						// PUBLIC ENDPOINTS
						// ==========================================

						.requestMatchers("/", "/favicon.ico", "/html/**", "/css/**", "/js/**", "/images/**").permitAll()

						.requestMatchers("/api/auth/**").permitAll()

						// ==========================================
						// CANDIDATE MODULE
						// ==========================================

						.requestMatchers("/api/candidate/**").hasAuthority("ROLE_CANDIDATE")

						.requestMatchers("/api/resumes/**").hasAuthority("ROLE_CANDIDATE")

						.requestMatchers("/api/resume-parser/**").hasAuthority("ROLE_CANDIDATE")

						.requestMatchers("/api/resume-upload/**").hasAnyAuthority("ROLE_CANDIDATE", "ROLE_RECRUITER")

						.requestMatchers("/api/career-agent/**").hasAuthority("ROLE_CANDIDATE")
						// ==========================================
						// RECRUITER MODULE
						// ==========================================

						.requestMatchers("/api/recruiters/**").hasAuthority("ROLE_RECRUITER")

						.requestMatchers(HttpMethod.GET, "/api/opportunities/search", "/api/opportunities/details/**")
						.hasAnyAuthority("ROLE_CANDIDATE", "ROLE_RECRUITER")

						.requestMatchers(HttpMethod.POST, "/api/opportunities/search")
						.hasAnyAuthority("ROLE_CANDIDATE", "ROLE_RECRUITER")

						.requestMatchers(HttpMethod.GET, "/api/opportunities/my").hasAuthority("ROLE_RECRUITER")

						.requestMatchers(HttpMethod.GET, "/api/opportunities/my-applications",
								"/api/opportunities/*/my-application")
						.hasAuthority("ROLE_CANDIDATE")

						.requestMatchers(HttpMethod.POST, "/api/opportunities/*/apply")
						.hasAuthority("ROLE_CANDIDATE")

						.requestMatchers(HttpMethod.POST, "/api/opportunities/internships",
								"/api/opportunities/hackathons")
						.hasAuthority("ROLE_RECRUITER")

						.requestMatchers("/api/opportunities/**").authenticated()

						.requestMatchers(HttpMethod.GET, "/api/companies/filter")
						.hasAnyAuthority("ROLE_CANDIDATE", "ROLE_RECRUITER")

						.requestMatchers("/api/companies/**").hasAuthority("ROLE_RECRUITER")

						// ==========================================
						// JOB MODULE
						// ==========================================

						.requestMatchers(HttpMethod.GET, "/api/jobs/my-jobs").hasAuthority("ROLE_RECRUITER")

						.requestMatchers(HttpMethod.POST, "/api/jobs").hasAuthority("ROLE_RECRUITER")

						.requestMatchers(HttpMethod.PUT, "/api/jobs/**").hasAuthority("ROLE_RECRUITER")

						.requestMatchers(HttpMethod.DELETE, "/api/jobs/**").hasAuthority("ROLE_RECRUITER")

						.requestMatchers("/api/jobs/**").authenticated()

						// ==========================================
						// APPLICATION MODULE
						// ==========================================

						.requestMatchers(HttpMethod.POST, "/api/applications/apply").hasAuthority("ROLE_CANDIDATE")

						.requestMatchers(HttpMethod.GET, "/api/applications/my-applications")
						.hasAuthority("ROLE_CANDIDATE")

						.requestMatchers(HttpMethod.GET, "/api/applications/job/**").hasAuthority("ROLE_RECRUITER")

						.requestMatchers(HttpMethod.PUT, "/api/applications/*/status").hasAuthority("ROLE_RECRUITER")

						.requestMatchers(HttpMethod.GET, "/api/applications/*/history")
						.hasAnyAuthority("ROLE_CANDIDATE", "ROLE_RECRUITER")

						// ==========================================
						// AI MATCHING
						// ==========================================

						.requestMatchers("/api/ai-match/job/*/candidates").hasAuthority("ROLE_RECRUITER")

						.requestMatchers("/api/ai-match/**").hasAnyAuthority("ROLE_CANDIDATE", "ROLE_RECRUITER")

						// ==========================================
						// ADMIN MODULE
						// ==========================================

						.requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

						// ==========================================
						// FALLBACK
						// ==========================================
						.requestMatchers("/api/users/**")
						.hasAnyAuthority("ROLE_CANDIDATE", "ROLE_RECRUITER", "ROLE_ADMIN").anyRequest().authenticated())

				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {

		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {

		return configuration.getAuthenticationManager();
	}
}