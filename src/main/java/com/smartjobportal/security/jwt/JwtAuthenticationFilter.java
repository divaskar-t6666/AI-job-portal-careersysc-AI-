package com.smartjobportal.security.jwt;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
	private static final String CAREER_AGENT_PATH = "/api/career-agent/";

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

		String path = request.getServletPath();

		return path.equals("/api/auth/register") || path.equals("/api/auth/login");
	}

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private UserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String requestPath = getRequestPath(request);
		boolean careerAgentRequest = requestPath.startsWith(CAREER_AGENT_PATH);
		String requestId = careerAgentRequest ? UUID.randomUUID().toString() : null;
		if (careerAgentRequest) {
			response.setHeader("X-Request-ID", requestId);
		}

		try {
			final String authorizationHeader = request.getHeader("Authorization");

			String jwtToken = null;

			String username = null;

			if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {

				jwtToken = authorizationHeader.substring(7);

				try {

					username = jwtUtil.extractUsername(jwtToken);

				} catch (IllegalArgumentException e) {

					logger.warn("Unable to read JWT token for request path {}", request.getServletPath());
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication token is invalid or expired");
					return;

				} catch (ExpiredJwtException e) {

					logger.warn("JWT token expired for request path {}", request.getServletPath());
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication token is invalid or expired");
					return;

				} catch (MalformedJwtException e) {

					logger.warn("Invalid JWT token for request path {}", request.getServletPath());
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication token is invalid or expired");
					return;

				} catch (UnsupportedJwtException e) {

					logger.warn("Unsupported JWT token for request path {}", request.getServletPath());
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication token is invalid or expired");
					return;

				} catch (SignatureException e) {

					logger.warn("JWT signature validation failed for request path {}", request.getServletPath());
					response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication token is invalid or expired");
					return;
				}
			}

			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

				UserDetails userDetails = userDetailsService.loadUserByUsername(username);

				if (jwtUtil.validateToken(jwtToken, userDetails)) {

					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
							null, userDetails.getAuthorities());

					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

					SecurityContextHolder.getContext().setAuthentication(authToken);

				}
			}

			filterChain.doFilter(request, response);
		} finally {
			if (careerAgentRequest) {
				logCareerAgentAuthentication(requestId, response);
			}
		}
	}

	private String getRequestPath(HttpServletRequest request) {
		String path = request.getServletPath();
		if (path == null || path.isBlank()) {
			path = request.getRequestURI();
			String contextPath = request.getContextPath();
			if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
				path = path.substring(contextPath.length());
			}
		}
		return path;
	}

	private void logCareerAgentAuthentication(String requestId, HttpServletResponse response) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean authenticated = authentication != null && authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
		String principalType = authentication == null || authentication.getPrincipal() == null
				? "none" : authentication.getPrincipal().getClass().getSimpleName();
		boolean candidateAuthority = authenticated && authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_CANDIDATE".equals(authority.getAuthority()));
		String roleCheck = !authenticated ? "not-authenticated" : candidateAuthority ? "allowed" : "denied";

		logger.info(
				"Career Agent auth requestId={} authenticated={} principalType={} candidateRoleCheck={} responseStatus={}",
				requestId, authenticated, principalType, roleCheck, response.getStatus());
	}
}