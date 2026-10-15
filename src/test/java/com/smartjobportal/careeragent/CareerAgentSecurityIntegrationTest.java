package com.smartjobportal.careeragent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import com.smartjobportal.careerai.GeminiApiClient;
import com.smartjobportal.security.service.CustomUserDetailsService;
import com.smartjobportal.security.jwt.JwtUtil;
import com.smartjobportal.security.userdetails.UserPrincipal;
import com.smartjobportal.entity.User;
import com.smartjobportal.job.dto.RecruiterCandidateMatchDto;
import com.smartjobportal.job.service.JobMatchingService;
import com.smartjobportal.repository.CandidateProfileRepository;
import com.smartjobportal.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@SpringBootTest
@AutoConfigureMockMvc
class CareerAgentSecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtUtil jwtUtil;

	@Value("${jwt.secret}")
	private String jwtSecret;

	@MockBean
	private CustomUserDetailsService userDetailsService;

	@MockBean
	private GeminiApiClient geminiApiClient;

	@MockBean
	private UserRepository userRepository;

	@MockBean
	private CandidateProfileRepository candidateProfileRepository;

	@MockBean
	private JobMatchingService jobMatchingService;

	@Test
	void unauthenticatedCareerAgentRequestReturnsUnauthorizedRatherThanForbidden() throws Exception {
		mockMvc.perform(get("/api/career-agent/status"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void invalidJwtReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/career-agent/status")
				.header("Authorization", "Bearer invalid-token"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void expiredJwtReturnsUnauthorized() throws Exception {
		long now = System.currentTimeMillis();
		String expiredToken = Jwts.builder()
				.setSubject("candidate@example.test")
				.setIssuedAt(new Date(now - 2_000))
				.setExpiration(new Date(now - 1_000))
				.signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret)), SignatureAlgorithm.HS256)
				.compact();

		mockMvc.perform(get("/api/career-agent/status")
				.header("Authorization", "Bearer " + expiredToken))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(authorities = "ROLE_RECRUITER")
	void authenticatedRecruiterStillCannotAccessCandidateCareerAgent() throws Exception {
		mockMvc.perform(get("/api/career-agent/status"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(authorities = "ROLE_CANDIDATE")
	void candidateCannotAccessRecruiterCandidateMatching() throws Exception {
		mockMvc.perform(get("/api/ai-match/job/31/candidates"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(authorities = "ROLE_RECRUITER")
	void recruiterMatchingEndpointReturnsRankedCandidateResponse() throws Exception {
		RecruiterCandidateMatchDto candidate = new RecruiterCandidateMatchDto();
		candidate.setCandidateName("Test Candidate");
		candidate.setCurrentJobTitle("Developer");
		candidate.setHasActiveResume(true);
		candidate.setMatchPercentage(75.0);
		candidate.setMatchedSkills(java.util.List.of("java"));
		candidate.setMissingSkills(java.util.List.of("spring"));
		when(jobMatchingService.getRecruiterCandidateMatches(31))
				.thenReturn(java.util.List.of(candidate));

		mockMvc.perform(get("/api/ai-match/job/31/candidates"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].candidateName").value("Test Candidate"))
				.andExpect(jsonPath("$[0].matchPercentage").value(75.0))
				.andExpect(jsonPath("$[0].matchedSkills[0]").value("java"))
				.andExpect(jsonPath("$[0].missingSkills[0]").value("spring"));
	}

	@Test
	void expiredJwtCannotAccessRecruiterCandidateMatching() throws Exception {
		long now = System.currentTimeMillis();
		String expiredToken = Jwts.builder()
				.setSubject("recruiter@example.test")
				.setIssuedAt(new Date(now - 2_000))
				.setExpiration(new Date(now - 1_000))
				.signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret)), SignatureAlgorithm.HS256)
				.compact();

		mockMvc.perform(get("/api/ai-match/job/31/candidates")
				.header("Authorization", "Bearer " + expiredToken))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void recruiterDashboardScriptUsesMatchingEndpointAndBearerHeader() throws Exception {
		mockMvc.perform(get("/js/recruiter-dashboard.js"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(
						"`/api/ai-match/job/${jobId}/candidates`")))
				.andExpect(content().string(containsString("\"Bearer \" + token")));
	}

	@Test
	void recruiterJwtCannotAccessCareerAgentChat() throws Exception {
		UserDetails recruiter = new UserPrincipal(
				"recruiter@example.test", "not-used", "ROLE_RECRUITER");
		when(userDetailsService.loadUserByUsername("recruiter@example.test")).thenReturn(recruiter);
		String token = jwtUtil.generateToken(recruiter);

		mockMvc.perform(post("/api/career-agent/chat")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"hello\",\"history\":[]}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(authorities = "ROLE_CANDIDATE")
	void authenticatedCandidateCanAccessCareerAgentStatus() throws Exception {
		mockMvc.perform(get("/api/career-agent/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.providerName").value("Career AI Assistant"));
	}

	@Test
	void legacyUnprefixedCandidateRoleResolvesToCandidateAuthority() {
		UserPrincipal principal = new UserPrincipal("candidate@example.test", "not-used", "candidate");

		org.junit.jupiter.api.Assertions.assertEquals(
				"ROLE_CANDIDATE", principal.getAuthorities().iterator().next().getAuthority());
	}

	@Test
	void candidateCanSendConsecutiveChatRequestsWithoutLosingAuthenticationOrContext() throws Exception {
		UserDetails candidate = new UserPrincipal(
				"candidate@example.test", "not-used", "ROLE_CANDIDATE");
		when(userDetailsService.loadUserByUsername("candidate@example.test")).thenReturn(candidate);
		when(geminiApiClient.isConfigured()).thenReturn(true);
		when(geminiApiClient.generate(any(), anyList(), any()))
				.thenAnswer(invocation -> "Gemini: " + invocation.getArgument(2, String.class));
		User candidateUser = new User();
		candidateUser.setUserId(7);
		candidateUser.setEmail("candidate@example.test");
		when(userRepository.findByEmail("candidate@example.test")).thenReturn(Optional.of(candidateUser));
		when(candidateProfileRepository.findByUserUserId(7)).thenReturn(Optional.empty());
		String token = jwtUtil.generateToken(candidate);

		mockMvc.perform(post("/api/career-agent/chat")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"hi\",\"history\":[]}"))
				.andExpect(status().isOk())
				.andExpect(header().exists("X-Request-ID"))
				.andExpect(jsonPath("$.answer").value("Gemini: hi"))
				.andExpect(jsonPath("$.providerMode").value("GEMINI_API"));

		mockMvc.perform(post("/api/career-agent/chat")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"message\":\"What skills do I have?\",\"history\":[]}"))
				.andExpect(status().isOk())
				.andExpect(header().exists("X-Request-ID"))
				.andExpect(jsonPath("$.answer").value("Gemini: What skills do I have?"))
				.andExpect(jsonPath("$.providerMode").value("GEMINI_API"));

		verify(userRepository, org.mockito.Mockito.times(2)).findByEmail("candidate@example.test");
		verify(candidateProfileRepository, org.mockito.Mockito.times(2)).findByUserUserId(7);
	}
}
