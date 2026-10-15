package com.smartjobportal.job.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import com.smartjobportal.entity.Company;
import com.smartjobportal.entity.Job;
import com.smartjobportal.entity.Opportunity;
import com.smartjobportal.entity.OpportunityType;
import com.smartjobportal.entity.RecruiterProfile;
import com.smartjobportal.entity.User;
import com.smartjobportal.job.dto.OpportunityListingDto;
import com.smartjobportal.job.dto.OpportunityPageResponseDto;
import com.smartjobportal.job.dto.OpportunitySearchDto;
import com.smartjobportal.job.service.impl.JobServiceImpl;
import com.smartjobportal.repository.CompanyRepository;
import com.smartjobportal.repository.JobRepository;
import com.smartjobportal.repository.OpportunityRepository;
import com.smartjobportal.repository.RecruiterProfileRepository;
import com.smartjobportal.repository.UserRepository;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class OpportunityDiscoveryIntegrationTest {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private OpportunityRepository opportunityRepository;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RecruiterProfileRepository recruiterProfileRepository;

	private OpportunityDiscoveryService discoveryService;
	private Company company;

	@BeforeEach
	void setUp() {
		JobServiceImpl jobService = new JobServiceImpl();
		ReflectionTestUtils.setField(jobService, "jobRepository", jobRepository);
		OpportunityService opportunityService = new OpportunityService(
				opportunityRepository, userRepository, recruiterProfileRepository);
		discoveryService = new OpportunityDiscoveryService(jobService, opportunityRepository, opportunityService);

		company = new Company();
		company.setCompanyName("Zoho");
		company = companyRepository.save(company);

		User recruiterUser = new User();
		recruiterUser.setEmail("discovery-recruiter@example.test");
		recruiterUser.setPassword("not-a-real-password");
		recruiterUser.setRole("RECRUITER");
		recruiterUser = userRepository.save(recruiterUser);
		RecruiterProfile recruiter = new RecruiterProfile();
		recruiter.setUser(recruiterUser);
		recruiter.setCompany(company);
		recruiter = recruiterProfileRepository.save(recruiter);

		saveJob("Java Engineer", "Build Java services", "Java, Spring");
		saveOpportunity(recruiter, OpportunityType.INTERNSHIP, "Backend Intern", "Work with Java systems",
				"Java, Spring Boot", "OPEN", LocalDateTime.now().plusDays(5));
		saveOpportunity(recruiter, OpportunityType.HACKATHON, "Cloud Hackathon", "Build cloud solutions",
				"Java, AWS", "PUBLISHED", LocalDateTime.now().plusDays(8));
		saveOpportunity(recruiter, OpportunityType.INTERNSHIP, "Expired Internship", "Expired role",
				"Java", "OPEN", LocalDateTime.now().minusDays(1));
		saveOpportunity(recruiter, OpportunityType.HACKATHON, "Closed Hackathon", "Closed event",
				"Java", "CLOSED", LocalDateTime.now().plusDays(8));
	}

	@Test
	void searchesJobsInternshipsAndHackathonsAcrossRelevantFields() {
		assertEquals(3, search(null, "java").getTotalElements());
		assertEquals(3, search(null, "ZOHO").getTotalElements());
		assertEquals(1, search(null, "Internship").getTotalElements());
		assertEquals(1, search(null, "Hackathon").getTotalElements());
		assertEquals(1, search("INTERNSHIP", null).getTotalElements());
		assertEquals(1, search("HACKATHON", null).getTotalElements());
		assertEquals(1, search(null, "hackathon java").getTotalElements());
	}

	@Test
	void returnsTypedIdsAndCorrectPageCounts() {
		OpportunityPageResponseDto firstPage =
				discoveryService.search(new OpportunitySearchDto(), 0, 2, "createdAt", "desc");

		assertEquals(3, firstPage.getTotalElements());
		assertEquals(2, firstPage.getContent().size());
		assertEquals(2, firstPage.getTotalPages());
		assertEquals(2, firstPage.getContent().stream()
				.filter(item -> item.getId() != null && item.getId() > 0)
				.count());

		List<String> types = discoveryService
				.search(new OpportunitySearchDto(), 0, 20, "createdAt", "desc")
				.getContent()
				.stream()
				.map(OpportunityListingDto::getOpportunityType)
				.toList();
		assertEquals(3, types.size());
		assertEquals(1, types.stream().filter("JOB"::equals).count());
	}

	@Test
	void retrievesTheDatabaseRecordByTypeAndIdForDetails() {
		OpportunityListingDto internship = search("INTERNSHIP", null).getContent().get(0);
		OpportunityListingDto internshipDetails =
				discoveryService.getDetails("INTERNSHIP", internship.getId());
		assertEquals("Backend Intern", internshipDetails.getTitle());
		assertEquals("Zoho", internshipDetails.getCompanyName());
		assertEquals("Java, Spring Boot", internshipDetails.getRequiredSkills());
		assertEquals("Work with Java systems", internshipDetails.getDescription());

		OpportunityListingDto job = search("JOB", null).getContent().get(0);
		OpportunityListingDto jobDetails = discoveryService.getDetails("JOB", job.getId());
		assertEquals("Java Engineer", jobDetails.getTitle());
		assertEquals("Build Java services", jobDetails.getDescription());
	}

	private OpportunityPageResponseDto search(String type, String keyword) {
		OpportunitySearchDto filters = new OpportunitySearchDto();
		filters.setOpportunityType(type);
		filters.setKeyword(keyword);
		return discoveryService.search(filters, 0, 20, "createdAt", "desc");
	}

	private void saveJob(String title, String description, String skills) {
		Job job = new Job();
		job.setJobTitle(title);
		job.setJobDescription(description);
		job.setRequiredSkills(skills);
		job.setJobLocation("Chennai");
		job.setEmploymentType("FULL_TIME");
		job.setExperienceRequired(2);
		job.setJobStatus("OPEN");
		job.setApplicationDeadline(LocalDateTime.now().plusDays(10));
		job.setCompany(company);
		jobRepository.save(job);
	}

	private void saveOpportunity(RecruiterProfile recruiter, OpportunityType type, String title,
			String description, String skills, String status, LocalDateTime deadline) {
		Opportunity opportunity = new Opportunity();
		opportunity.setOpportunityType(type);
		opportunity.setTitle(title);
		opportunity.setDescription(description);
		opportunity.setRequiredSkills(skills);
		opportunity.setEligibilityCriteria("Open to applicants.");
		opportunity.setDuration("3 months");
		opportunity.setWorkMode("REMOTE");
		opportunity.setLocation("Online");
		opportunity.setApplicationDeadline(deadline);
		opportunity.setStatus(status);
		opportunity.setCompany(company);
		opportunity.setRecruiterProfile(recruiter);
		opportunityRepository.save(opportunity);
	}
}
