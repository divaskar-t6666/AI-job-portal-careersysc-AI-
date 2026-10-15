package com.smartjobportal.careeragent.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CareerAgentChatRequest {

	@NotBlank
	@Size(max = 1200)
	private String message;

	@Pattern(regexp = "(?i)JOB|INTERNSHIP|HACKATHON")
	private String opportunityType;

	@Positive
	private Integer opportunityId;

	@Size(max = 12)
	@Valid
	private List<CareerAgentChatTurn> history = new ArrayList<>();

	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }
	public String getOpportunityType() { return opportunityType; }
	public void setOpportunityType(String opportunityType) { this.opportunityType = opportunityType; }
	public Integer getOpportunityId() { return opportunityId; }
	public void setOpportunityId(Integer opportunityId) { this.opportunityId = opportunityId; }
	public List<CareerAgentChatTurn> getHistory() { return history; }
	public void setHistory(List<CareerAgentChatTurn> history) { this.history = history; }

	public static class CareerAgentChatTurn {
		@Pattern(regexp = "user|assistant")
		private String role;

		@NotBlank
		@Size(max = 1200)
		private String content;

		public String getRole() { return role; }
		public void setRole(String role) { this.role = role; }
		public String getContent() { return content; }
		public void setContent(String content) { this.content = content; }
	}
}
