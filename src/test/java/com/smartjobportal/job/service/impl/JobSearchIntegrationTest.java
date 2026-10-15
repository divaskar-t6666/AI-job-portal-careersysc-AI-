package com.smartjobportal.job.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import com.smartjobportal.entity.Company;
import com.smartjobportal.entity.Job;
import com.smartjobportal.job.dto.JobPageResponseDto;
import com.smartjobportal.job.dto.JobSearchDto;
import com.smartjobportal.repository.CompanyRepository;
import com.smartjobportal.repository.JobRepository;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class JobSearchIntegrationTest {

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private CompanyRepository companyRepository;

	private JobServiceImpl jobService;

	@BeforeEach
	void setUp() {
		jobService = new JobServiceImpl();
		ReflectionTestUtils.setField(jobService, "jobRepository", jobRepository);

		Company infosys = saveCompany("Infosys");
		Company zoho = saveCompany("Zoho");
		Company acme = saveCompany("Acme");

		saveJob(infosys, "Java Engineer", "Build backend systems", "Java, Spring", "Hyderabad", "FULL_TIME", 4,
				"OPEN");
		saveJob(infosys, "Data Analyst", "Analyze business datasets", "Python, SQL", "Bengaluru", "FULL_TIME", 2,
				"OPEN");
		saveJob(zoho, "Frontend Engineer", "Create web interfaces", "HTML, CSS", "Chennai", "PART_TIME", 1, "OPEN");
		saveJob(zoho, "QA Analyst", "Automate test workflows", "Python, SQL", "Pune", "FULL_TIME", 3, "OPEN");
		saveJob(acme, "Graphic Designer", "Design visual assets", "Photoshop", "Pune", "INTERNSHIP", 0, "CLOSED");
		saveJob(acme, "Python Developer", "Create data tools", "Django", "Hyderabad", "FULL_TIME", 2, "OPEN");
		saveJob(acme, "Support Associate", "Python scripting is helpful", "Customer Support", "Pune", "FULL_TIME",
				1, "OPEN");
	}

	@Test
	void searchesCompanyNamesAndRequiredSkillsCaseInsensitively() {
		JobPageResponseDto companyResults = search("  InFoSyS  ");
		JobPageResponseDto pythonResults = search("python");
		JobPageResponseDto htmlResults = search(" HTML ");
		JobPageResponseDto titleResults = search("  DeSiGnEr ");

		assertEquals(2, companyResults.getTotalElements());
		assertEquals(List.of("Infosys", "Infosys"),
				companyResults.getContent().stream().map(job -> job.getCompanyName()).toList());
		assertEquals(4, pythonResults.getTotalElements());
		assertEquals(List.of("Data Analyst", "Python Developer", "QA Analyst", "Support Associate"),
				pythonResults.getContent().stream().map(job -> job.getJobTitle()).sorted().toList());
		assertEquals(1, htmlResults.getTotalElements());
		assertEquals("Zoho", htmlResults.getContent().get(0).getCompanyName());
		assertEquals(1, titleResults.getTotalElements());
		assertEquals("Acme", titleResults.getContent().get(0).getCompanyName());
	}

	@Test
	void everyKeywordTermMustMatchAndOtherFiltersRemainCombined() {
		JobSearchDto filters = new JobSearchDto();
		filters.setKeyword("  Python   Infosys ");
		filters.setLocation(" bengaluru ");
		filters.setSkill("Python");
		filters.setCompanyName("INFOSYS");
		filters.setEmploymentType("full_time");
		filters.setExperienceRequired(3);
		filters.setJobStatus("open");

		JobPageResponseDto results = search(filters);

		assertEquals(1, results.getTotalElements());
		assertEquals("Data Analyst", results.getContent().get(0).getJobTitle());
		assertEquals("Infosys", results.getContent().get(0).getCompanyName());

		JobPageResponseDto keywordOnlyResults = search("Python Infosys");
		assertEquals(1, keywordOnlyResults.getTotalElements());
		assertEquals("Data Analyst", keywordOnlyResults.getContent().get(0).getJobTitle());
	}

	@Test
	void blankKeywordRestoresAllJobs() {
		JobPageResponseDto results = jobService.searchJobsWithPagination(new JobSearchDto(), 0, 6, "jobTitle", "asc");

		assertEquals(7, results.getTotalElements());
		assertEquals(6, results.getContent().size());
	}

	private JobPageResponseDto search(String keyword) {
		JobSearchDto filters = new JobSearchDto();
		filters.setKeyword(keyword);
		return search(filters);
	}

	private JobPageResponseDto search(JobSearchDto filters) {
		return jobService.searchJobsWithPagination(filters, 0, 20, "jobTitle", "asc");
	}

	private Company saveCompany(String name) {
		Company company = new Company();
		company.setCompanyName(name);
		return companyRepository.save(company);
	}

	private void saveJob(Company company, String title, String description, String skills, String location,
			String employmentType, int experience, String status) {
		Job job = new Job();
		job.setCompany(company);
		job.setJobTitle(title);
		job.setJobDescription(description);
		job.setRequiredSkills(skills);
		job.setJobLocation(location);
		job.setEmploymentType(employmentType);
		job.setExperienceRequired(experience);
		job.setJobStatus(status);
		jobRepository.save(job);
	}
}
