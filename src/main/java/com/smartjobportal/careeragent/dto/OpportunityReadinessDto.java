package com.smartjobportal.careeragent.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OpportunityReadinessDto {

	private Integer opportunityId;
	private String opportunityType;
	private String title;
	private boolean resumeAvailable;
	private boolean resumeParsed;
	private List<String> matchedSkills = List.of();
	private List<String> missingRequiredSkills = List.of();
	private List<String> missingPreferredSkills = List.of();
	private List<String> requirementsSatisfied = List.of();
	private List<String> requirementsNotDemonstrated = List.of();
	private List<String> informationToConfirm = List.of();
	private List<String> recommendedNextSteps = List.of();
	private String eligibilityAssessment;
	private String experienceAssessment;
	private Integer experienceRequired;
	private Boolean fresherEligible;
	private List<String> projectRequirements = List.of();
	private String status;
	private LocalDateTime applicationDeadline;
	private boolean currentlyAvailable;

	public Integer getOpportunityId() { return opportunityId; }
	public void setOpportunityId(Integer opportunityId) { this.opportunityId = opportunityId; }
	public String getOpportunityType() { return opportunityType; }
	public void setOpportunityType(String opportunityType) { this.opportunityType = opportunityType; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public boolean isResumeAvailable() { return resumeAvailable; }
	public void setResumeAvailable(boolean resumeAvailable) { this.resumeAvailable = resumeAvailable; }
	public boolean isResumeParsed() { return resumeParsed; }
	public void setResumeParsed(boolean resumeParsed) { this.resumeParsed = resumeParsed; }
	public List<String> getMatchedSkills() { return matchedSkills; }
	public void setMatchedSkills(List<String> matchedSkills) { this.matchedSkills = matchedSkills; }
	public List<String> getMissingRequiredSkills() { return missingRequiredSkills; }
	public void setMissingRequiredSkills(List<String> missingRequiredSkills) { this.missingRequiredSkills = missingRequiredSkills; }
	public List<String> getMissingPreferredSkills() { return missingPreferredSkills; }
	public void setMissingPreferredSkills(List<String> missingPreferredSkills) { this.missingPreferredSkills = missingPreferredSkills; }
	public List<String> getRequirementsSatisfied() { return requirementsSatisfied; }
	public void setRequirementsSatisfied(List<String> requirementsSatisfied) { this.requirementsSatisfied = requirementsSatisfied; }
	public List<String> getRequirementsNotDemonstrated() { return requirementsNotDemonstrated; }
	public void setRequirementsNotDemonstrated(List<String> requirementsNotDemonstrated) { this.requirementsNotDemonstrated = requirementsNotDemonstrated; }
	public List<String> getInformationToConfirm() { return informationToConfirm; }
	public void setInformationToConfirm(List<String> informationToConfirm) { this.informationToConfirm = informationToConfirm; }
	public List<String> getRecommendedNextSteps() { return recommendedNextSteps; }
	public void setRecommendedNextSteps(List<String> recommendedNextSteps) { this.recommendedNextSteps = recommendedNextSteps; }
	public String getEligibilityAssessment() { return eligibilityAssessment; }
	public void setEligibilityAssessment(String eligibilityAssessment) { this.eligibilityAssessment = eligibilityAssessment; }
	public String getExperienceAssessment() { return experienceAssessment; }
	public void setExperienceAssessment(String experienceAssessment) { this.experienceAssessment = experienceAssessment; }
	public Integer getExperienceRequired() { return experienceRequired; }
	public void setExperienceRequired(Integer experienceRequired) { this.experienceRequired = experienceRequired; }
	public Boolean getFresherEligible() { return fresherEligible; }
	public void setFresherEligible(Boolean fresherEligible) { this.fresherEligible = fresherEligible; }
	public List<String> getProjectRequirements() { return projectRequirements; }
	public void setProjectRequirements(List<String> projectRequirements) { this.projectRequirements = projectRequirements; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public LocalDateTime getApplicationDeadline() { return applicationDeadline; }
	public void setApplicationDeadline(LocalDateTime applicationDeadline) { this.applicationDeadline = applicationDeadline; }
	public boolean isCurrentlyAvailable() { return currentlyAvailable; }
	public void setCurrentlyAvailable(boolean currentlyAvailable) { this.currentlyAvailable = currentlyAvailable; }
}
