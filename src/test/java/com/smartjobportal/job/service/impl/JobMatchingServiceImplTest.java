package com.smartjobportal.job.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.smartjobportal.entity.CandidateProfile;
import com.smartjobportal.entity.CandidateSkill;
import com.smartjobportal.entity.Job;
import com.smartjobportal.entity.JobApplication;
import com.smartjobportal.entity.JobMatch;
import com.smartjobportal.entity.RecruiterProfile;
import com.smartjobportal.entity.Resume;
import com.smartjobportal.entity.User;
import com.smartjobportal.repository.CandidateProfileRepository;
import com.smartjobportal.repository.CandidateSkillRepository;
import com.smartjobportal.repository.JobApplicationRepository;
import com.smartjobportal.repository.JobMatchRepository;
import com.smartjobportal.repository.JobRepository;
import com.smartjobportal.repository.RecruiterProfileRepository;
import com.smartjobportal.repository.ResumeRepository;
import com.smartjobportal.repository.UserRepository;

import org.mockito.Mockito;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class JobMatchingServiceImplTest {

	private final JobRepository jobRepository = Mockito.mock(JobRepository.class);
	private final ResumeRepository resumeRepository = Mockito.mock(ResumeRepository.class);
	private final JobMatchRepository jobMatchRepository = Mockito.mock(JobMatchRepository.class);
	private final JobApplicationRepository jobApplicationRepository = Mockito.mock(JobApplicationRepository.class);
	private final CandidateProfileRepository candidateProfileRepository = Mockito
			.mock(CandidateProfileRepository.class);
	private final CandidateSkillRepository candidateSkillRepository = Mockito
			.mock(CandidateSkillRepository.class);
	private final UserRepository userRepository = Mockito.mock(UserRepository.class);
	private final RecruiterProfileRepository recruiterProfileRepository = Mockito
			.mock(RecruiterProfileRepository.class);
	private final JobMatchingServiceImpl service = new JobMatchingServiceImpl();

	private final User user = new User();
	private final CandidateProfile candidateProfile = new CandidateProfile();
	private final Job job = new Job();
	private final Resume resume = new Resume();

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(service, "jobRepository", jobRepository);
		ReflectionTestUtils.setField(service, "resumeRepository", resumeRepository);
		ReflectionTestUtils.setField(service, "jobMatchRepository", jobMatchRepository);
		ReflectionTestUtils.setField(service, "jobApplicationRepository", jobApplicationRepository);
		ReflectionTestUtils.setField(service, "candidateProfileRepository", candidateProfileRepository);
		ReflectionTestUtils.setField(service, "candidateSkillRepository", candidateSkillRepository);
		ReflectionTestUtils.setField(service, "userRepository", userRepository);
		ReflectionTestUtils.setField(service, "recruiterProfileRepository", recruiterProfileRepository);

		user.setUserId(7);
		user.setEmail("candidate@example.com");
		candidateProfile.setCandidateProfileId(11);
		job.setJobId(31);
		job.setRequiredSkills("Java, java, Spring Boot, SQL");
		resume.setResumeId(42);
		resume.setCandidateProfile(candidateProfile);
		resume.setParsedSkills("JAVA, sql, java");

		when(jobRepository.findById(31)).thenReturn(Optional.of(job));
		when(resumeRepository.findById(42)).thenReturn(Optional.of(resume));
		when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(user));
		when(candidateProfileRepository.findByUserUserId(7)).thenReturn(Optional.of(candidateProfile));

		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(
						"candidate@example.com",
						"token",
						java.util.List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"))));
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void matchingUsesUniqueCaseInsensitiveRequiredAndResumeSkills() {
		when(jobMatchRepository.existsByJobAndResume(job, resume)).thenReturn(false);

		var result = service.matchResumeWithJob(31, 42);

		assertEquals(31, result.getJobId());
		assertEquals(42, result.getResumeId());
		assertEquals(100.0 / 3.0 * 2.0, result.getMatchPercentage(), 0.0001);
		assertEquals("java, sql", result.getMatchedSkills());
		assertEquals("spring boot", result.getMissingSkills());
		verify(jobMatchRepository).save(any(JobMatch.class));
	}

	@Test
	void recommendsPartiallyMatchedJobSoCandidateCanViewTheRealScore() {
		job.setRequiredSkills(
				"Python, Java, SQL, MySQL, HTML, CSS, JavaScript, Flask, Spring Boot, REST API, Git, Problem Solving");
		job.setJobTitle("Python Developer");
		resume.setParsedSkills("JavaScript, HTML, CSS");
		when(jobRepository.findByJobStatus("OPEN")).thenReturn(java.util.List.of(job));
		when(jobMatchRepository.existsByJobAndResume(job, resume)).thenReturn(false);

		var results = service.getRecommendedJobs(42);

		assertEquals(1, results.size());
		assertEquals(31, results.get(0).getJobId());
		assertEquals(25.0, results.get(0).getMatchPercentage().doubleValue(), 0.0001);
		assertEquals("html, css, javascript", results.get(0).getMatchedSkills());
	}

	@Test
	void candidateCannotMatchAnotherCandidatesResume() {
		CandidateProfile differentCandidate = new CandidateProfile();
		differentCandidate.setCandidateProfileId(99);
		when(candidateProfileRepository.findByUserUserId(7)).thenReturn(Optional.of(differentCandidate));

		assertThrows(AccessDeniedException.class, () -> service.matchResumeWithJob(31, 42));
	}

	@Test
	void recruiterCanStillViewApplicantsForTheirOwnJob() {
		User recruiterUser = new User();
		recruiterUser.setUserId(8);
		recruiterUser.setEmail("recruiter@example.com");
		RecruiterProfile recruiter = new RecruiterProfile();
		recruiter.setRecruiterProfileId(15);
		job.setRecruiterProfile(recruiter);

		CandidateProfile applicant = new CandidateProfile();
		User applicantUser = new User();
		applicantUser.setFirstName("Candidate");
		applicantUser.setLastName("One");
		applicantUser.setEmail("applicant@example.com");
		applicant.setUser(applicantUser);
		resume.setResumeTitle("Resume");
		resume.setFileName("resume.pdf");
		resume.setFilePath("uploads/resume.pdf");

		JobApplication application = new JobApplication();
		application.setApplicationId(5);
		application.setCandidateProfile(applicant);
		application.setResume(resume);

		when(jobRepository.findById(31)).thenReturn(Optional.of(job));
		when(jobApplicationRepository.findByJob(job)).thenReturn(java.util.List.of(application));
		when(jobApplicationRepository.existsByJobAndResume(job, resume)).thenReturn(true);
		when(userRepository.findByEmail("recruiter@example.com")).thenReturn(Optional.of(recruiterUser));
		when(recruiterProfileRepository.findByUserUserId(8)).thenReturn(Optional.of(recruiter));
		when(jobMatchRepository.existsByJobAndResume(job, resume)).thenReturn(false);
		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(
						"recruiter@example.com",
						"token",
						java.util.List.of(new SimpleGrantedAuthority("ROLE_RECRUITER"))));

		var results = service.getAppliedCandidateMatches(31);

		assertEquals(1, results.size());
		assertEquals(5, results.get(0).getApplicationId());
		assertEquals("applicant@example.com", results.get(0).getCandidateEmail());
		assertEquals(31, results.get(0).getJobId());
	}

	@Test
	void recruiterMatchesAllCandidatesBySavedAndResumeSkillsAndRanksByScore() {
		User recruiterUser = new User();
		recruiterUser.setUserId(8);
		recruiterUser.setEmail("recruiter@example.com");
		RecruiterProfile recruiter = new RecruiterProfile();
		recruiter.setRecruiterProfileId(15);
		job.setRecruiterProfile(recruiter);
		job.setRequiredSkills("Java, Spring, SQL");

		CandidateProfile strongest = candidateProfile(11, "Ava", "Developer");
		CandidateProfile partial = candidateProfile(12, "Ben", "Analyst");
		CandidateProfile noSkills = candidateProfile(13, "Cleo", null);
		Resume strongestResume = new Resume();
		strongestResume.setResumeId(50);
		strongestResume.setCandidateProfile(strongest);
		strongestResume.setIsActive(true);
		strongestResume.setParsedSkills("spring, SQL");

		CandidateSkill savedSkill = new CandidateSkill();
		savedSkill.setCandidateProfile(strongest);
		savedSkill.setSkillName("JAVA");

		Resume partialResume = new Resume();
		partialResume.setResumeId(51);
		partialResume.setCandidateProfile(partial);
		partialResume.setIsActive(true);
		partialResume.setParsedSkills("Java");

		when(jobRepository.findById(31)).thenReturn(Optional.of(job));
		when(candidateProfileRepository.findAll()).thenReturn(java.util.List.of(noSkills, partial, strongest));
		when(resumeRepository
				.findByCandidateProfileCandidateProfileIdInAndIsActiveTrueOrderByUpdatedAtDescResumeIdDesc(
						any()))
				.thenReturn(java.util.List.of(strongestResume, partialResume));
		when(candidateSkillRepository.findByCandidateProfileCandidateProfileIdIn(any()))
				.thenReturn(java.util.List.of(savedSkill));
		when(userRepository.findByEmail("recruiter@example.com")).thenReturn(Optional.of(recruiterUser));
		when(recruiterProfileRepository.findByUserUserId(8)).thenReturn(Optional.of(recruiter));
		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(
						"recruiter@example.com",
						"token",
						java.util.List.of(new SimpleGrantedAuthority("ROLE_RECRUITER"))));

		var results = service.getRecruiterCandidateMatches(31);

		assertEquals(3, results.size());
		assertEquals("Ava", results.get(0).getCandidateName());
		assertEquals(100.0, results.get(0).getMatchPercentage());
		assertEquals(java.util.List.of("java", "spring", "sql"), results.get(0).getMatchedSkills());
		assertEquals(java.util.List.of(), results.get(0).getMissingSkills());
		assertEquals("Ben", results.get(1).getCandidateName());
		assertEquals(100.0 / 3.0, results.get(1).getMatchPercentage(), 0.0001);
		assertEquals(java.util.List.of("spring", "sql"), results.get(1).getMissingSkills());
		assertEquals("Cleo", results.get(2).getCandidateName());
		assertEquals(0.0, results.get(2).getMatchPercentage());
		assertEquals(java.util.List.of("java", "spring", "sql"), results.get(2).getMissingSkills());
		assertFalse(results.get(2).isHasActiveResume());
	}

	@Test
	void recruiterCannotMatchCandidatesAgainstAnotherRecruitersJob() {
		User recruiterUser = new User();
		recruiterUser.setUserId(8);
		recruiterUser.setEmail("recruiter@example.com");
		RecruiterProfile anotherRecruiter = new RecruiterProfile();
		anotherRecruiter.setRecruiterProfileId(99);
		job.setRecruiterProfile(anotherRecruiter);
		when(jobRepository.findById(31)).thenReturn(Optional.of(job));
		when(userRepository.findByEmail("recruiter@example.com")).thenReturn(Optional.of(recruiterUser));
		RecruiterProfile loggedInRecruiter = new RecruiterProfile();
		loggedInRecruiter.setRecruiterProfileId(15);
		when(recruiterProfileRepository.findByUserUserId(8)).thenReturn(Optional.of(loggedInRecruiter));
		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(
						"recruiter@example.com",
						"token",
						java.util.List.of(new SimpleGrantedAuthority("ROLE_RECRUITER"))));

		assertThrows(AccessDeniedException.class, () -> service.getRecruiterCandidateMatches(31));
	}

	@Test
	void candidateCannotUseRecruiterMatchingService() {
		job.setRecruiterProfile(new RecruiterProfile());
		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(
						"candidate@example.com",
						"token",
						java.util.List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"))));

		assertThrows(AccessDeniedException.class, () -> service.getRecruiterCandidateMatches(31));
	}

	@Test
	void unknownJobIdFailsRatherThanReturningAnEmptyMatchList() {
		when(jobRepository.findById(404)).thenReturn(Optional.empty());

		assertThrows(com.smartjobportal.exception.ResourceNotFoundException.class,
				() -> service.getRecruiterCandidateMatches(404));
	}

	@Test
	void emptyRequiredAndCandidateSkillListsProduceAZeroScoreWithoutFailing() {
		User recruiterUser = new User();
		recruiterUser.setUserId(8);
		recruiterUser.setEmail("recruiter@example.com");
		RecruiterProfile recruiter = new RecruiterProfile();
		recruiter.setRecruiterProfileId(15);
		job.setRecruiterProfile(recruiter);
		job.setRequiredSkills(" , ");
		CandidateProfile candidate = candidateProfile(11, "Ava", null);
		when(jobRepository.findById(31)).thenReturn(Optional.of(job));
		when(candidateProfileRepository.findAll()).thenReturn(java.util.List.of(candidate));
		when(resumeRepository
				.findByCandidateProfileCandidateProfileIdInAndIsActiveTrueOrderByUpdatedAtDescResumeIdDesc(any()))
				.thenReturn(java.util.List.of());
		when(candidateSkillRepository.findByCandidateProfileCandidateProfileIdIn(any()))
				.thenReturn(java.util.List.of());
		when(userRepository.findByEmail("recruiter@example.com")).thenReturn(Optional.of(recruiterUser));
		when(recruiterProfileRepository.findByUserUserId(8)).thenReturn(Optional.of(recruiter));
		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(
						"recruiter@example.com",
						"token",
						java.util.List.of(new SimpleGrantedAuthority("ROLE_RECRUITER"))));

		var results = service.getRecruiterCandidateMatches(31);

		assertEquals(1, results.size());
		assertEquals(0.0, results.get(0).getMatchPercentage());
		assertEquals(java.util.List.of(), results.get(0).getMatchedSkills());
		assertEquals(java.util.List.of(), results.get(0).getMissingSkills());
	}

	private CandidateProfile candidateProfile(Integer id, String firstName, String currentTitle) {
		CandidateProfile profile = new CandidateProfile();
		profile.setCandidateProfileId(id);
		profile.setCurrentJobTitle(currentTitle);
		User candidate = new User();
		candidate.setFirstName(firstName);
		candidate.setLastName("");
		profile.setUser(candidate);
		return profile;
	}
}
