package com.smartjobportal.job.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateHackathonDto extends OpportunityRequestDto {

	@NotBlank
	@Size(max = 10000)
	private String problemStatement;

	@NotBlank
	@Pattern(regexp = "INDIVIDUAL|TEAM|BOTH")
	private String participationOptions;

	@NotNull
	@Future
	private LocalDateTime eventDate;

	@Future
	private LocalDateTime eventEndDate;

	@Size(max = 500)
	private String venueAddress;

	@Size(max = 5000)
	private String prizes;

	@Size(max = 5000)
	private String judgingCriteria;

	public String getProblemStatement() { return problemStatement; }
	public void setProblemStatement(String problemStatement) { this.problemStatement = problemStatement; }
	public String getParticipationOptions() { return participationOptions; }
	public void setParticipationOptions(String participationOptions) { this.participationOptions = participationOptions; }
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
}
