package com.smartjobportal.job.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import com.smartjobportal.job.service.impl.JobServiceImpl;

import com.smartjobportal.entity.Company;
import com.smartjobportal.entity.Opportunity;
import com.smartjobportal.entity.RecruiterProfile;
import com.smartjobportal.entity.User;
import com.smartjobportal.job.dto.CreateHackathonDto;
import com.smartjobportal.job.dto.CreateInternshipDto;
import com.smartjobportal.job.dto.OpportunityListingDto;
import com.smartjobportal.job.dto.OpportunitySearchDto;
import com.smartjobportal.job.dto.OpportunityResponseDto;
import com.smartjobportal.repository.CompanyRepository;
import com.smartjobportal.repository.JobRepository;
import com.smartjobportal.repository.OpportunityRepository;
import com.smartjobportal.repository.RecruiterProfileRepository;
import com.smartjobportal.repository.UserRepository;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(OpportunityService.class)
class OpportunityServiceIntegrationTest {

	@Autowired
	private OpportunityService opportunityService;

	@Autowired
	private OpportunityRepository opportunityRepository;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private RecruiterProfileRepository recruiterProfileRepository;

	private Validator validator;
	private User recruiter;
	private Company company;

	@BeforeEach
	void setUp() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
		company = new Company();
		company.setCompanyName("CareerSync Test Company");
		company.setCompanyPhone("+1 555 0100");
		company.setHeadquartersLocation("Test City");
		company = companyRepository.save(company);

		recruiter = new User();
		recruiter.setEmail("recruiter@example.test");
		recruiter.setPassword("not-a-real-password");
		recruiter.setRole("RECRUITER");
		recruiter = userRepository.save(recruiter);

		RecruiterProfile profile = new RecruiterProfile();
		profile.setUser(recruiter);
		profile.setCompany(company);
		profile.setCompanyName(company.getCompanyName());
		recruiterProfileRepository.save(profile);
		authenticate("ROLE_RECRUITER");
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createsAndPersistsInternshipWithAuthenticatedCompany() {
		CreateInternshipDto request = internshipRequest();
		OpportunityResponseDto response = opportunityService.createInternship(request);

		Opportunity saved = opportunityRepository.findById(response.getOpportunityId()).orElseThrow();
		assertEquals("Backend Engineering Intern", saved.getTitle());
		assertEquals("PAID", saved.getStipendType());
		assertEquals(new BigDecimal("1250.00"), saved.getStipendAmount());
		assertEquals(Boolean.TRUE, saved.getFresherEligible());
		assertEquals(company.getCompanyId(), saved.getCompany().getCompanyId());
		assertEquals(recruiter.getUserId(), saved.getRecruiterProfile().getUser().getUserId());
		assertEquals("CareerSync Test Company", response.getCompanyName());
		assertEquals("+1 555 0100", response.getCompanyPhone());
		assertEquals("Test City", response.getCompanyAddress());
	}

	@Test
	void createsAndPersistsHackathonDetails() {
		CreateHackathonDto request = new CreateHackathonDto();
		request.setTitle("Climate Tech Challenge");
		request.setDescription("Build a practical climate technology prototype.");
		request.setProblemStatement("Reduce energy waste in urban buildings.");
		request.setRequiredSkills("Java, data analysis");
		request.setEligibilityCriteria("Open to students and professionals.");
		request.setDuration("48 hours");
		request.setParticipationOptions("BOTH");
		request.setWorkMode("ONLINE");
		request.setLocation("Online");
		request.setApplicationDeadline(LocalDateTime.now().plusDays(5));
		request.setEventDate(LocalDateTime.now().plusDays(8));
		request.setEventEndDate(LocalDateTime.now().plusDays(9));
		request.setVenueAddress("Convention Center, Test City");
		request.setPrizes("Cash prizes and mentorship");
		request.setJudgingCriteria("Innovation, impact, and execution");

		OpportunityResponseDto response = opportunityService.createHackathon(request);
		Opportunity saved = opportunityRepository.findById(response.getOpportunityId()).orElseThrow();

		assertEquals("HACKATHON", saved.getOpportunityType().name());
		assertEquals("Reduce energy waste in urban buildings.", saved.getProblemStatement());
		assertEquals("BOTH", saved.getParticipationOptions());
		assertEquals("Cash prizes and mentorship", saved.getPrizes());
		assertEquals("Innovation, impact, and execution", saved.getJudgingCriteria());
		assertEquals("Convention Center, Test City", saved.getVenueAddress());
		assertNotNull(saved.getEventDate());
		assertNotNull(saved.getEventEndDate());
		assertEquals("Test City", response.getCompanyAddress());
	}

