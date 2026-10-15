package com.smartjobportal.job.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.smartjobportal.entity.Company;
import com.smartjobportal.entity.Opportunity;
import com.smartjobportal.entity.OpportunityType;
import com.smartjobportal.entity.RecruiterProfile;
import com.smartjobportal.entity.User;
import com.smartjobportal.exception.ResourceNotFoundException;
import com.smartjobportal.exception.UnauthorizedActionException;
import com.smartjobportal.job.dto.CreateHackathonDto;
import com.smartjobportal.job.dto.CreateInternshipDto;
import com.smartjobportal.job.dto.OpportunityRequestDto;
import com.smartjobportal.job.dto.OpportunityResponseDto;
import com.smartjobportal.repository.OpportunityRepository;
import com.smartjobportal.repository.RecruiterProfileRepository;
import com.smartjobportal.repository.UserRepository;

@Service
public class OpportunityService {

	private final OpportunityRepository opportunityRepository;
	private final UserRepository userRepository;
	private final RecruiterProfileRepository recruiterProfileRepository;

	public OpportunityService(
			OpportunityRepository opportunityRepository,
			UserRepository userRepository,
			RecruiterProfileRepository recruiterProfileRepository) {
		this.opportunityRepository = opportunityRepository;
		this.userRepository = userRepository;
		this.recruiterProfileRepository = recruiterProfileRepository;
	}

	@Transactional
	public OpportunityResponseDto createInternship(CreateInternshipDto request) {
		if ("PAID".equals(request.getStipendType()) && request.getStipendAmount() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A paid internship must include a stipend amount");
		}
		if ("UNPAID".equals(request.getStipendType()) && request.getStipendAmount() != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An unpaid internship cannot include a stipend amount");
		}

		Opportunity opportunity = newOpportunity(request, OpportunityType.INTERNSHIP);
		opportunity.setExperienceRequired(request.getExperienceRequired());
		opportunity.setFresherEligible(request.getFresherEligible());
		opportunity.setStipendType(request.getStipendType());
		opportunity.setStipendAmount(request.getStipendAmount());
		return getOpportunityResponse(opportunityRepository.save(opportunity));
	}

	@Transactional
	public OpportunityResponseDto createHackathon(CreateHackathonDto request) {
		Opportunity opportunity = newOpportunity(request, OpportunityType.HACKATHON);
		opportunity.setProblemStatement(request.getProblemStatement());
		opportunity.setParticipationOptions(request.getParticipationOptions());
		opportunity.setEventDate(request.getEventDate());
		if (request.getEventEndDate() != null && request.getEventEndDate().isBefore(request.getEventDate())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event end date cannot be before its start date");
		}
		opportunity.setEventEndDate(request.getEventEndDate());
		opportunity.setVenueAddress(blankToNull(request.getVenueAddress()));
		opportunity.setPrizes(blankToNull(request.getPrizes()));
		opportunity.setJudgingCriteria(blankToNull(request.getJudgingCriteria()));
		return getOpportunityResponse(opportunityRepository.save(opportunity));
	}

	@Transactional(readOnly = true)
	public List<OpportunityResponseDto> getMyOpportunities() {
		RecruiterProfile recruiterProfile = currentRecruiter().recruiterProfile();
		return opportunityRepository.findByRecruiterProfileOrderByCreatedAtDesc(recruiterProfile)
				.stream()
				.map(this::getOpportunityResponse)
				.toList();
	}

	private Opportunity newOpportunity(OpportunityRequestDto request, OpportunityType type) {
		RecruiterContext context = currentRecruiter();
		Opportunity opportunity = new Opportunity();
		opportunity.setOpportunityType(type);
		opportunity.setTitle(request.getTitle().trim());
		opportunity.setDescription(request.getDescription().trim());
		opportunity.setRequiredSkills(request.getRequiredSkills().trim());
		opportunity.setEligibilityCriteria(request.getEligibilityCriteria().trim());
		opportunity.setDuration(request.getDuration().trim());
		opportunity.setWorkMode(request.getWorkMode().trim());
		opportunity.setLocation(request.getLocation().trim());
		opportunity.setApplicationDeadline(request.getApplicationDeadline());
		opportunity.setApplicationLink(blankToNull(request.getApplicationLink()));
		opportunity.setApplicationInstructions(blankToNull(request.getApplicationInstructions()));
		opportunity.setCompany(context.company());
		opportunity.setRecruiterProfile(context.recruiterProfile());
		opportunity.setStatus("OPEN");
		return opportunity;
	}

	private RecruiterContext currentRecruiter() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getAuthorities().stream().noneMatch(a -> "ROLE_RECRUITER".equals(a.getAuthority()))) {
			throw new AccessDeniedException("Only authenticated recruiters can manage opportunities");
		}

		User user = userRepository.findByEmail(authentication.getName())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		RecruiterProfile recruiterProfile = recruiterProfileRepository.findByUserUserId(user.getUserId())
				.orElseThrow(() -> new UnauthorizedActionException("Recruiter profile not found"));
		Company company = recruiterProfile.getCompany();
		if (company == null) {
			throw new UnauthorizedActionException("Recruiter is not assigned to any company");
		}
		return new RecruiterContext(recruiterProfile, company);
	}

	public OpportunityResponseDto getOpportunityResponse(Opportunity opportunity) {
		OpportunityResponseDto response = new OpportunityResponseDto();
		response.setOpportunityId(opportunity.getOpportunityId());
		response.setOpportunityType(opportunity.getOpportunityType());
		response.setTitle(opportunity.getTitle());
		response.setDescription(opportunity.getDescription());
		response.setProblemStatement(opportunity.getProblemStatement());
		response.setRequiredSkills(opportunity.getRequiredSkills());
		response.setEligibilityCriteria(opportunity.getEligibilityCriteria());
		response.setExperienceRequired(opportunity.getExperienceRequired());
		response.setFresherEligible(opportunity.getFresherEligible());
		response.setDuration(opportunity.getDuration());
		response.setStipendType(opportunity.getStipendType());
		response.setStipendAmount(opportunity.getStipendAmount());
		response.setParticipationOptions(opportunity.getParticipationOptions());
		response.setWorkMode(opportunity.getWorkMode());
		response.setLocation(opportunity.getLocation());
		response.setApplicationDeadline(opportunity.getApplicationDeadline());
		response.setEventDate(opportunity.getEventDate());
		response.setEventEndDate(opportunity.getEventEndDate());
		response.setVenueAddress(opportunity.getVenueAddress());
		response.setPrizes(opportunity.getPrizes());
		response.setJudgingCriteria(opportunity.getJudgingCriteria());
		response.setApplicationLink(opportunity.getApplicationLink());
		response.setApplicationInstructions(opportunity.getApplicationInstructions());
		response.setStatus(opportunity.getStatus());
		Company company = opportunity.getCompany();
		response.setCompanyName(company.getCompanyName());
		response.setCompanyPhone(company.getCompanyPhone());
		response.setCompanyWebsite(company.getCompanyWebsite());
		response.setCompanyAddress(company.getHeadquartersLocation());
		response.setCreatedAt(opportunity.getCreatedAt());
		return response;
	}

	@Transactional(readOnly = true)
	public OpportunityResponseDto getOpportunityDetails(OpportunityType type, Integer id) {
		Opportunity opportunity = opportunityRepository.findById(id)
				.filter(item -> item.getOpportunityType() == type)
				.orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
		return getOpportunityResponse(opportunity);
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private record RecruiterContext(RecruiterProfile recruiterProfile, Company company) {}
}
