package com.smartjobportal.job.dto;

import java.time.LocalDateTime;

import com.smartjobportal.entity.ApplicationStatus;

public class OpportunityApplicationResponseDto {

	private Integer opportunityId;
	private String opportunityTitle;
	private String opportunityType;
	private ApplicationStatus applicationStatus;
	private LocalDateTime appliedAt;

	public Integer getOpportunityId() { return opportunityId; }
	public void setOpportunityId(Integer opportunityId) { this.opportunityId = opportunityId; }
	public String getOpportunityTitle() { return opportunityTitle; }
	public void setOpportunityTitle(String opportunityTitle) { this.opportunityTitle = opportunityTitle; }
	public String getOpportunityType() { return opportunityType; }
	public void setOpportunityType(String opportunityType) { this.opportunityType = opportunityType; }
	public ApplicationStatus getApplicationStatus() { return applicationStatus; }
	public void setApplicationStatus(ApplicationStatus applicationStatus) { this.applicationStatus = applicationStatus; }
	public LocalDateTime getAppliedAt() { return appliedAt; }
	public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}
