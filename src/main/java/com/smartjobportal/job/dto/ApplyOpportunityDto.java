package com.smartjobportal.job.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ApplyOpportunityDto {

	@NotNull
	private Integer resumeId;

	@Size(max = 10000)
	private String coverLetter;

	public Integer getResumeId() { return resumeId; }
	public void setResumeId(Integer resumeId) { this.resumeId = resumeId; }
	public String getCoverLetter() { return coverLetter; }
	public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }
}
