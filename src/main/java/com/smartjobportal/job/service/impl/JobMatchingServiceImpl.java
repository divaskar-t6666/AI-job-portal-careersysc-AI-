package com.smartjobportal.job.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartjobportal.entity.CandidateProfile;
import com.smartjobportal.entity.CandidateSkill;
import com.smartjobportal.entity.Job;
import com.smartjobportal.entity.JobApplication;
import com.smartjobportal.entity.JobMatch;
import com.smartjobportal.entity.RecruiterProfile;
import com.smartjobportal.entity.Resume;
import com.smartjobportal.entity.User;
import com.smartjobportal.exception.ResourceNotFoundException;
import com.smartjobportal.job.dto.MatchResultDto;
import com.smartjobportal.job.dto.RecruiterCandidateMatchDto;
import com.smartjobportal.job.dto.RecommendedJobDto;
import com.smartjobportal.job.service.JobMatchingService;
import com.smartjobportal.repository.JobApplicationRepository;
import com.smartjobportal.repository.CandidateProfileRepository;
import com.smartjobportal.repository.CandidateSkillRepository;
import com.smartjobportal.repository.JobMatchRepository;
import com.smartjobportal.repository.JobRepository;
import com.smartjobportal.repository.RecruiterProfileRepository;
import com.smartjobportal.repository.ResumeRepository;
import com.smartjobportal.repository.UserRepository;
import com.smartjobportal.matching.util.SkillMatchingUtil;

@Service
public class JobMatchingServiceImpl implements JobMatchingService {

	private static final double MIN_RECOMMENDATION_MATCH_PERCENTAGE = 20.0;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private ResumeRepository resumeRepository;

	@Autowired
	private JobMatchRepository jobMatchRepository;

	@Autowired
	private JobApplicationRepository jobApplicationRepository;

	@Autowired
	private CandidateProfileRepository candidateProfileRepository;

	@Autowired
	private CandidateSkillRepository candidateSkillRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RecruiterProfileRepository recruiterProfileRepository;

