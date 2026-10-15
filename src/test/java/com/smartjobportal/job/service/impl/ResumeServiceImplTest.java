package com.smartjobportal.job.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.smartjobportal.entity.CandidateProfile;
import com.smartjobportal.entity.Resume;
import com.smartjobportal.entity.User;
import com.smartjobportal.job.dto.ResumeResponseDto;
import com.smartjobportal.job.dto.UpdateResumeDto;
import com.smartjobportal.job.service.JobMatchingService;
import com.smartjobportal.job.service.ResumeUploadService;
import com.smartjobportal.resumeparser.dto.ResumeParserResultDto;
import com.smartjobportal.resumeparser.exception.ResumeParsingException;
import com.smartjobportal.resumeparser.service.ResumeParserService;
import com.smartjobportal.repository.CandidateProfileRepository;
import com.smartjobportal.repository.JobApplicationRepository;
import com.smartjobportal.repository.JobMatchRepository;
import com.smartjobportal.repository.ResumeRepository;
import com.smartjobportal.repository.UserRepository;

import org.mockito.Mockito;

class ResumeServiceImplTest {

	private final ResumeRepository resumeRepository = Mockito.mock(ResumeRepository.class);
	private final CandidateProfileRepository candidateProfileRepository = Mockito
			.mock(CandidateProfileRepository.class);
	private final JobMatchRepository jobMatchRepository = Mockito.mock(JobMatchRepository.class);
	private final JobMatchingService jobMatchingService = Mockito.mock(JobMatchingService.class);
	private final UserRepository userRepository = Mockito.mock(UserRepository.class);
	private final JobApplicationRepository jobApplicationRepository = Mockito
			.mock(JobApplicationRepository.class);
	private final ResumeUploadService resumeUploadService = Mockito.mock(ResumeUploadService.class);
	private final ResumeParserService resumeParserService = Mockito.mock(ResumeParserService.class);
	private final ResumeServiceImpl service = new ResumeServiceImpl();

	private final User user = new User();
	private final CandidateProfile candidate = new CandidateProfile();
	private final Resume resume = new Resume();
	private final Resume otherResume = new Resume();

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(service, "resumeRepository", resumeRepository);
		ReflectionTestUtils.setField(service, "candidateProfileRepository", candidateProfileRepository);
		ReflectionTestUtils.setField(service, "jobMatchRepository", jobMatchRepository);
		ReflectionTestUtils.setField(service, "jobMatchingService", jobMatchingService);
		ReflectionTestUtils.setField(service, "userRepository", userRepository);
		ReflectionTestUtils.setField(service, "jobApplicationRepository", jobApplicationRepository);
		ReflectionTestUtils.setField(service, "resumeUploadService", resumeUploadService);
		ReflectionTestUtils.setField(service, "resumeParserService", resumeParserService);

		user.setUserId(7);
		user.setEmail("candidate@example.com");
		candidate.setUser(user);
		candidate.setCandidateProfileId(11);
		resume.setResumeId(42);
		resume.setCandidateProfile(candidate);
		resume.setIsActive(false);
		resume.setFileName("old-resume.pdf");
		resume.setFilePath("uploads/old-resume.pdf");
		resume.setFileType("application/pdf");
		resume.setFileSizeKb(BigDecimal.valueOf(100));
		resume.setParsedSkills("Old skills");
		resume.setParsedExperience("Old experience");
		resume.setParsedEducation("Old education");
		otherResume.setResumeId(43);
		otherResume.setCandidateProfile(candidate);
		otherResume.setIsActive(true);

