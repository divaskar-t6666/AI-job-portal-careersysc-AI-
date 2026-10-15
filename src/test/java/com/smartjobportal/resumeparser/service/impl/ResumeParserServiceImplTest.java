package com.smartjobportal.resumeparser.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.smartjobportal.entity.CandidateProfile;
import com.smartjobportal.entity.CandidateSkill;
import com.smartjobportal.entity.Resume;
import com.smartjobportal.entity.User;
import com.smartjobportal.repository.CandidateEducationRepository;
import com.smartjobportal.repository.CandidateExperienceRepository;
import com.smartjobportal.repository.CandidateProfileRepository;
import com.smartjobportal.repository.CandidateSkillRepository;
import com.smartjobportal.repository.ResumeRepository;
import com.smartjobportal.repository.UserRepository;
import com.smartjobportal.resumeparser.dto.CandidateInfoDto;
import com.smartjobportal.resumeparser.dto.EducationDto;
import com.smartjobportal.resumeparser.dto.ExperienceDto;
import com.smartjobportal.resumeparser.exception.ResumeParsingException;
import com.smartjobportal.resumeparser.extractor.TextExtractionService;
import com.smartjobportal.resumeparser.parser.CandidateInfoParser;
import com.smartjobportal.resumeparser.parser.EducationParser;
import com.smartjobportal.resumeparser.parser.ExperienceParser;
import com.smartjobportal.resumeparser.parser.SkillParser;

import org.mockito.Mockito;

class ResumeParserServiceImplTest {

	private final ResumeRepository resumeRepository = Mockito.mock(ResumeRepository.class);
	private final CandidateProfileRepository candidateProfileRepository = Mockito
			.mock(CandidateProfileRepository.class);
	private final UserRepository userRepository = Mockito.mock(UserRepository.class);
	private final TextExtractionService textExtractionService = Mockito.mock(TextExtractionService.class);
	private final CandidateInfoParser candidateInfoParser = Mockito.mock(CandidateInfoParser.class);
	private final SkillParser skillParser = Mockito.mock(SkillParser.class);
	private final EducationParser educationParser = Mockito.mock(EducationParser.class);
	private final ExperienceParser experienceParser = Mockito.mock(ExperienceParser.class);
	private final CandidateSkillRepository candidateSkillRepository = Mockito
			.mock(CandidateSkillRepository.class);
	private final CandidateEducationRepository candidateEducationRepository = Mockito
			.mock(CandidateEducationRepository.class);
	private final CandidateExperienceRepository candidateExperienceRepository = Mockito
			.mock(CandidateExperienceRepository.class);
	private final ResumeParserServiceImpl service = new ResumeParserServiceImpl();

	private final User user = new User();
	private final CandidateProfile candidate = new CandidateProfile();
	private final Resume resume = new Resume();