	@Override
	@Transactional(readOnly = true)
	public List<RecruiterCandidateMatchDto> getRecruiterCandidateMatches(Integer jobId) {

		Job job = jobRepository.findById(jobId)
				.orElseThrow(() -> new ResourceNotFoundException("Job not found with ID : " + jobId));
		assertRecruiterOwnsJob(job);

		List<CandidateProfile> candidates = candidateProfileRepository.findAll();
		if (candidates.isEmpty()) {
			return List.of();
		}

		List<Integer> candidateProfileIds = candidates.stream()
				.map(CandidateProfile::getCandidateProfileId)
				.filter(java.util.Objects::nonNull)
				.toList();

		Map<Integer, Resume> activeResumes = new LinkedHashMap<>();
		if (!candidateProfileIds.isEmpty()) {
			resumeRepository
					.findByCandidateProfileCandidateProfileIdInAndIsActiveTrueOrderByUpdatedAtDescResumeIdDesc(
							candidateProfileIds)
					.forEach(resume -> activeResumes.putIfAbsent(
							resume.getCandidateProfile().getCandidateProfileId(), resume));
		}

		Map<Integer, Set<String>> savedSkills = new LinkedHashMap<>();
		if (!candidateProfileIds.isEmpty()) {
			for (CandidateSkill skill : candidateSkillRepository
					.findByCandidateProfileCandidateProfileIdIn(candidateProfileIds)) {
				if (skill.getCandidateProfile() != null && skill.getCandidateProfile().getCandidateProfileId() != null) {
					savedSkills.computeIfAbsent(
							skill.getCandidateProfile().getCandidateProfileId(), ignored -> new LinkedHashSet<>())
							.addAll(SkillMatchingUtil.convertSkillsToSet(skill.getSkillName()));
				}
			}
		}

		Set<String> requiredSkills = SkillMatchingUtil.convertSkillsToSet(job.getRequiredSkills());
		List<RecruiterCandidateMatchDto> results = new ArrayList<>(candidates.size());

		for (CandidateProfile candidate : candidates) {
			Integer candidateId = candidate.getCandidateProfileId();
			Resume resume = candidateId == null ? null : activeResumes.get(candidateId);
			Set<String> candidateSkills = new LinkedHashSet<>(
					candidateId == null ? Set.of() : savedSkills.getOrDefault(candidateId, Set.of()));
			if (resume != null) {
				candidateSkills.addAll(SkillMatchingUtil.convertSkillsToSet(resume.getParsedSkills()));
			}

			List<String> matchedSkills = requiredSkills.stream()
					.filter(candidateSkills::contains)
					.toList();
			List<String> missingSkills = requiredSkills.stream()
					.filter(skill -> !candidateSkills.contains(skill))
					.toList();

			RecruiterCandidateMatchDto result = new RecruiterCandidateMatchDto();
			result.setCandidateName(candidate.getUser() == null ? "" : candidate.getUser().getFullName());
			result.setCurrentJobTitle(candidate.getCurrentJobTitle());
			result.setHasActiveResume(resume != null);
			result.setMatchedSkills(matchedSkills);
			result.setMissingSkills(missingSkills);
			result.setMatchPercentage(SkillMatchingUtil.calculatePercentage(matchedSkills.size(), requiredSkills.size()));
			results.add(result);
		}

		results.sort(Comparator.comparingDouble(RecruiterCandidateMatchDto::getMatchPercentage)
				.reversed()
				.thenComparing(RecruiterCandidateMatchDto::getCandidateName,
						Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
		return results;
	}

	@Override
	public MatchResultDto matchResumeWithJob(Integer jobId, Integer resumeId) {

		Job job = jobRepository.findById(jobId)
				.orElseThrow(() -> new ResourceNotFoundException("Job not found with ID : " + jobId));

		Resume resume = resumeRepository.findById(resumeId)
				.orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID : " + resumeId));

		assertCanMatchResume(job, resume);

		/*
		 * Return existing match if already calculated
		 */
		if (jobMatchRepository.existsByJobAndResume(job, resume)) {

			/*
			 * Delete existing cached match so that resume changes are reflected.
			 */

			JobMatch existingMatch = jobMatchRepository.findByJobAndResume(job, resume);

			if (existingMatch != null) {

				jobMatchRepository.delete(existingMatch);

			}
		}

		/*
		 * Safe handling for NULL required skills
		 */
		String requiredSkillsText = job.getRequiredSkills();

		if (requiredSkillsText == null || requiredSkillsText.isBlank()) {

			requiredSkillsText = "";
		}

		/*
		 * Safe handling for NULL parsed skills
		 */
		String parsedSkillsText = resume.getParsedSkills();

		if (parsedSkillsText == null || parsedSkillsText.isBlank()) {

			parsedSkillsText = "";
		}

		Set<String> jobSkills = SkillMatchingUtil.convertSkillsToSet(requiredSkillsText);

		Set<String> resumeSkills = SkillMatchingUtil.convertSkillsToSet(parsedSkillsText);

		List<String> matchedSkills = new ArrayList<>();

		List<String> missingSkills = new ArrayList<>();

		for (String skill : jobSkills) {

			if (resumeSkills.contains(skill)) {

				matchedSkills.add(skill);

			} else {

				missingSkills.add(skill);
			}
		}

		double matchPercentage = SkillMatchingUtil.calculatePercentage(matchedSkills.size(), jobSkills.size());

		JobMatch jobMatch = new JobMatch();

		jobMatch.setJob(job);

		jobMatch.setResume(resume);

		jobMatch.setMatchPercentage(matchPercentage);

		jobMatch.setMatchedSkills(String.join(", ", matchedSkills));

		jobMatch.setMissingSkills(String.join(", ", missingSkills));

		jobMatchRepository.save(jobMatch);

		MatchResultDto dto = new MatchResultDto();

		dto.setJobId(jobId);

		dto.setResumeId(resumeId);

		dto.setMatchPercentage(matchPercentage);

		dto.setMatchedSkills(String.join(", ", matchedSkills));

		dto.setMissingSkills(String.join(", ", missingSkills));

		return dto;
	}

	@Override
	public List<MatchResultDto> getJobMatches(Integer jobId) {

		Job job = jobRepository.findById(jobId)
				.orElseThrow(() -> new ResourceNotFoundException("Job not found with ID : " + jobId));

		assertRecruiterOwnsJob(job);

		List<JobMatch> matches = jobMatchRepository.findByJobOrderByMatchPercentageDesc(job);

		return matches.stream().map(match -> {

			MatchResultDto dto = new MatchResultDto();

			dto.setJobId(match.getJob().getJobId());

			dto.setResumeId(match.getResume().getResumeId());

			dto.setMatchPercentage(match.getMatchPercentage());

			dto.setMatchedSkills(match.getMatchedSkills());

			dto.setMissingSkills(match.getMissingSkills());

			return dto;

		}).collect(Collectors.toList());
	}

	@Override
	public List<RecommendedJobDto> getRecommendedJobs(Integer resumeId) {

		Resume resume = resumeRepository.findById(resumeId)
				.orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID : " + resumeId));

		assertLoggedInCandidateOwnsResume(resume);

		List<Job> jobs = jobRepository.findByJobStatus("OPEN");

		List<RecommendedJobDto> recommendations = new ArrayList<>();

		for (Job job : jobs) {

			/*
			 * Skip jobs with no configured skills
			 */
			if (job.getRequiredSkills() == null || job.getRequiredSkills().isBlank()) {

				continue;
			}

			MatchResultDto result = matchResumeWithJob(job.getJobId(), resumeId);

			if (result.getMatchPercentage() >= MIN_RECOMMENDATION_MATCH_PERCENTAGE) {

				RecommendedJobDto dto = new RecommendedJobDto();

				dto.setJobId(job.getJobId());

				dto.setJobTitle(job.getJobTitle());

				if (job.getCompany() != null) {

					dto.setCompanyName(job.getCompany().getCompanyName());
				}

				dto.setMatchPercentage(java.math.BigDecimal.valueOf(result.getMatchPercentage()));

				dto.setMatchedSkills(result.getMatchedSkills());

				dto.setMissingSkills(result.getMissingSkills());

				recommendations.add(dto);
			}
		}

		recommendations.sort(

				(a, b) -> b.getMatchPercentage().compareTo(a.getMatchPercentage()));

		return recommendations;
	}

	@Override
	public Page<RecommendedJobDto> getRecommendedJobs(Integer resumeId, Pageable pageable) {

		List<RecommendedJobDto> recommendations = getRecommendedJobs(resumeId);

		int start = (int) pageable.getOffset();

		int end = Math.min(start + pageable.getPageSize(), recommendations.size());

		List<RecommendedJobDto> pageContent = recommendations.subList(start, end);

		return new PageImpl<>(pageContent, pageable, recommendations.size());
	}

	@Override
	public List<MatchResultDto> getAppliedCandidateMatches(Integer jobId) {

		Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found"));

		assertRecruiterOwnsJob(job);

		List<JobApplication> applications = jobApplicationRepository.findByJob(job);

		List<MatchResultDto> results = new ArrayList<>();

		for (JobApplication application : applications) {

			Resume resume = application.getResume();

			MatchResultDto matchResult = matchResumeWithJob(jobId, resume.getResumeId());

			matchResult.setApplicationId(application.getApplicationId());

			matchResult.setCandidateName(application.getCandidateProfile().getUser().getFirstName() + " "
					+ application.getCandidateProfile().getUser().getLastName());

			matchResult.setCandidateEmail(application.getCandidateProfile().getUser().getEmail());

			matchResult.setResumeTitle(resume.getResumeTitle());

			matchResult.setFileName(resume.getFileName());

			matchResult.setFilePath(resume.getFilePath());

			results.add(matchResult);
		}

		results.sort((a, b) -> Double.compare(b.getMatchPercentage(), a.getMatchPercentage()));

		return results;
	}

	private void assertCanMatchResume(Job job, Resume resume) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AccessDeniedException("Authentication is required to match a resume");
		}

		boolean isCandidate = authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_CANDIDATE".equals(authority.getAuthority()));

		if (isCandidate) {
			assertLoggedInCandidateOwnsResume(resume);
			return;
		}

		assertRecruiterOwnsJob(job);

		if (!jobApplicationRepository.existsByJobAndResume(job, resume)) {
			throw new AccessDeniedException("You can only match resumes submitted to your job");
		}
	}

