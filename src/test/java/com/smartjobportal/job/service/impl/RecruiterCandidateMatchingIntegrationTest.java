package com.smartjobportal.job.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.smartjobportal.entity.CandidateProfile;
import com.smartjobportal.entity.CandidateSkill;
import com.smartjobportal.entity.Job;
import com.smartjobportal.entity.RecruiterProfile;
import com.smartjobportal.entity.Resume;
import com.smartjobportal.entity.User;
import com.smartjobportal.job.dto.RecruiterCandidateMatchDto;
import com.smartjobportal.job.service.JobMatchingService;
import com.smartjobportal.repository.CandidateProfileRepository;
import com.smartjobportal.repository.CandidateSkillRepository;
import com.smartjobportal.repository.JobRepository;
import com.smartjobportal.repository.RecruiterProfileRepository;
import com.smartjobportal.repository.ResumeRepository;
import com.smartjobportal.repository.UserRepository;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(JobMatchingServiceImpl.class)
class RecruiterCandidateMatchingIntegrationTest {

	@Autowired
	private JobMatchingService jobMatchingService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RecruiterProfileRepository recruiterProfileRepository;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private CandidateProfileRepository candidateProfileRepository;

	@Autowired
	private CandidateSkillRepository candidateSkillRepository;

	@Autowired
	private ResumeRepository resumeRepository;

	private Integer jobId;

	@BeforeEach
	void setUp() {
		User recruiterUser = new User();
		recruiterUser.setEmail("matching-recruiter@example.test");
		recruiterUser.setPassword("test-only");
		recruiterUser.setRole("RECRUITER");
		recruiterUser = userRepository.save(recruiterUser);

		RecruiterProfile recruiter = new RecruiterProfile();
		recruiter.setUser(recruiterUser);
		recruiter = recruiterProfileRepository.save(recruiter);

		Job job = new Job();
		job.setJobTitle("Backend Engineer");
		job.setRequiredSkills("Java, Spring, SQL");
		job.setRecruiterProfile(recruiter);
		jobId = jobRepository.save(job).getJobId();

		User completeUser = new User();
		completeUser.setEmail("complete-candidate@example.test");
		completeUser.setPassword("test-only");
		completeUser.setRole("CANDIDATE");
		completeUser.setFirstName("Complete");
		completeUser.setLastName("Candidate");
		CandidateProfile completeProfile = new CandidateProfile();
		completeProfile.setUser(userRepository.save(completeUser));
		completeProfile = candidateProfileRepository.save(completeProfile);

		CandidateSkill savedSkill = new CandidateSkill();
		savedSkill.setCandidateProfile(completeProfile);
		savedSkill.setSkillName("Java");
		candidateSkillRepository.save(savedSkill);

		Resume resume = new Resume();
		resume.setCandidateProfile(completeProfile);
		resume.setResumeTitle("Backend resume");
		resume.setIsActive(true);
		resume.setParsedSkills("spring");
		resumeRepository.save(resume);

		User partialUser = new User();
		partialUser.setEmail("partial-candidate@example.test");
		partialUser.setPassword("test-only");
		partialUser.setRole("CANDIDATE");
		partialUser.setFirstName("Partial");
		partialUser.setLastName("Candidate");
		CandidateProfile partialProfile = new CandidateProfile();
		partialProfile.setUser(userRepository.save(partialUser));
		candidateProfileRepository.save(partialProfile);

		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(
						"matching-recruiter@example.test",
						"test-only",
						List.of(new SimpleGrantedAuthority("ROLE_RECRUITER"))));
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void loadsSavedCandidateAndResumeSkillsFromRepositoriesAndRanksResults() {
		List<RecruiterCandidateMatchDto> results = jobMatchingService.getRecruiterCandidateMatches(jobId);

		assertEquals(2, results.size());
		assertEquals("Complete Candidate", results.get(0).getCandidateName());
		assertEquals(200.0 / 3.0, results.get(0).getMatchPercentage(), 0.0001);
		assertEquals(List.of("java", "spring"), results.get(0).getMatchedSkills());
		assertEquals(List.of("sql"), results.get(0).getMissingSkills());
		assertTrue(results.get(0).isHasActiveResume());

		assertEquals("Partial Candidate", results.get(1).getCandidateName());
		assertEquals(0.0, results.get(1).getMatchPercentage());
		assertEquals(List.of("java", "spring", "sql"), results.get(1).getMissingSkills());
		assertFalse(results.get(1).isHasActiveResume());
	}
}
