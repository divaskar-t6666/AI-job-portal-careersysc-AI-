package com.smartjobportal.job.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.smartjobportal.entity.CandidateProfile;
import com.smartjobportal.entity.Company;
import com.smartjobportal.entity.Opportunity;
import com.smartjobportal.entity.OpportunityType;
import com.smartjobportal.entity.RecruiterProfile;
import com.smartjobportal.entity.Resume;
import com.smartjobportal.entity.User;
import com.smartjobportal.job.dto.ApplyOpportunityDto;
import com.smartjobportal.repository.CandidateProfileRepository;
import com.smartjobportal.repository.CompanyRepository;
import com.smartjobportal.repository.OpportunityApplicationRepository;
import com.smartjobportal.repository.OpportunityRepository;
import com.smartjobportal.repository.RecruiterProfileRepository;
import com.smartjobportal.repository.ResumeRepository;
import com.smartjobportal.repository.UserRepository;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class OpportunityApplicationIntegrationTest {

	@Autowired private OpportunityApplicationRepository applicationRepository;
	@Autowired private OpportunityRepository opportunityRepository;
	@Autowired private CandidateProfileRepository candidateProfileRepository;
	@Autowired private CompanyRepository companyRepository;
	@Autowired private RecruiterProfileRepository recruiterProfileRepository;
	@Autowired private ResumeRepository resumeRepository;
	@Autowired private UserRepository userRepository;

	private OpportunityApplicationService applicationService;
	private CandidateProfile candidate;
	private Resume resume;
	private Opportunity opportunity;

	@BeforeEach
	void setUp() {
		applicationService = new OpportunityApplicationService(
				applicationRepository,
				opportunityRepository,
				candidateProfileRepository,
				resumeRepository,
				userRepository);

		User candidateUser = new User();
		candidateUser.setEmail("candidate@example.test");
		candidateUser.setPassword("not-a-real-password");
		candidateUser.setRole("CANDIDATE");
		candidateUser = userRepository.save(candidateUser);
		candidate = new CandidateProfile();
		candidate.setUser(candidateUser);
		candidate = candidateProfileRepository.save(candidate);

		resume = new Resume();
		resume.setCandidateProfile(candidate);
		resume.setResumeTitle("Candidate Resume");
		resume.setFileName("candidate.pdf");
		resume.setFilePath("test/candidate.pdf");
		resume.setFileType("application/pdf");
		resume = resumeRepository.save(resume);

		Company company = new Company();
		company.setCompanyName("Test Company");
		company = companyRepository.save(company);
		User recruiterUser = new User();
		recruiterUser.setEmail("recruiter@example.test");
		recruiterUser.setPassword("not-a-real-password");
		recruiterUser.setRole("RECRUITER");
		recruiterUser = userRepository.save(recruiterUser);
		RecruiterProfile recruiter = new RecruiterProfile();
		recruiter.setUser(recruiterUser);
		recruiter.setCompany(company);
		recruiter = recruiterProfileRepository.save(recruiter);

		opportunity = new Opportunity();
		opportunity.setOpportunityType(OpportunityType.INTERNSHIP);
		opportunity.setTitle("Java Internship");
		opportunity.setDescription("Build Java applications.");
		opportunity.setRequiredSkills("Java");
		opportunity.setEligibilityCriteria("Open to candidates.");
		opportunity.setDuration("3 months");
		opportunity.setWorkMode("REMOTE");
		opportunity.setLocation("Online");
		opportunity.setApplicationDeadline(LocalDateTime.now().plusDays(3));
		opportunity.setStatus("OPEN");
		opportunity.setCompany(company);
		opportunity.setRecruiterProfile(recruiter);
		opportunity = opportunityRepository.save(opportunity);

		authenticate("ROLE_CANDIDATE", candidateUser.getEmail());
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void appliesOnceUsingOwnedResumeAndPersistsStatus() {
		ApplyOpportunityDto request = new ApplyOpportunityDto();
		request.setResumeId(resume.getResumeId());

		var response = applicationService.apply(opportunity.getOpportunityId(), request);
		assertEquals(opportunity.getOpportunityId(), response.getOpportunityId());
		assertEquals("APPLIED", response.getApplicationStatus().name());
		assertEquals(1, applicationRepository.count());

		ResponseStatusException duplicate = assertThrows(ResponseStatusException.class,
				() -> applicationService.apply(opportunity.getOpportunityId(), request));
		assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());
	}

	@Test
	void rejectsOtherUsersAndExpiredOpportunities() {
		Resume otherResume = new Resume();
		otherResume.setResumeTitle("Other Resume");
		otherResume.setFileName("other.pdf");
		otherResume.setFilePath("test/other.pdf");
		otherResume.setFileType("application/pdf");
		otherResume = resumeRepository.save(otherResume);
		ApplyOpportunityDto request = new ApplyOpportunityDto();
		request.setResumeId(otherResume.getResumeId());
		assertThrows(AccessDeniedException.class,
				() -> applicationService.apply(opportunity.getOpportunityId(), request));
		assertEquals(0, applicationRepository.count());

		opportunity.setApplicationDeadline(LocalDateTime.now().minusDays(1));
		opportunityRepository.save(opportunity);
		request.setResumeId(resume.getResumeId());
		ResponseStatusException expired = assertThrows(ResponseStatusException.class,
				() -> applicationService.apply(opportunity.getOpportunityId(), request));
		assertEquals(HttpStatus.CONFLICT, expired.getStatusCode());
		assertEquals(0, applicationRepository.count());
	}

	private void authenticate(String authority, String email) {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(
						email,
						"ignored",
						List.of(new SimpleGrantedAuthority(authority))));
	}
}
