package com.smartjobportal.job.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartjobportal.job.dto.CreateHackathonDto;
import com.smartjobportal.job.dto.CreateInternshipDto;
import com.smartjobportal.job.dto.ApplyOpportunityDto;
import com.smartjobportal.job.dto.OpportunityApplicationResponseDto;
import com.smartjobportal.job.dto.OpportunityListingDto;
import com.smartjobportal.job.dto.OpportunityPageResponseDto;
import com.smartjobportal.job.dto.OpportunityResponseDto;
import com.smartjobportal.job.dto.OpportunitySearchDto;
import com.smartjobportal.job.service.OpportunityApplicationService;
import com.smartjobportal.job.service.OpportunityDiscoveryService;
import com.smartjobportal.job.service.OpportunityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {

	private final OpportunityService opportunityService;
	private final OpportunityDiscoveryService discoveryService;
	private final OpportunityApplicationService applicationService;

	public OpportunityController(
			OpportunityService opportunityService,
			OpportunityDiscoveryService discoveryService,
			OpportunityApplicationService applicationService) {
		this.opportunityService = opportunityService;
		this.discoveryService = discoveryService;
		this.applicationService = applicationService;
	}

	@PostMapping("/internships")
	public ResponseEntity<OpportunityResponseDto> createInternship(@Valid @RequestBody CreateInternshipDto request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(opportunityService.createInternship(request));
	}

	@PostMapping("/hackathons")
	public ResponseEntity<OpportunityResponseDto> createHackathon(@Valid @RequestBody CreateHackathonDto request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(opportunityService.createHackathon(request));
	}

	@GetMapping("/my")
	public ResponseEntity<List<OpportunityResponseDto>> getMyOpportunities() {
		return ResponseEntity.ok(opportunityService.getMyOpportunities());
	}

	@PostMapping("/search")
	public ResponseEntity<OpportunityPageResponseDto> search(
			@RequestBody OpportunitySearchDto request,
			@RequestParam(defaultValue = "0") int pageNumber,
			@RequestParam(defaultValue = "6") int pageSize,
			@RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "desc") String sortDirection) {
		return ResponseEntity.ok(
				discoveryService.search(request, pageNumber, pageSize, sortBy, sortDirection));
	}

	@GetMapping("/details/{type}/{id}")
	public ResponseEntity<OpportunityListingDto> getDetails(
			@PathVariable String type,
			@PathVariable Integer id) {
		return ResponseEntity.ok(discoveryService.getDetails(type, id));
	}

	@PostMapping("/{opportunityId}/apply")
	public ResponseEntity<OpportunityApplicationResponseDto> apply(
			@PathVariable Integer opportunityId,
			@Valid @RequestBody ApplyOpportunityDto request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.apply(opportunityId, request));
	}

	@GetMapping("/my-applications")
	public ResponseEntity<List<OpportunityApplicationResponseDto>> getMyApplications() {
		return ResponseEntity.ok(applicationService.getMyApplications());
	}

	@GetMapping("/{opportunityId}/my-application")
	public ResponseEntity<OpportunityApplicationResponseDto> getMyApplication(@PathVariable Integer opportunityId) {
		OpportunityApplicationResponseDto application = applicationService.getMyApplication(opportunityId);
		return application == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(application);
	}
}
