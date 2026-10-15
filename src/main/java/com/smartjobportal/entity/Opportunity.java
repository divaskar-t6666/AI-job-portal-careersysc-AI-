package com.smartjobportal.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "opportunities")
public class Opportunity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "opportunity_id")
	private Integer opportunityId;

	@Enumerated(EnumType.STRING)
	@Column(name = "opportunity_type", nullable = false, length = 20)
	private OpportunityType opportunityType;

	@Column(name = "title", nullable = false, length = 150)
	private String title;

	@Column(name = "description", nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(name = "problem_statement", columnDefinition = "TEXT")
	private String problemStatement;

	@Column(name = "required_skills", nullable = false, columnDefinition = "TEXT")
	private String requiredSkills;

	@Column(name = "eligibility_criteria", nullable = false, columnDefinition = "TEXT")
	private String eligibilityCriteria;

	@Column(name = "experience_required")
	private Integer experienceRequired;

	@Column(name = "fresher_eligible")
	private Boolean fresherEligible;

	@Column(name = "duration", nullable = false, length = 100)
	private String duration;

	@Column(name = "stipend_type", length = 20)
	private String stipendType;

	@Column(name = "stipend_amount", precision = 12, scale = 2)
	private BigDecimal stipendAmount;

	@Column(name = "participation_options", length = 30)
	private String participationOptions;

	@Column(name = "work_mode", nullable = false, length = 30)
	private String workMode;

	@Column(name = "location", nullable = false, length = 200)
	private String location;

	@Column(name = "application_deadline", nullable = false)
	private LocalDateTime applicationDeadline;

	@Column(name = "event_date")
	private LocalDateTime eventDate;

	@Column(name = "event_end_date")
	private LocalDateTime eventEndDate;

	@Column(name = "venue_address", length = 500)
	private String venueAddress;

	@Column(name = "prizes", columnDefinition = "TEXT")
	private String prizes;

	@Column(name = "judging_criteria", columnDefinition = "TEXT")
	private String judgingCriteria;

	@Column(name = "application_link", length = 2048)
	private String applicationLink;

	@Column(name = "application_instructions", columnDefinition = "TEXT")
	private String applicationInstructions;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false)
	private Company company;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recruiter_profile_id", nullable = false)
	private RecruiterProfile recruiterProfile;

	@PrePersist
	void prePersist() {
		createdAt = LocalDateTime.now();
		updatedAt = createdAt;
		if (status == null) {
			status = "OPEN";
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public Integer getOpportunityId() { return opportunityId; }
	public void setOpportunityId(Integer opportunityId) { this.opportunityId = opportunityId; }
	public OpportunityType getOpportunityType() { return opportunityType; }
	public void setOpportunityType(OpportunityType opportunityType) { this.opportunityType = opportunityType; }
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
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public Company getCompany() { return company; }
	public void setCompany(Company company) { this.company = company; }
	public RecruiterProfile getRecruiterProfile() { return recruiterProfile; }
	public void setRecruiterProfile(RecruiterProfile recruiterProfile) { this.recruiterProfile = recruiterProfile; }
}