	@Test
	void recruiterPostsAreSearchableAndResolvableByCandidateOpportunityId() {
		OpportunityResponseDto internship =
				opportunityService.createInternship(internshipRequest());

		CreateHackathonDto hackathonRequest = new CreateHackathonDto();
		hackathonRequest.setTitle("CareerSync Java Hackathon");
		hackathonRequest.setDescription("Build a Java project.");
		hackathonRequest.setProblemStatement("Build a new developer tool.");
		hackathonRequest.setRequiredSkills("Java, APIs");
		hackathonRequest.setEligibilityCriteria("Open to everyone.");
		hackathonRequest.setDuration("24 hours");
		hackathonRequest.setParticipationOptions("BOTH");
		hackathonRequest.setWorkMode("ONLINE");
		hackathonRequest.setLocation("Online");
		hackathonRequest.setApplicationDeadline(LocalDateTime.now().plusDays(5));
		hackathonRequest.setEventDate(LocalDateTime.now().plusDays(8));
		hackathonRequest.setEventEndDate(LocalDateTime.now().plusDays(9));
		hackathonRequest.setVenueAddress("Convention Center, Test City");
		hackathonRequest.setJudgingCriteria("Innovation, impact, and execution");
		OpportunityResponseDto hackathon = opportunityService.createHackathon(hackathonRequest);

		JobServiceImpl jobs = new JobServiceImpl();
		ReflectionTestUtils.setField(jobs, "jobRepository", jobRepository);
		OpportunityDiscoveryService discoveryService = new OpportunityDiscoveryService(
				jobs, opportunityRepository, opportunityService);
		authenticateAs("candidate@example.test", "ROLE_CANDIDATE");

		OpportunitySearchDto internshipSearch = new OpportunitySearchDto();
		internshipSearch.setOpportunityType("INTERNSHIP");
		internshipSearch.setKeyword("internship");
		OpportunityListingDto foundInternship =
				discoveryService.search(internshipSearch, 0, 10, "createdAt", "desc")
						.getContent().get(0);
		assertEquals(internship.getOpportunityId(), foundInternship.getId());
		assertEquals(internship.getTitle(),
				discoveryService.getDetails("INTERNSHIP", foundInternship.getId()).getTitle());

		OpportunitySearchDto hackathonSearch = new OpportunitySearchDto();
		hackathonSearch.setOpportunityType("HACKATHON");
		hackathonSearch.setKeyword("java");
		OpportunityListingDto foundHackathon =
				discoveryService.search(hackathonSearch, 0, 10, "createdAt", "desc")
						.getContent().get(0);
		assertEquals(hackathon.getOpportunityId(), foundHackathon.getId());
		assertEquals(hackathon.getTitle(),
				discoveryService.getDetails("HACKATHON", foundHackathon.getId()).getTitle());
		OpportunityListingDto hackathonDetails =
				discoveryService.getDetails("HACKATHON", foundHackathon.getId());
		assertEquals("Convention Center, Test City", hackathonDetails.getVenueAddress());
		assertEquals("Innovation, impact, and execution", hackathonDetails.getJudgingCriteria());
	}

	@Test
	void rejectsInvalidRequestsAndPaidInternshipWithoutAmount() {
		CreateInternshipDto invalid = new CreateInternshipDto();
		assertFalse(validator.validate(invalid).isEmpty());

		CreateInternshipDto missingAmount = internshipRequest();
		missingAmount.setStipendAmount(null);
		ResponseStatusException error = assertThrows(
				ResponseStatusException.class,
				() -> opportunityService.createInternship(missingAmount));
		assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
		assertEquals(0, opportunityRepository.count());
	}

	@Test
	void candidateCannotCreateOpportunity() {
		authenticate("ROLE_CANDIDATE");
		assertThrows(
				AccessDeniedException.class,
				() -> opportunityService.createInternship(internshipRequest()));
		assertEquals(0, opportunityRepository.count());
	}

	@Test
	void recruiterListingOnlyReturnsThatRecruitersOpportunities() {
		opportunityService.createInternship(internshipRequest());

		Company otherCompany = new Company();
		otherCompany.setCompanyName("Other Company");
		otherCompany = companyRepository.save(otherCompany);
		User otherUser = new User();
		otherUser.setEmail("other@example.test");
		otherUser.setPassword("not-a-real-password");
		otherUser.setRole("RECRUITER");
		otherUser = userRepository.save(otherUser);
		RecruiterProfile otherProfile = new RecruiterProfile();
		otherProfile.setUser(otherUser);
		otherProfile.setCompany(otherCompany);
		recruiterProfileRepository.save(otherProfile);
		authenticateAs("other@example.test", "ROLE_RECRUITER");
		opportunityService.createInternship(internshipRequest());

		authenticate("ROLE_RECRUITER");
		List<OpportunityResponseDto> ownOpportunities = opportunityService.getMyOpportunities();
		assertEquals(1, ownOpportunities.size());
		assertEquals("CareerSync Test Company", ownOpportunities.get(0).getCompanyName());
	}

	private CreateInternshipDto internshipRequest() {
		CreateInternshipDto request = new CreateInternshipDto();
		request.setTitle("Backend Engineering Intern");
		request.setDescription("Implement and test backend features.");
		request.setRequiredSkills("Java, Spring Boot");
		request.setEligibilityCriteria("Students or recent graduates.");
		request.setExperienceRequired(0);
		request.setFresherEligible(true);
		request.setDuration("3 months");
		request.setStipendType("PAID");
		request.setStipendAmount(new BigDecimal("1250.00"));
		request.setWorkMode("HYBRID");
		request.setLocation("Bengaluru");
		request.setApplicationDeadline(LocalDateTime.now().plusDays(5));
		return request;
	}

	private void authenticate(String authority) {
		authenticateAs(recruiter.getEmail(), authority);
	}

	private void authenticateAs(String email, String authority) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(
						email,
						"ignored",
						List.of(new SimpleGrantedAuthority(authority))));
	}
}
