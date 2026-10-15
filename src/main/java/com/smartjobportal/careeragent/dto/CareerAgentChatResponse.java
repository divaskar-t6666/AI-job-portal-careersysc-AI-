package com.smartjobportal.careeragent.dto;

import java.util.List;

public class CareerAgentChatResponse {

	private String answer;
	private String providerMode;
	private String providerNotice;
	private List<CareerAgentSuggestionDto> suggestions = List.of();

	public CareerAgentChatResponse() {
	}

	public CareerAgentChatResponse(String answer, String providerMode, String providerNotice,
			List<CareerAgentSuggestionDto> suggestions) {
		this.answer = answer;
		this.providerMode = providerMode;
		this.providerNotice = providerNotice;
		this.suggestions = suggestions;
	}

	public String getAnswer() { return answer; }
	public void setAnswer(String answer) { this.answer = answer; }
	public String getProviderMode() { return providerMode; }
	public void setProviderMode(String providerMode) { this.providerMode = providerMode; }
	public String getProviderNotice() { return providerNotice; }
	public void setProviderNotice(String providerNotice) { this.providerNotice = providerNotice; }
	public List<CareerAgentSuggestionDto> getSuggestions() { return suggestions; }
	public void setSuggestions(List<CareerAgentSuggestionDto> suggestions) { this.suggestions = suggestions; }
}
