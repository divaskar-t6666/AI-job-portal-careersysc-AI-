package com.smartjobportal.entity;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "opportunity_applications", uniqueConstraints = @UniqueConstraint(
		name = "uk_opportunity_application_candidate",
		columnNames = { "candidate_profile_id", "opportunity_id" }))
public class OpportunityApplication {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "opportunity_application_id")
	private Integer opportunityApplicationId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "candidate_profile_id", nullable = false)
	private CandidateProfile candidateProfile;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "opportunity_id", nullable = false)
	private Opportunity opportunity;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "resume_id", nullable = false)
	private Resume resume;

	@Enumerated(EnumType.STRING)
	@Column(name = "application_status", nullable = false)
	private ApplicationStatus applicationStatus;

	@Column(name = "cover_letter", columnDefinition = "TEXT")
	private String coverLetter;

	@Column(name = "applied_at", nullable = false)
	private LocalDateTime appliedAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void prePersist() {
		appliedAt = LocalDateTime.now();
		updatedAt = appliedAt;
		if (applicationStatus == null) {
			applicationStatus = ApplicationStatus.APPLIED;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public Integer getOpportunityApplicationId() { return opportunityApplicationId; }
	public CandidateProfile getCandidateProfile() { return candidateProfile; }
	public void setCandidateProfile(CandidateProfile candidateProfile) { this.candidateProfile = candidateProfile; }
	public Opportunity getOpportunity() { return opportunity; }
	public void setOpportunity(Opportunity opportunity) { this.opportunity = opportunity; }
	public Resume getResume() { return resume; }
	public void setResume(Resume resume) { this.resume = resume; }
	public ApplicationStatus getApplicationStatus() { return applicationStatus; }
	public void setApplicationStatus(ApplicationStatus applicationStatus) { this.applicationStatus = applicationStatus; }
	public String getCoverLetter() { return coverLetter; }
	public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }
	public LocalDateTime getAppliedAt() { return appliedAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
}
