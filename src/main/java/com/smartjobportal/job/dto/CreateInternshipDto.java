package com.smartjobportal.job.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public class CreateInternshipDto extends OpportunityRequestDto {

	@NotNull
	@PositiveOrZero
	private Integer experienceRequired;

	@NotNull
	private Boolean fresherEligible;

	@NotBlank
	@Pattern(regexp = "PAID|UNPAID")
	private String stipendType;

	@PositiveOrZero
	private BigDecimal stipendAmount;

	public Integer getExperienceRequired() { return experienceRequired; }
	public void setExperienceRequired(Integer experienceRequired) { this.experienceRequired = experienceRequired; }
	public Boolean getFresherEligible() { return fresherEligible; }
	public void setFresherEligible(Boolean fresherEligible) { this.fresherEligible = fresherEligible; }
	public String getStipendType() { return stipendType; }
	public void setStipendType(String stipendType) { this.stipendType = stipendType; }
	public BigDecimal getStipendAmount() { return stipendAmount; }
	public void setStipendAmount(BigDecimal stipendAmount) { this.stipendAmount = stipendAmount; }
}
