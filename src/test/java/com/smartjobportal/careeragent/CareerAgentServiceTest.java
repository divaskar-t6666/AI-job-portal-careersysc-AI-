package com.smartjobportal.careeragent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.smartjobportal.careerai.CareerAgentAiProvider;
import com.smartjobportal.careerai.CareerAgentProviderUnavailableException;
import com.smartjobportal.careeragent.dto.CareerAgentChatRequest;
import com.smartjobportal.careeragent.dto.CareerAgentChatResponse;
import com.smartjobportal.careeragent.dto.OpportunityReadinessDto;
import com.smartjobportal.entity.CandidateProfile;
import com.smartjobportal.entity.CandidateEducation;
import com.smartjobportal.entity.CandidateExperience;
import com.smartjobportal.entity.CandidateSkill;
import com.smartjobportal.entity.Resume;
import com.smartjobportal.entity.User;
import com.smartjobportal.job.dto.OpportunityListingDto;
import com.smartjobportal.job.dto.OpportunityPageResponseDto;
import com.smartjobportal.job.service.OpportunityDiscoveryService;
import com.smartjobportal.repository.CandidateProfileRepository;
import com.smartjobportal.repository.CandidateEducationRepository;
import com.smartjobportal.repository.CandidateExperienceRepository;
import com.smartjobportal.repository.CandidateSkillRepository;
import com.smartjobportal.repository.ResumeRepository;
import com.smartjobportal.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CareerAgentServiceTest {

	@Mock private ResumeRepository resumeRepository;
	@Mock private CandidateProfileRepository candidateProfileRepository;
	@Mock private CandidateEducationRepository candidateEducationRepository;
	@Mock private CandidateExperienceRepository candidateExperienceRepository;
	@Mock private CandidateSkillRepository candidateSkillRepository;
	@Mock private UserRepository userRepository;
	@Mock private OpportunityDiscoveryService opportunityDiscoveryService;
	@Mock private CareerAgentAiProvider aiProvider;

	private CareerAgentService service;
	private CandidateProfile profile;
	private Resume resume;

	@BeforeEach
	void setUp() {
		service = new CareerAgentService(resumeRepository, candidateProfileRepository,
				candidateEducationRepository, candidateExperienceRepository, candidateSkillRepository,
				userRepository, opportunityDiscoveryService, aiProvider);
		User user = new User();
		user.setUserId(7);
		user.setEmail("candidate@example.test");
		profile = new CandidateProfile();
		profile.setUser(user);
		profile.setCandidateProfileId(11);
		profile.setYearsOfExperience(0.0);
		profile.setHighestQualification("Bachelor's degree");
		resume = new Resume();
		resume.setResumeId(23);
		resume.setCandidateProfile(profile);
		resume.setIsActive(true);
		resume.setParsedSkills("JS, SpringBoot");
		resume.setParsedExperience("Academic project experience.");

		authenticate("ROLE_CANDIDATE");
		lenient().when(userRepository.findByEmail("candidate@example.test")).thenReturn(Optional.of(user));
		lenient().when(candidateProfileRepository.findByUserUserId(7)).thenReturn(Optional.of(profile));
		lenient().when(resumeRepository
				.findFirstByCandidateProfileAndIsActiveOrderByUpdatedAtDescResumeIdDesc(profile, true))
				.thenReturn(Optional.of(resume));
		lenient().when(candidateEducationRepository.findByCandidateProfile(profile)).thenReturn(List.of());
		lenient().when(candidateExperienceRepository.findByCandidateProfile(profile)).thenReturn(List.of());
		lenient().when(candidateSkillRepository.findByCandidateProfile(profile)).thenReturn(List.of());
		OpportunityPageResponseDto emptyPage = new OpportunityPageResponseDto();
		emptyPage.setContent(List.of());
		lenient().when(opportunityDiscoveryService.search(any(), anyInt(), anyInt(), anyString(), anyString()))
				.thenReturn(emptyPage);
		lenient().when(aiProvider.isConfigured()).thenReturn(false);
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void rejectsBlankOversizedOrMalformedChatInput() {
		Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
		CareerAgentChatRequest request = new CareerAgentChatRequest();
		request.setMessage(" ");
		request.setOpportunityType("ADMIN");
		request.setOpportunityId(-2);
		assertFalse(validator.validate(request).isEmpty());

		request.setMessage("x".repeat(1201));
		request.setOpportunityType("JOB");
		request.setOpportunityId(1);
		assertFalse(validator.validate(request).isEmpty());
	}

	@Test
	void asksCandidateToUploadOrVerifyMissingParsedResume() {
		when(resumeRepository
				.findFirstByCandidateProfileAndIsActiveOrderByUpdatedAtDescResumeIdDesc(profile, true))
				.thenReturn(Optional.empty());
		when(opportunityDiscoveryService.getDetails("JOB", 41)).thenReturn(opportunity(41, "JOB"));
		OpportunityReadinessDto result = service.analyze("JOB", 41);
		assertFalse(result.isResumeAvailable());
		assertTrue(result.getMissingRequiredSkills().isEmpty());
		assertTrue(result.getMissingPreferredSkills().isEmpty());
		assertTrue(result.getInformationToConfirm().stream().anyMatch(value -> value.contains("Upload a resume")));
	}

	@Test
	void normalizesAliasesAndSeparatesMissingRequiredAndPreferredSkills() {
		OpportunityListingDto opportunity = opportunity(41, "INTERNSHIP");
		opportunity.setRequiredSkills("JavaScript, Spring Boot, SQL; Preferred: Docker");
		opportunity.setFresherEligible(true);
		when(opportunityDiscoveryService.getDetails("INTERNSHIP", 41)).thenReturn(opportunity);

		OpportunityReadinessDto result = service.analyze("INTERNSHIP", 41);

		assertEquals(List.of("JavaScript", "Spring Boot"), result.getMatchedSkills());
		assertEquals(List.of("SQL"), result.getMissingRequiredSkills());
		assertEquals(List.of("Docker"), result.getMissingPreferredSkills());
		assertTrue(result.getRequirementsSatisfied().stream().anyMatch(value -> value.contains("allows freshers")));
		assertTrue(result.getEligibilityAssessment().startsWith("May be eligible"));
	}

	@Test
	void distinguishesExplicitMandatoryExperienceFromPreferredOrAmbiguousExperience() {
		OpportunityListingDto mandatory = opportunity(51, "JOB");
		mandatory.setExperienceRequired(3);
		mandatory.setDescription("Minimum 3 years experience required.");
		when(opportunityDiscoveryService.getDetails("JOB", 51)).thenReturn(mandatory);
		profile.setYearsOfExperience(1.0);

		OpportunityReadinessDto mandatoryResult = service.analyze("JOB", 51);
		assertTrue(mandatoryResult.getEligibilityAssessment().startsWith("Does not meet"));

		OpportunityListingDto preferred = opportunity(52, "JOB");
		preferred.setExperienceRequired(3);
		preferred.setDescription("3 years experience preferred.");
		when(opportunityDiscoveryService.getDetails("JOB", 52)).thenReturn(preferred);
		OpportunityReadinessDto preferredResult = service.analyze("JOB", 52);
		assertTrue(preferredResult.getEligibilityAssessment().startsWith("May be eligible"));
		assertTrue(preferredResult.getInformationToConfirm().stream()
				.anyMatch(value -> value.contains("whether the listed experience is a strict minimum")));
	}

	@Test
	void reportsAmbiguousEligibilityAndProjectEvidenceWithoutRejectingCandidate() {
		OpportunityListingDto opportunity = opportunity(61, "INTERNSHIP");
		opportunity.setEligibilityCriteria("Relevant bachelor's degree required.");
		opportunity.setDescription("Applicants must have professional project experience.");
		opportunity.setExperienceRequired(2);
		profile.setYearsOfExperience(null);
		when(opportunityDiscoveryService.getDetails("INTERNSHIP", 61)).thenReturn(opportunity);

		OpportunityReadinessDto result = service.analyze("INTERNSHIP", 61);

		assertFalse(result.getProjectRequirements().isEmpty());
		assertTrue(result.getRequirementsSatisfied().stream().anyMatch(value -> value.contains("bachelor's")));
		assertTrue(result.getInformationToConfirm().stream().anyMatch(value -> value.contains("professional project experience")));
		assertTrue(result.getEligibilityAssessment().startsWith("May be eligible"));
	}

	@Test
	void fresherExclusionIsAnExplicitRequirementNotJustALowSkillScore() {
		OpportunityListingDto opportunity = opportunity(65, "INTERNSHIP");
		opportunity.setFresherEligible(false);
		when(opportunityDiscoveryService.getDetails("INTERNSHIP", 65)).thenReturn(opportunity);

		OpportunityReadinessDto result = service.analyze("INTERNSHIP", 65);

		assertTrue(result.getEligibilityAssessment().startsWith("Does not meet"));
		assertTrue(result.getRequirementsNotDemonstrated().stream()
				.anyMatch(value -> value.contains("freshers are not eligible")));
	}

	@Test
	void missingEligibilityAndExperienceFieldsRemainUncertain() {
		OpportunityListingDto opportunity = opportunity(66, "JOB");
		opportunity.setEligibilityCriteria(null);
		opportunity.setExperienceRequired(null);
		opportunity.setDescription("Software development role.");
		when(opportunityDiscoveryService.getDetails("JOB", 66)).thenReturn(opportunity);

		OpportunityReadinessDto result = service.analyze("JOB", 66);

		assertTrue(result.getEligibilityAssessment().startsWith("May be eligible"));
		assertTrue(result.getInformationToConfirm().stream()
				.anyMatch(value -> value.contains("does not specify eligibility criteria")));
		assertTrue(result.getExperienceAssessment().contains("No structured experience minimum"));
	}

	@Test
	void suppliesExactSavedCandidateAndOpportunityContextToGeminiAndReportsProviderFailure() {
		OpportunityListingDto opportunity = opportunity(71, "HACKATHON");
		opportunity.setEligibilityCriteria("Open to university students.");
		opportunity.setRequiredSkills("JavaScript, Spring Boot, SQL; Preferred: Docker");
		when(opportunityDiscoveryService.getDetails("HACKATHON", 71)).thenReturn(opportunity);
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any()))
				.thenThrow(new CareerAgentProviderUnavailableException("unavailable"));

		CareerAgentChatRequest request = new CareerAgentChatRequest();
		request.setMessage("What is the eligibility?");
		request.setOpportunityType("HACKATHON");
		request.setOpportunityId(71);
		ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.chat(request));

		verify(opportunityDiscoveryService).getDetails("HACKATHON", 71);
		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(aiProvider).generate(prompt.capture(), any(), any());
		assertTrue(prompt.getValue().contains("Parsed skills: JS, SpringBoot"));
		assertTrue(prompt.getValue().contains("Persisted opportunity (HACKATHON #71)"));
		assertTrue(prompt.getValue().contains("Open to university students"));
		assertTrue(prompt.getValue().contains("missingRequiredSkills=[SQL]"));
		assertTrue(prompt.getValue().contains("missingPreferredSkills=[Docker]"));
		assertEquals(503, exception.getStatusCode().value());
	}

	@Test
	void rejectsNonCandidateAccess() {
		authenticate("ROLE_RECRUITER");
		assertThrows(AccessDeniedException.class, () -> service.analyze("JOB", 1));
		CareerAgentChatRequest request = new CareerAgentChatRequest();
		request.setMessage("Hello");
		assertThrows(AccessDeniedException.class, () -> service.chat(request));
	}

	@Test
	void rejectsChatWithOnlyOnePartOfOpportunityContext() {
		CareerAgentChatRequest request = new CareerAgentChatRequest();
		request.setMessage("Can I apply?");
		request.setOpportunityType("JOB");
		assertThrows(ResponseStatusException.class, () -> service.chat(request));
	}

	@Test
	void sendsJavaLearningQuestionToGeminiWithAuthenticatedCandidateContext() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any())).thenReturn("Gemini Java learning plan");
		CareerAgentChatResponse response = chat("How many days does it take to learn basic Java?");

		assertEquals("Gemini Java learning plan", response.getAnswer());
		assertEquals("GEMINI_API", response.getProviderMode());
		verify(aiProvider).generate(any(), any(), any());
		verify(userRepository).findByEmail("candidate@example.test");
		verify(candidateProfileRepository).findByUserUserId(7);
	}

	@Test
	void sendsReactTimelineQuestionToGemini() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any())).thenReturn("Gemini React learning estimate");
		CareerAgentChatResponse response = chat("How long will it take to learn React?");

		assertEquals("Gemini React learning estimate", response.getAnswer());
		assertEquals("GEMINI_API", response.getProviderMode());
	}

	@Test
	void sendsLearningRoadmapRequestAndRecentHistoryToGemini() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any())).thenReturn("Gemini roadmap");
		CareerAgentChatRequest request = new CareerAgentChatRequest();
		request.setMessage("Give me a 7-day React learning roadmap.");
		List<CareerAgentChatRequest.CareerAgentChatTurn> requestHistory =
				List.of(turn("user", "I know JavaScript"), turn("assistant", "Great."));
		request.setHistory(requestHistory);

		CareerAgentChatResponse response = service.chat(request);

		verify(aiProvider).generate(any(), eq(List.of(
				new CareerAgentAiProvider.CareerAgentTurn("user", "I know JavaScript"),
				new CareerAgentAiProvider.CareerAgentTurn("assistant", "Great."))), any());
		assertEquals("Gemini roadmap", response.getAnswer());
		assertEquals("GEMINI_API", response.getProviderMode());
	}

	@Test
	void sendsGreetingsAndFollowUpMessagesToGeminiWithCandidateContext() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any())).thenReturn("Hello! What would you like to explore?");

		CareerAgentChatResponse response = chat("Hello");

		assertEquals("Hello! What would you like to explore?", response.getAnswer());
		verify(aiProvider).generate(any(), any(), any());
		verify(userRepository).findByEmail("candidate@example.test");
	}

	@Test
	void sendsGeneralProjectSuggestionsToGeminiWithCandidateContext() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any())).thenReturn("Try a small Java CLI project.");

		CareerAgentChatResponse response = chat("Recommend a Java project for beginners.");

		assertEquals("Try a small Java CLI project.", response.getAnswer());
		assertEquals("GEMINI_API", response.getProviderMode());
		verify(userRepository).findByEmail("candidate@example.test");
	}

	@Test
	void answersMySkillsUsingOnlyTheAuthenticatedCandidatesSavedResumeAndProfile() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any())).thenReturn("Your saved resume lists JavaScript and Spring Boot.");

		CareerAgentChatResponse response = chat("my skills");

		assertEquals("Your saved resume lists JavaScript and Spring Boot.", response.getAnswer());
		verify(userRepository).findByEmail("candidate@example.test");
		verify(candidateProfileRepository).findByUserUserId(7);
		verify(resumeRepository)
				.findFirstByCandidateProfileAndIsActiveOrderByUpdatedAtDescResumeIdDesc(profile, true);
		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(aiProvider).generate(prompt.capture(), any(), eq("my skills"));
		assertTrue(prompt.getValue().contains("Parsed skills: JS, SpringBoot"));
		assertTrue(prompt.getValue().contains("profile experience years: 0.0"));
		assertTrue(prompt.getValue().contains("profile qualification: Bachelor's degree"));
	}

	@Test
	void retrievesSavedSkillEducationExperienceAndProfileRecordsForGeminiContext() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any())).thenReturn("Your saved career summary.");
		CandidateSkill skill = new CandidateSkill();
		skill.setSkillName("PostgreSQL");
		skill.setSkillLevel(CandidateSkill.SkillLevel.ADVANCED);
		CandidateEducation education = new CandidateEducation();
		education.setDegree("BSc");
		education.setSpecialization("Computer Science");
		education.setInstitutionName("North University");
		CandidateExperience experience = new CandidateExperience();
		experience.setJobTitle("Backend Developer");
		experience.setCompanyName("Example Systems");
		when(candidateSkillRepository.findByCandidateProfile(profile)).thenReturn(List.of(skill));
		when(candidateEducationRepository.findByCandidateProfile(profile)).thenReturn(List.of(education));
		when(candidateExperienceRepository.findByCandidateProfile(profile)).thenReturn(List.of(experience));
		profile.setProfileHeadline("Backend engineer");
		profile.setProfessionalSummary("Builds database-backed services.");

		chat("Summarize my saved career background.");

		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(aiProvider).generate(prompt.capture(), any(), any());
		assertTrue(prompt.getValue().contains("PostgreSQL (level=ADVANCED"));
		assertTrue(prompt.getValue().contains("BSc"));
		assertTrue(prompt.getValue().contains("North University"));
		assertTrue(prompt.getValue().contains("Backend Developer"));
		assertTrue(prompt.getValue().contains("Example Systems"));
		assertTrue(prompt.getValue().contains("Backend engineer"));
		assertTrue(prompt.getValue().contains("Builds database-backed services."));
		verify(candidateSkillRepository).findByCandidateProfile(profile);
		verify(candidateEducationRepository).findByCandidateProfile(profile);
		verify(candidateExperienceRepository).findByCandidateProfile(profile);
	}

	@Test
	void candidateContextIsScopedToTheAuthenticatedUsersProfile() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any())).thenReturn("Saved skills.");
		User otherUser = new User();
		otherUser.setUserId(8);
		otherUser.setEmail("other@example.test");
		CandidateProfile otherProfile = new CandidateProfile();
		otherProfile.setUser(otherUser);
		otherProfile.setCandidateProfileId(12);
		otherProfile.setProfileHeadline("Other candidate profile");
		Resume otherResume = new Resume();
		otherResume.setCandidateProfile(otherProfile);
		otherResume.setIsActive(true);
		otherResume.setParsedSkills("Kotlin");
		authenticateAs("other@example.test", "ROLE_CANDIDATE");
		when(userRepository.findByEmail("other@example.test")).thenReturn(Optional.of(otherUser));
		when(candidateProfileRepository.findByUserUserId(8)).thenReturn(Optional.of(otherProfile));
		when(resumeRepository
				.findFirstByCandidateProfileAndIsActiveOrderByUpdatedAtDescResumeIdDesc(otherProfile, true))
				.thenReturn(Optional.of(otherResume));
		when(candidateSkillRepository.findByCandidateProfile(otherProfile)).thenReturn(List.of());
		when(candidateEducationRepository.findByCandidateProfile(otherProfile)).thenReturn(List.of());
		when(candidateExperienceRepository.findByCandidateProfile(otherProfile)).thenReturn(List.of());

		chat("What skills do I have?");

		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(aiProvider).generate(prompt.capture(), any(), any());
		assertTrue(prompt.getValue().contains("Parsed skills: Kotlin"));
		assertTrue(prompt.getValue().contains("Other candidate profile"));
		assertFalse(prompt.getValue().contains("JS, SpringBoot"));
		assertFalse(prompt.getValue().contains("candidate@example.test"));
		verify(candidateProfileRepository).findByUserUserId(8);
		verify(resumeRepository)
				.findFirstByCandidateProfileAndIsActiveOrderByUpdatedAtDescResumeIdDesc(otherProfile, true);
		verify(candidateSkillRepository).findByCandidateProfile(otherProfile);
	}

	@Test
	void saysWhenCandidateRecordsAreMissingInsteadOfInventingResumeDetails() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any())).thenReturn("No saved skills are available.");
		when(candidateProfileRepository.findByUserUserId(7)).thenReturn(Optional.empty());

		chat("What skills do I have?");

		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(aiProvider).generate(prompt.capture(), any(), any());
		assertTrue(prompt.getValue().contains("No active resume is saved."));
		assertTrue(prompt.getValue().contains("No candidate profile is saved."));
		assertTrue(prompt.getValue().contains("No candidate skill records are saved."));
		assertTrue(prompt.getValue().contains("No education records are saved."));
		assertTrue(prompt.getValue().contains("No work experience records are saved."));
		verify(resumeRepository, org.mockito.Mockito.never())
				.findFirstByCandidateProfileAndIsActiveOrderByUpdatedAtDescResumeIdDesc(any(), any());
	}

	@Test
	void recommendsSkillImprovementsOnlyFromSavedMatchedOpportunityRequirements() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any())).thenReturn("Consider SQL.");
		OpportunityListingDto match = opportunity(93, "JOB");
		match.setRequiredSkills("JavaScript, Spring Boot, SQL");
		OpportunityPageResponseDto page = new OpportunityPageResponseDto();
		page.setContent(List.of(match));
		when(opportunityDiscoveryService.search(any(), eq(0), eq(100), eq("createdAt"), eq("desc")))
				.thenReturn(page);

		chat("Which skills should I improve?");

		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(aiProvider).generate(prompt.capture(), any(), eq("Which skills should I improve?"));
		assertTrue(prompt.getValue().contains("requiredSkills=JavaScript, Spring Boot, SQL"));
		assertTrue(prompt.getValue().contains("missingRequiredSkills=SQL"));
		assertTrue(prompt.getValue().contains("Skills to consider based on missing requirements"));
		assertTrue(prompt.getValue().contains("not proof that you lack them"));
	}

	@Test
	void handlesConsecutiveChatRequestsWithFreshAuthenticatedContextAndGeminiResponses() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any())).thenAnswer(invocation ->
				"Gemini answer: " + invocation.getArgument(2, String.class));

		assertEquals("Gemini answer: Hello", chat("Hello").getAnswer());
		assertEquals("Gemini answer: What skills do I have?", chat("What skills do I have?").getAnswer());

		verify(userRepository, org.mockito.Mockito.times(2)).findByEmail("candidate@example.test");
		verify(resumeRepository, org.mockito.Mockito.times(2))
				.findFirstByCandidateProfileAndIsActiveOrderByUpdatedAtDescResumeIdDesc(profile, true);
		verify(aiProvider, org.mockito.Mockito.times(2)).generate(any(), any(), any());
	}

	@Test
	void learnsNextBasedOnCurrentSkillsUsesSavedCandidateContext() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any())).thenReturn("Build on your saved skills.");

		CareerAgentChatResponse response = chat("What should I learn next based on my current skills?");

		assertEquals("Build on your saved skills.", response.getAnswer());
		verify(userRepository).findByEmail("candidate@example.test");
		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(aiProvider).generate(prompt.capture(), any(), eq("What should I learn next based on my current skills?"));
		assertTrue(prompt.getValue().contains("Parsed skills: JS, SpringBoot"));
	}

	@Test
	void reportsMissingGoogleApiKeyInsteadOfReturningDeterministicChatText() {
		CareerAgentChatRequest request = new CareerAgentChatRequest();
		request.setMessage("Give me a 7-day React learning roadmap.");
		ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.chat(request));

		assertEquals(503, exception.getStatusCode().value());
		assertTrue(exception.getReason().contains("GOOGLE_API_KEY"));
		verify(aiProvider).isConfigured();
		verify(userRepository).findByEmail("candidate@example.test");
	}

	@Test
	void reportsGeminiApiFailureInsteadOfReturningDeterministicChatText() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any()))
				.thenThrow(new CareerAgentProviderUnavailableException("API error"));

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> chat("Give me a 7-day React learning roadmap."));

		assertEquals(503, exception.getStatusCode().value());
		assertEquals("The AI assistant is temporarily unavailable. Please try again later.", exception.getReason());
	}

	@Test
	void usesOnlyPersistedMatchedOpportunitiesForRecommendations() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any())).thenReturn("Saved recommendations");
		OpportunityListingDto match = opportunity(89, "INTERNSHIP");
		match.setRequiredSkills("JavaScript, Spring Boot");
		OpportunityListingDto hackathon = opportunity(90, "HACKATHON");
		hackathon.setRequiredSkills("JavaScript, Spring Boot");
		OpportunityPageResponseDto page = new OpportunityPageResponseDto();
		page.setContent(List.of(match, hackathon));
		when(opportunityDiscoveryService.search(any(), eq(0), eq(100), eq("createdAt"), eq("desc")))
				.thenReturn(page);
		CareerAgentChatResponse response = chat("Which jobs and hackathons suit my skills?");

		assertEquals("Saved recommendations", response.getAnswer());
		assertEquals("GEMINI_API", response.getProviderMode());
		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(aiProvider).generate(prompt.capture(), any(), any());
		assertTrue(prompt.getValue().contains("id=89"));
		assertTrue(prompt.getValue().contains("id=90"));
		assertTrue(prompt.getValue().contains("matchedSkills=JavaScript, Spring Boot"));
	}

	private CareerAgentChatRequest.CareerAgentChatTurn turn(String role, String content) {
		CareerAgentChatRequest.CareerAgentChatTurn turn = new CareerAgentChatRequest.CareerAgentChatTurn();
		turn.setRole(role);
		turn.setContent(content);
		return turn;
	}

	@Test
	void includesCandidateDataInGeminiPromptForGeneralLearningQuestion() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any())).thenReturn("Gemini answer");
		chat("Can I learn Spring Boot in 30 days?");

		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(aiProvider).generate(prompt.capture(), any(), any());
		assertTrue(prompt.getValue().contains("Parsed skills: JS, SpringBoot"));
		assertTrue(prompt.getValue().contains("Candidate profile:"));
		assertTrue(prompt.getValue().contains("Saved candidate skills:"));
	}

	@Test
	void generatesChatResponsesFromGeminiInsteadOfDeterministicRoadmapText() {
		when(aiProvider.isConfigured()).thenReturn(true);
		when(aiProvider.generate(any(), any(), any())).thenReturn("Gemini roadmap response");
		CareerAgentChatResponse response = chat("Give me a 7-day React learning roadmap.");

		assertEquals("Gemini roadmap response", response.getAnswer());
		assertEquals("GEMINI_API", response.getProviderMode());
		verify(userRepository).findByEmail("candidate@example.test");
	}

	private CareerAgentChatResponse chat(String message) {
		CareerAgentChatRequest request = new CareerAgentChatRequest();
		request.setMessage(message);
		return service.chat(request);
	}

	private OpportunityListingDto opportunity(Integer id, String type) {
		OpportunityListingDto opportunity = new OpportunityListingDto();
		opportunity.setId(id);
		opportunity.setOpportunityType(type);
		opportunity.setTitle("Saved opportunity " + id);
		opportunity.setCompanyName("Test company");
		opportunity.setDescription("Build software with required skills.");
		opportunity.setRequiredSkills("JavaScript, Spring Boot");
		opportunity.setEligibilityCriteria("Eligibility not specified.");
		opportunity.setFresherEligible(null);
		opportunity.setStatus("OPEN");
		opportunity.setApplicationDeadline(LocalDateTime.now().plusDays(5));
		return opportunity;
	}

	private void authenticate(String role) {
		authenticateAs("candidate@example.test", role);
	}

	private void authenticateAs(String email, String role) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(email, "n/a",
						List.of(new SimpleGrantedAuthority(role))));
	}
}
