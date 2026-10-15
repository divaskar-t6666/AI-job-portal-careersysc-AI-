package com.smartjobportal.job.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OpportunityListingDto {

	private Integer id;
	private String opportunityType;
	private String title;
	private String description;
	private String problemStatement;
	private String requiredSkills;
	private String eligibilityCriteria;
	private Integer experienceRequired;
	private Boolean fresherEligible;
	private String duration;
	private String stipendType;
	private BigDecimal stipendAmount;
	private String participationOptions;
	private String workMode;
	private String location;
	private String employmentType;
	private String salaryPackage;
	private LocalDateTime applicationDeadline;
	private LocalDateTime eventDate;
	private LocalDateTime eventEndDate;
	private String venueAddress;
	private String prizes;
	private String judgingCriteria;
	private String applicationLink;
	private String applicationInstructions;
	private String status;
	private String companyName;
	private String companyPhone;
	private String companyWebsite;
	private String companyAddress;
	private LocalDateTime createdAt;

	public Integer getId() { return id; }
	public void setId(Integer id) { this.id = id; }
	public String getOpportunityType() { return opportunityType; }
	public void setOpportunityType(String opportunityType) { this.opportunityType = opportunityType; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public String getProblemStatement() { return problemStatement; }
	public void setProblemStatement(String problemStatement) { this.problemStatement = problemStatement; }
	public String getRequiredSkills() { return requiredSkills; }
	public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }
	public String getEligibilityCriteria() { return eligibilityCriteria; }
	public void setEligibilityCriteria(String eligibilityCriteria) { this.eligibilityCriteria = eligibilityCriteria; }
	public Integer getExperienceRequired() { return experienceRequired; }
	public void setExperienceRequired(Integer experienceRequired) { this.experienceRequired = experienceRequired; }
	public Boolean getFresherEligible() { return fresherEligible; }
	public void setFresherEligible(Boolean fresherEligible) { this.fresherEligible = fresherEligible; }
	public String getDuration() { return duration; }
	public void setDuration(String duration) { this.duration = duration; }
	public String getStipendType() { return stipendType; }
	public void setStipendType(String stipendType) { this.stipendType = stipendType; }
	public BigDecimal getStipendAmount() { return stipendAmount; }
	public void setStipendAmount(BigDecimal stipendAmount) { this.stipendAmount = stipendAmount; }
	public String getParticipationOptions() { return participationOptions; }
	public void setParticipationOptions(String participationOptions) { this.participationOptions = participationOptions; }
	public String getWorkMode() { return workMode; }
	public void setWorkMode(String workMode) { this.workMode = workMode; }
	public String getLocation() { return location; }
	public void setLocation(String location) { this.location = location; }
	public String getEmploymentType() { return employmentType; }
	public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
	public String getSalaryPackage() { return salaryPackage; }
	public void setSalaryPackage(String salaryPackage) { this.salaryPackage = salaryPackage; }
	public LocalDateTime getApplicationDeadline() { return applicationDeadline; }
	public void setApplicationDeadline(LocalDateTime applicationDeadline) { this.applicationDeadline = applicationDeadline; }
	public LocalDateTime getEventDate() { return eventDate; }
	public void setEventDate(LocalDateTime eventDate) { this.eventDate = eventDate; }
	public LocalDateTime getEventEndDate() { return eventEndDate; }
	public void setEventEndDate(LocalDateTime eventEndDate) { this.eventEndDate = eventEndDate; }
	public String getVenueAddress() { return venueAddress; }
	public void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }
	public String getPrizes() { return prizes; }
	public void setPrizes(String prizes) { this.prizes = prizes; }
	public String getJudgingCriteria() { return judgingCriteria; }
	public void setJudgingCriteria(String judgingCriteria) { this.judgingCriteria = judgingCriteria; }
	public String getApplicationLink() { return applicationLink; }
	public void setApplicationLink(String applicationLink) { this.applicationLink = applicationLink; }
	public String getApplicationInstructions() { return applicationInstructions; }
	public void setApplicationInstructions(String applicationInstructions) { this.applicationInstructions = applicationInstructions; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }
	public String getCompanyName() { return companyName; }
	public void setCompanyName(String companyName) { this.companyName = companyName; }
	public String getCompanyPhone() { return companyPhone; }
	public void setCompanyPhone(String companyPhone) { this.companyPhone = companyPhone; }
	public String getCompanyWebsite() { return companyWebsite; }
	public void setCompanyWebsite(String companyWebsite) { this.companyWebsite = companyWebsite; }
	public String getCompanyAddress() { return companyAddress; }
	public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
