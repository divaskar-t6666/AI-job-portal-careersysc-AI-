package com.smartjobportal.job.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartjobportal.entity.Opportunity;
import com.smartjobportal.entity.OpportunityType;
import com.smartjobportal.exception.ResourceNotFoundException;
import com.smartjobportal.job.dto.JobResponseDto;
import com.smartjobportal.job.dto.JobSearchDto;
import com.smartjobportal.job.dto.OpportunityListingDto;
import com.smartjobportal.job.dto.OpportunityPageResponseDto;
import com.smartjobportal.job.dto.OpportunityResponseDto;
import com.smartjobportal.job.dto.OpportunitySearchDto;
import com.smartjobportal.repository.OpportunityRepository;

@Service
public class OpportunityDiscoveryService {

	private static final Set<String> PUBLISHED_STATUSES = Set.of("OPEN", "PUBLISHED", "ACTIVE");

	private final JobService jobService;
	private final OpportunityRepository opportunityRepository;
	private final OpportunityService opportunityService;

	public OpportunityDiscoveryService(
			JobService jobService,
			OpportunityRepository opportunityRepository,
			OpportunityService opportunityService) {
		this.jobService = jobService;
		this.opportunityRepository = opportunityRepository;
		this.opportunityService = opportunityService;
	}

	@Transactional(readOnly = true)
	public OpportunityPageResponseDto search(OpportunitySearchDto search, int page, int pageSize,
			String sortBy, String sortDirection) {
		if (page < 0 || pageSize < 1 || pageSize > 100) {
			throw new IllegalArgumentException("Page must be non-negative and page size must be between 1 and 100");
		}
		String category = normalize(search.getOpportunityType());
		if (category != null && !Set.of("JOB", "INTERNSHIP", "HACKATHON").contains(category.toUpperCase(Locale.ROOT))) {
			throw new IllegalArgumentException("Unsupported opportunity type");
		}

		List<OpportunityListingDto> matches = new ArrayList<>();
		if (category == null || category.equalsIgnoreCase("JOB")) {
			JobSearchDto jobSearch = copyJobFilters(search);
			jobSearch.setKeyword(withoutJobTypeKeyword(jobSearch.getKeyword()));
			for (JobResponseDto job : jobService.searchJobs(jobSearch)) {
				if (!isAvailable(job.getJobStatus(), job.getApplicationDeadline(), search.getJobStatus())) {
					continue;
				}
				matches.add(fromJob(job));
			}
		}

		if (category == null || !category.equalsIgnoreCase("JOB")) {
			for (Opportunity opportunity : opportunityRepository.findAll()) {
				if (!isAvailable(opportunity.getStatus(), opportunity.getApplicationDeadline(), search.getJobStatus())
						|| !matchesFilters(opportunity, search, category)) {
					continue;
				}
				matches.add(fromOpportunity(opportunityService.getOpportunityResponse(opportunity)));
			}
		}

		Comparator<OpportunityListingDto> comparator = comparator(sortBy);
		boolean titleSort = "jobTitle".equalsIgnoreCase(sortBy) || "title".equalsIgnoreCase(sortBy);
		if ("desc".equalsIgnoreCase(sortDirection) || (!titleSort && !"asc".equalsIgnoreCase(sortDirection))) {
			comparator = comparator.reversed();
		}
		matches.sort(comparator);

		int totalElements = matches.size();
		int totalPages = (int) Math.ceil((double) totalElements / pageSize);
		int fromIndex = Math.min(page * pageSize, totalElements);
		int toIndex = Math.min(fromIndex + pageSize, totalElements);

		OpportunityPageResponseDto response = new OpportunityPageResponseDto();
		response.setContent(matches.subList(fromIndex, toIndex));
		response.setPageNumber(page);
		response.setPageSize(pageSize);
		response.setTotalElements(totalElements);
		response.setTotalPages(totalPages);
		response.setLastPage(page >= totalPages - 1);
		return response;
	}

