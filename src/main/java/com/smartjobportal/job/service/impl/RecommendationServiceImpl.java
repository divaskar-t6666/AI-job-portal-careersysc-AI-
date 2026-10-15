package com.smartjobportal.job.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.smartjobportal.entity.CandidateProfile;
import com.smartjobportal.entity.Resume;
import com.smartjobportal.entity.User;
import com.smartjobportal.job.dto.RecommendedJobDto;
import com.smartjobportal.job.service.JobMatchingService;
import com.smartjobportal.job.service.RecommendationService;
import com.smartjobportal.repository.CandidateProfileRepository;
import com.smartjobportal.repository.ResumeRepository;
import com.smartjobportal.repository.UserRepository;

@Service
public class RecommendationServiceImpl implements RecommendationService {

	@Autowired
	private ResumeRepository resumeRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CandidateProfileRepository candidateProfileRepository;

	@Autowired
	private JobMatchingService jobMatchingService;

	@Override
	public List<RecommendedJobDto> getRecommendations() {

		CandidateProfile candidateProfile = getLoggedInCandidate();

		List<Resume> activeResumes = resumeRepository.findByCandidateProfileAndIsActive(candidateProfile, true);

		if (activeResumes.isEmpty()) {

			throw new RuntimeException("No active resume found");
		}

		Resume activeResume = activeResumes.get(0);

		return jobMatchingService.getRecommendedJobs(activeResume.getResumeId());
	}

	private CandidateProfile getLoggedInCandidate() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getAuthorities().stream()
						.noneMatch(authority -> "ROLE_CANDIDATE".equals(authority.getAuthority()))) {
			throw new AccessDeniedException("Candidate authentication is required");
		}

		String email = authentication.getName();

		User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

		return candidateProfileRepository.findByUserUserId(user.getUserId())
				.orElseThrow(() -> new RuntimeException("Candidate profile not found"));
	}
}