		when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(user));
		when(candidateProfileRepository.findByUserUserId(7)).thenReturn(Optional.of(candidate));
		when(resumeRepository.findById(42)).thenReturn(Optional.of(resume));
		when(resumeRepository.findByCandidateProfile(candidate)).thenReturn(List.of(resume, otherResume));
		when(resumeRepository.findByCandidateProfileAndIsActive(candidate, true)).thenReturn(List.of(resume));
		when(resumeRepository.save(Mockito.any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(jobMatchRepository.findByResume(resume)).thenReturn(List.of());

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
	void updatingResumeActivatesSavedResumeAndDeactivatesOtherResume() {
		UpdateResumeDto update = new UpdateResumeDto();
		update.setResumeTitle("Updated resume");
		update.setParsedSkills("Java, Spring Boot");
		update.setParsedExperience("5 years");
		update.setParsedEducation("Bachelor's degree");

		var saved = service.updateResume(42, update);
		ResumeResponseDto activeResume = service.getActiveResume();

		assertEquals(42, saved.getResumeId());
		assertEquals("Java, Spring Boot", saved.getParsedSkills());
		assertTrue(saved.getIsActive());
		assertTrue(resume.getIsActive());
		assertFalse(otherResume.getIsActive());
		assertEquals(42, activeResume.getResumeId());
		assertEquals("Java, Spring Boot", activeResume.getParsedSkills());
		assertEquals("5 years", activeResume.getParsedExperience());
		assertEquals("Bachelor's degree", activeResume.getParsedEducation());
		verify(jobMatchingService).getRecommendedJobs(42);
	}

	@Test
	void replacingResumeParsesAndPersistsNewDetailsThenRefreshesMatches() {
		MultipartFile file = Mockito.mock(MultipartFile.class);
		when(file.getSize()).thenReturn(2048L);
		when(resumeUploadService.replaceResumeFile(file)).thenReturn("new-resume.pdf");
		doAnswer(invocation -> {
			resume.setParsedSkills("Python, Django");
			resume.setParsedExperience("Backend Developer | Example Co");
			resume.setParsedEducation("B.Sc | Example University");
			return new ResumeParserResultDto();
		}).when(resumeParserService).parseResume(42);
		when(jobMatchRepository.findByResume(resume)).thenReturn(List.of());

		ResumeResponseDto result = service.replaceResumeFile(42, file);

		assertEquals(42, result.getResumeId());
		assertEquals("new-resume.pdf", result.getFileName());
		assertEquals("uploads\\new-resume.pdf", result.getFilePath());
		assertEquals("Python, Django", result.getParsedSkills());
		assertEquals("Backend Developer | Example Co", result.getParsedExperience());
		assertEquals("B.Sc | Example University", result.getParsedEducation());
		assertTrue(result.getIsActive());
		assertFalse(otherResume.getIsActive());
		verify(resumeParserService).parseResume(42);
		verify(jobMatchRepository).deleteAll(List.of());
		verify(jobMatchingService).getRecommendedJobs(42);
		verify(resumeUploadService).deleteResumeFile("old-resume.pdf");
	}

	@Test
	void failedReplacementParsingPreservesExistingResumeAndDeletesNewFile() {
		MultipartFile file = Mockito.mock(MultipartFile.class);
		when(file.getSize()).thenReturn(2048L);
		when(resumeUploadService.replaceResumeFile(file)).thenReturn("invalid-resume.pdf");
		Mockito.doThrow(new ResumeParsingException("No readable text was found"))
				.when(resumeParserService).parseResume(42);

		assertThrows(ResumeParsingException.class, () -> service.replaceResumeFile(42, file));

		assertEquals("old-resume.pdf", resume.getFileName());
		assertEquals("uploads/old-resume.pdf", resume.getFilePath());
		assertEquals("Old skills", resume.getParsedSkills());
		assertEquals("Old experience", resume.getParsedExperience());
		assertEquals("Old education", resume.getParsedEducation());
		verify(resumeUploadService).deleteResumeFile("invalid-resume.pdf");
		verify(resumeUploadService, never()).deleteResumeFile("old-resume.pdf");
		Mockito.verifyNoInteractions(jobMatchingService);
	}
}