	@Transactional(readOnly = true)
	public OpportunityListingDto getDetails(String type, Integer id) {
		if ("JOB".equalsIgnoreCase(type)) {
			JobResponseDto job = jobService.getJobById(id);
			return fromJob(job);
		}
		OpportunityType opportunityType;
		try {
			opportunityType = OpportunityType.valueOf(type.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new ResourceNotFoundException("Opportunity not found");
		}
		Opportunity opportunity = opportunityRepository.findById(id)
				.filter(item -> item.getOpportunityType() == opportunityType)
				.orElseThrow(() -> new ResourceNotFoundException("Opportunity not found"));
		return fromOpportunity(opportunityService.getOpportunityResponse(opportunity));
	}

	private boolean matchesFilters(Opportunity opportunity, OpportunitySearchDto filters, String category) {
		if (category != null && !opportunity.getOpportunityType().name().equalsIgnoreCase(category)) {
			return false;
		}
		if (!matchesTerms(opportunity, filters.getKeyword())) {
			return false;
		}
		if (!contains(opportunity.getLocation(), filters.getLocation())) {
			return false;
		}
		if (!contains(opportunity.getRequiredSkills(), filters.getSkill())) {
			return false;
		}
		if (!contains(opportunity.getCompany().getCompanyName(), filters.getCompanyName())) {
			return false;
		}
		if (normalize(filters.getEmploymentType()) != null
				&& !opportunity.getOpportunityType().name().equalsIgnoreCase(filters.getEmploymentType())) {
			return false;
		}
		if (filters.getExperienceRequired() != null
				&& (opportunity.getExperienceRequired() == null
						|| opportunity.getExperienceRequired() > filters.getExperienceRequired())) {
			return false;
		}
		return normalize(filters.getJobStatus()) == null
				|| opportunity.getStatus().equalsIgnoreCase(filters.getJobStatus());
	}

	private boolean matchesTerms(Opportunity opportunity, String keyword) {
		String normalized = normalize(keyword);
		if (normalized == null) {
			return true;
		}
		String searchable = String.join(" ",
				safe(opportunity.getTitle()),
				safe(opportunity.getCompany().getCompanyName()),
				safe(opportunity.getRequiredSkills()),
				safe(opportunity.getDescription()),
				safe(opportunity.getProblemStatement()),
				opportunity.getOpportunityType().name()).toLowerCase(Locale.ROOT);
		return List.of(normalized.toLowerCase(Locale.ROOT).split("\\s+"))
				.stream().allMatch(searchable::contains);
	}

	private boolean isAvailable(String status, LocalDateTime deadline, String requestedStatus) {
		return status != null
				&& (normalize(requestedStatus) == null
						? PUBLISHED_STATUSES.contains(status.toUpperCase(Locale.ROOT))
						: status.equalsIgnoreCase(requestedStatus.trim()))
				&& (deadline == null || !deadline.isBefore(LocalDateTime.now()));
	}

	private Comparator<OpportunityListingDto> comparator(String sortBy) {
		if ("jobTitle".equalsIgnoreCase(sortBy) || "title".equalsIgnoreCase(sortBy)) {
			return Comparator.comparing(item -> safe(item.getTitle()).toLowerCase(Locale.ROOT));
		}
		return Comparator.comparing(
				OpportunityListingDto::getCreatedAt,
				Comparator.nullsLast(Comparator.naturalOrder()));
	}

	private JobSearchDto copyJobFilters(OpportunitySearchDto source) {
		JobSearchDto target = new JobSearchDto();
		target.setKeyword(source.getKeyword());
		target.setLocation(source.getLocation());
		target.setSkill(source.getSkill());
		target.setCompanyName(source.getCompanyName());
		target.setEmploymentType(source.getEmploymentType());
		target.setExperienceRequired(source.getExperienceRequired());
		target.setJobStatus(source.getJobStatus());
		return target;
	}

	private String withoutJobTypeKeyword(String keyword) {
		String normalized = normalize(keyword);
		if (normalized == null) {
			return null;
		}
		String remaining = java.util.Arrays.stream(normalized.split("\\s+"))
				.filter(term -> !term.equalsIgnoreCase("job") && !term.equalsIgnoreCase("jobs"))
				.collect(Collectors.joining(" "));
		return remaining.isBlank() ? null : remaining;
	}

	private boolean contains(String field, String value) {
		String normalized = normalize(value);
		return normalized == null
				|| (field != null && field.toLowerCase(Locale.ROOT).contains(normalized.toLowerCase(Locale.ROOT)));
	}

	private String normalize(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}

	private OpportunityListingDto fromJob(JobResponseDto job) {
		OpportunityListingDto item = new OpportunityListingDto();
		item.setId(job.getJobId());
		item.setOpportunityType("JOB");
		item.setTitle(job.getJobTitle());
		item.setDescription(job.getJobDescription());
		item.setRequiredSkills(job.getRequiredSkills());
		item.setCompanyName(job.getCompanyName());
		item.setCompanyPhone(job.getCompanyPhone());
		item.setCompanyWebsite(job.getCompanyWebsite());
		item.setCompanyAddress(job.getCompanyAddress());
		item.setLocation(job.getJobLocation());
		item.setEmploymentType(job.getEmploymentType());
		item.setSalaryPackage(job.getSalaryPackage());
		item.setExperienceRequired(job.getExperienceRequired());
		item.setApplicationDeadline(job.getApplicationDeadline());
		item.setCreatedAt(job.getCreatedAt());
		item.setStatus(job.getJobStatus());
		return item;
	}

	private OpportunityListingDto fromOpportunity(OpportunityResponseDto opportunity) {
		OpportunityListingDto item = new OpportunityListingDto();
		item.setId(opportunity.getOpportunityId());
		item.setOpportunityType(opportunity.getOpportunityType().name());
		item.setTitle(opportunity.getTitle());
		item.setDescription(opportunity.getDescription());
		item.setProblemStatement(opportunity.getProblemStatement());
		item.setRequiredSkills(opportunity.getRequiredSkills());
		item.setEligibilityCriteria(opportunity.getEligibilityCriteria());
		item.setExperienceRequired(opportunity.getExperienceRequired());
		item.setFresherEligible(opportunity.getFresherEligible());
		item.setDuration(opportunity.getDuration());
		item.setStipendType(opportunity.getStipendType());
		item.setStipendAmount(opportunity.getStipendAmount());
		item.setParticipationOptions(opportunity.getParticipationOptions());
		item.setWorkMode(opportunity.getWorkMode());
		item.setLocation(opportunity.getLocation());
		item.setApplicationDeadline(opportunity.getApplicationDeadline());
		item.setEventDate(opportunity.getEventDate());
		item.setEventEndDate(opportunity.getEventEndDate());
		item.setVenueAddress(opportunity.getVenueAddress());
		item.setPrizes(opportunity.getPrizes());
		item.setJudgingCriteria(opportunity.getJudgingCriteria());
		item.setApplicationLink(opportunity.getApplicationLink());
		item.setApplicationInstructions(opportunity.getApplicationInstructions());
		item.setStatus(opportunity.getStatus());
		item.setCompanyName(opportunity.getCompanyName());
		item.setCompanyPhone(opportunity.getCompanyPhone());
		item.setCompanyWebsite(opportunity.getCompanyWebsite());
		item.setCompanyAddress(opportunity.getCompanyAddress());
		item.setCreatedAt(opportunity.getCreatedAt());
		return item;
	}
}
