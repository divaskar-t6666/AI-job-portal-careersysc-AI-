package com.smartjobportal.job.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public abstract class OpportunityRequestDto {

	@NotBlank
	@Size(max = 150)
	private String title;

	@NotBlank
	@Size(max = 10000)
	private String description;

	@NotBlank
	@Size(max = 5000)
	private String requiredSkills;

	@NotBlank
	@Size(max = 5000)
	private String eligibilityCriteria;

	@NotBlank
	@Size(max = 100)
	private String duration;

	@NotBlank
	@Size(max = 30)
	private String workMode;

	@NotBlank
	@Size(max = 200)
	private String location;

	@NotNull
	@Future
	private LocalDateTime applicationDeadline;

	@Size(max = 2048)
	@Pattern(regexp = "https?://[^\\s]+", message = "Application link must use HTTP or HTTPS")
	private String applicationLink;

	@Size(max = 5000)
	private String applicationInstructions;

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public String getRequiredSkills() { return requiredSkills; }
	public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }
	public String getEligibilityCriteria() { return eligibilityCriteria; }
	public void setEligibilityCriteria(String eligibilityCriteria) { this.eligibilityCriteria = eligibilityCriteria; }
	public String getDuration() { return duration; }
	public void setDuration(String duration) { this.duration = duration; }
	public String getWorkMode() { return workMode; }
	public void setWorkMode(String workMode) { this.workMode = workMode; }
	public String getLocation() { return location; }
	public void setLocation(String location) { this.location = location; }
	public LocalDateTime getApplicationDeadline() { return applicationDeadline; }
	public void setApplicationDeadline(LocalDateTime applicationDeadline) { this.applicationDeadline = applicationDeadline; }
	public String getApplicationLink() { return applicationLink; }
	public void setApplicationLink(String applicationLink) { this.applicationLink = applicationLink; }
	public String getApplicationInstructions() { return applicationInstructions; }
	public void setApplicationInstructions(String applicationInstructions) { this.applicationInstructions = applicationInstructions; }
}