	@BeforeEach
	void setUp() throws Exception {
		ReflectionTestUtils.setField(service, "resumeRepository", resumeRepository);
		ReflectionTestUtils.setField(service, "candidateProfileRepository", candidateProfileRepository);
		ReflectionTestUtils.setField(service, "userRepository", userRepository);
		ReflectionTestUtils.setField(service, "textExtractionService", textExtractionService);
		ReflectionTestUtils.setField(service, "candidateInfoParser", candidateInfoParser);
		ReflectionTestUtils.setField(service, "skillParser", skillParser);
		ReflectionTestUtils.setField(service, "educationParser", educationParser);
		ReflectionTestUtils.setField(service, "experienceParser", experienceParser);
		ReflectionTestUtils.setField(service, "candidateSkillRepository", candidateSkillRepository);
		ReflectionTestUtils.setField(service, "candidateEducationRepository", candidateEducationRepository);
		ReflectionTestUtils.setField(service, "candidateExperienceRepository", candidateExperienceRepository);

		user.setUserId(7);
		user.setEmail("candidate@example.com");
		candidate.setUser(user);
		candidate.setCandidateProfileId(11);
		resume.setResumeId(42);
		resume.setCandidateProfile(candidate);
		resume.setFilePath("uploads/resume.pdf");

		when(resumeRepository.findById(42)).thenReturn(Optional.of(resume));
		when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(user));
		when(candidateProfileRepository.findByUserUserId(7)).thenReturn(Optional.of(candidate));
		when(textExtractionService.extractText(any(File.class))).thenReturn("resume text");
		when(candidateInfoParser.parse("resume text")).thenReturn(new CandidateInfoDto());
		when(skillParser.parse("resume text")).thenReturn(List.of("Java", "Spring"));
		when(educationParser.parse("resume text"))
				.thenReturn(List.of(new EducationDto("B.Tech", "Example University", "2020")));
		when(experienceParser.parse("resume text"))
				.thenReturn(List.of(new ExperienceDto("Example Systems", "Java Developer", "3 Years")));

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
	void parsingResumePersistsSkillsExperienceAndEducationOnOwnedResume() {
		var result = service.parseResume(42);

		assertEquals("Java, Spring", resume.getParsedSkills());
		assertEquals("Java Developer | Example Systems | 3 Years", resume.getParsedExperience());
		assertEquals("B.Tech | Example University | 2020", resume.getParsedEducation());
		assertEquals(List.of("Java", "Spring"), result.getSkills());
		assertEquals("Example Systems", result.getExperience().get(0).getCompanyName());
		assertEquals("Example University", result.getEducation().get(0).getInstitution());
		verify(resumeRepository).save(resume);
		verify(candidateSkillRepository).deleteByCandidateProfile(candidate);
		verify(candidateEducationRepository).deleteByCandidateProfile(candidate);
		verify(candidateExperienceRepository).deleteByCandidateProfile(candidate);
	}

	@Test
	void parsingReplacementKeepsManualSkillsAndUsesNewExtractedSkillsWithoutDuplicates() {
		resume.setParsedSkills("JavaScript, HTML, CSS, Legacy PDF Skill, Docker, Kubernetes, docker");

		CandidateSkill previousJavascript = candidateSkill("JavaScript");
		CandidateSkill previousHtml = candidateSkill("HTML");
		CandidateSkill previousCss = candidateSkill("CSS");
		CandidateSkill previousPdfOnly = candidateSkill("Legacy PDF Skill");
		when(candidateSkillRepository.findByCandidateProfile(candidate))
				.thenReturn(List.of(previousJavascript, previousHtml, previousCss, previousPdfOnly));
		when(skillParser.parse("resume text"))
				.thenReturn(List.of("JavaScript", "HTML", "CSS", "Python", "python"));

		var result = service.parseResume(42);
		List<String> expectedSkills = List.of("JavaScript", "HTML", "CSS", "Python", "Docker", "Kubernetes");

		assertEquals(expectedSkills, result.getSkills());
		assertEquals(String.join(", ", expectedSkills), resume.getParsedSkills());
		ArgumentCaptor<CandidateSkill> savedSkills = ArgumentCaptor.forClass(CandidateSkill.class);
		verify(candidateSkillRepository, times(expectedSkills.size())).save(savedSkills.capture());
		assertEquals(expectedSkills,
				savedSkills.getAllValues().stream().map(CandidateSkill::getSkillName).toList());
	}

	@Test
	void parsingRejectsResumeWhenNoReadableTextWasExtracted() throws Exception {
		when(textExtractionService.extractText(any(File.class))).thenReturn("  ");

		assertThrows(ResumeParsingException.class, () -> service.parseResume(42));
		verify(resumeRepository, never()).save(any(Resume.class));
	}

	@Test
	void parsingRejectsResumeWhenAuthenticatedCandidateDoesNotOwnIt() {
		CandidateProfile otherOwner = new CandidateProfile();
		otherOwner.setCandidateProfileId(99);
		resume.setCandidateProfile(otherOwner);

		assertThrows(AccessDeniedException.class, () -> service.parseResume(42));
		Mockito.verifyNoInteractions(textExtractionService);
	}

	private CandidateSkill candidateSkill(String name) {
		CandidateSkill skill = new CandidateSkill();
		skill.setCandidateProfile(candidate);
		skill.setSkillName(name);
		return skill;
	}
}
