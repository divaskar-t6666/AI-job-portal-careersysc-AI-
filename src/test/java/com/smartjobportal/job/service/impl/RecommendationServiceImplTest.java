package com.smartjobportal.job.service.impl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.smartjobportal.entity.CandidateProfile;
import com.smartjobportal.entity.Resume;
import com.smartjobportal.entity.User;
import com.smartjobportal.job.dto.RecommendedJobDto;
import com.smartjobportal.job.service.JobMatchingService;
import com.smartjobportal.repository.CandidateProfileRepository;
import com.smartjobportal.repository.ResumeRepository;
import com.smartjobportal.repository.UserRepository;

import org.mockito.Mockito;

class RecommendationServiceImplTest {

	private final ResumeRepository resumeRepository = Mockito.mock(ResumeRepository.class);
	private final UserRepository userRepository = Mockito.mock(UserRepository.class);
	private final CandidateProfileRepository candidateProfileRepository = Mockito
			.mock(CandidateProfileRepository.class);
	private final JobMatchingService jobMatchingService = Mockito.mock(JobMatchingService.class);
	private final RecommendationServiceImpl service = new RecommendationServiceImpl();

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(service, "resumeRepository", resumeRepository);
		ReflectionTestUtils.setField(service, "userRepository", userRepository);
		ReflectionTestUtils.setField(service, "candidateProfileRepository", candidateProfileRepository);
		ReflectionTestUtils.setField(service, "jobMatchingService", jobMatchingService);

		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(
						"candidate@example.com",
						"token",
						List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"))));
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void recommendationsAreCalculatedFromTheLoggedInCandidatesActiveResume() {
		User user = new User();
		user.setUserId(7);
		CandidateProfile candidate = new CandidateProfile();
		Resume resume = new Resume();
		resume.setResumeId(42);
		RecommendedJobDto recommendation = new RecommendedJobDto();
		recommendation.setJobId(31);
		List<RecommendedJobDto> expected = List.of(recommendation);

		when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(user));
		when(candidateProfileRepository.findByUserUserId(7)).thenReturn(Optional.of(candidate));
		when(resumeRepository.findByCandidateProfileAndIsActive(candidate, true)).thenReturn(List.of(resume));
		when(jobMatchingService.getRecommendedJobs(42)).thenReturn(expected);

		assertSame(expected, service.getRecommendations());
		verify(jobMatchingService).getRecommendedJobs(42);
	}
}
