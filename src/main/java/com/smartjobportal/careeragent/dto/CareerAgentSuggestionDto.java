package com.smartjobportal.careeragent.dto;

public class CareerAgentSuggestionDto {

	private Integer opportunityId;
	private String opportunityType;
	private String title;
	private String companyName;
	private String matchedSkills;
	private String requiredSkills;
	private String missingSkills;

	public Integer getOpportunityId() { return opportunityId; }
	public void setOpportunityId(Integer opportunityId) { this.opportunityId = opportunityId; }
	public String getOpportunityType() { return opportunityType; }
	public void setOpportunityType(String opportunityType) { this.opportunityType = opportunityType; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getCompanyName() { return companyName; }
	public void setCompanyName(String companyName) { this.companyName = companyName; }
	public String getMatchedSkills() { return matchedSkills; }
	public void setMatchedSkills(String matchedSkills) { this.matchedSkills = matchedSkills; }
	public String getRequiredSkills() { return requiredSkills; }
	public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }
	public String getMissingSkills() { return missingSkills; }
	public void setMissingSkills(String missingSkills) { this.missingSkills = missingSkills; }
}