	private void assertLoggedInCandidateOwnsResume(Resume resume) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getAuthorities().stream()
						.noneMatch(authority -> "ROLE_CANDIDATE".equals(authority.getAuthority()))) {
			throw new AccessDeniedException("Candidate authentication is required");
		}

		User user = userRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new AccessDeniedException("Candidate account was not found"));

		boolean ownsResume = candidateProfileRepository.findByUserUserId(user.getUserId())
				.map(candidate -> candidate.getCandidateProfileId()
						.equals(resume.getCandidateProfile().getCandidateProfileId()))
				.orElse(false);

		if (!ownsResume) {
			throw new AccessDeniedException("You cannot access another candidate's resume");
		}
	}

	private void assertRecruiterOwnsJob(Job job) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getAuthorities().stream()
						.noneMatch(authority -> "ROLE_RECRUITER".equals(authority.getAuthority()))) {
			throw new AccessDeniedException("Recruiter authentication is required");
		}

		User user = userRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new AccessDeniedException("Recruiter account was not found"));

		RecruiterProfile recruiter = recruiterProfileRepository.findByUserUserId(user.getUserId())
				.orElseThrow(() -> new AccessDeniedException("Recruiter profile was not found"));

		if (job.getRecruiterProfile() == null
				|| !job.getRecruiterProfile().getRecruiterProfileId().equals(recruiter.getRecruiterProfileId())) {
			throw new AccessDeniedException("You can only view matches for your own job");
		}
	}
}