package com.smartjobportal.job.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.smartjobportal.entity.ApplicationStatus;
import com.smartjobportal.entity.CandidateProfile;
import com.smartjobportal.entity.Opportunity;
import com.smartjobportal.entity.OpportunityApplication;
import com.smartjobportal.entity.Resume;
import com.smartjobportal.entity.User;
import com.smartjobportal.exception.ResourceNotFoundException;
import com.smartjobportal.job.dto.ApplyOpportunityDto;
import com.smartjobportal.job.dto.OpportunityApplicationResponseDto;
import com.smartjobportal.repository.CandidateProfileRepository;
import com.smartjobportal.repository.OpportunityApplicationRepository;
import com.smartjobportal.repository.OpportunityRepository;
import com.smartjobportal.repository.ResumeRepository;
import com.smartjobportal.repository.UserRepository;

@Service
public class OpportunityApplicationService {

	private final OpportunityApplicationRepository applicationRepository;
	private final OpportunityRepository opportunityRepository;
	private final CandidateProfileRepository candidateProfileRepository;
	private final ResumeRepository resumeRepository;
	private final UserRepository userRepository;

	public OpportunityApplicationService(
			OpportunityApplicationRepository applicationRepository,
			OpportunityRepository opportunityRepository,
			CandidateProfileRepository candidateProfileRepository,
			ResumeRepository resumeRepository,
			UserRepository userRepository) {
		this.applicationRepository = applicationRepository;
		this.opportunityRepository = opportunityRepository;
		this.candidateProfileRepository = candidateProfileRepository;
		this.resumeRepository = resumeRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public OpportunityApplicationResponseDto apply(Integer opportunityId, ApplyOpportunityDto request) {
		CandidateProfile candidate = currentCandidate();
		Opportunity opportunity = opportunityRepository.findById(opportunityId)
				.orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
		if (!isOpen(opportunity)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This opportunity is closed or past its deadline");
		}
		if (applicationRepository.existsByCandidateProfileAndOpportunity(candidate, opportunity)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "You already applied to this opportunity");
		}
		Resume resume = resumeRepository.findById(request.getResumeId())
				.orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
		if (resume.getCandidateProfile() == null
				|| !resume.getCandidateProfile().getCandidateProfileId().equals(candidate.getCandidateProfileId())) {
			throw new AccessDeniedException("You cannot use another candidate's resume");
		}

		OpportunityApplication application = new OpportunityApplication();
		application.setCandidateProfile(candidate);
		application.setOpportunity(opportunity);
		application.setResume(resume);
		application.setCoverLetter(request.getCoverLetter());
		application.setApplicationStatus(ApplicationStatus.APPLIED);
		return toResponse(applicationRepository.save(application));
	}

	@Transactional(readOnly = true)
	public List<OpportunityApplicationResponseDto> getMyApplications() {
		CandidateProfile candidate = currentCandidate();
		return applicationRepository.findByCandidateProfileOrderByAppliedAtDesc(candidate)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public OpportunityApplicationResponseDto getMyApplication(Integer opportunityId) {
		CandidateProfile candidate = currentCandidate();
		Opportunity opportunity = opportunityRepository.findById(opportunityId)
				.orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
		return applicationRepository.findByCandidateProfileAndOpportunity(candidate, opportunity)
				.map(this::toResponse)
				.orElse(null);
	}

	private CandidateProfile currentCandidate() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getAuthorities().stream().noneMatch(a -> "ROLE_CANDIDATE".equals(a.getAuthority()))) {
			throw new AccessDeniedException("Only authenticated candidates can apply to opportunities");
		}
		User user = userRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		return candidateProfileRepository.findByUserUserId(user.getUserId())
				.orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));
	}

	private boolean isOpen(Opportunity opportunity) {
		String status = opportunity.getStatus();
		return status != null
				&& List.of("OPEN", "ACTIVE", "PUBLISHED").contains(status.toUpperCase(Locale.ROOT))
				&& (opportunity.getApplicationDeadline() == null
						|| !opportunity.getApplicationDeadline().isBefore(LocalDateTime.now()));
	}

	private OpportunityApplicationResponseDto toResponse(OpportunityApplication application) {
		OpportunityApplicationResponseDto response = new OpportunityApplicationResponseDto();
		response.setOpportunityId(application.getOpportunity().getOpportunityId());
		response.setOpportunityTitle(application.getOpportunity().getTitle());
		response.setOpportunityType(application.getOpportunity().getOpportunityType().name());
		response.setApplicationStatus(application.getApplicationStatus());
		response.setAppliedAt(application.getAppliedAt());
		return response;
	}
}
