package com.smartjobportal.careeragent.dto;

public class CareerAgentStatusDto {

	private boolean providerConfigured;
	private String providerName;
	private String dataDisclosure;

	public CareerAgentStatusDto() {
	}

	public CareerAgentStatusDto(boolean providerConfigured, String providerName, String dataDisclosure) {
		this.providerConfigured = providerConfigured;
		this.providerName = providerName;
		this.dataDisclosure = dataDisclosure;
	}

	public boolean isProviderConfigured() { return providerConfigured; }
	public void setProviderConfigured(boolean providerConfigured) { this.providerConfigured = providerConfigured; }
	public String getProviderName() { return providerName; }
	public void setProviderName(String providerName) { this.providerName = providerName; }
	public String getDataDisclosure() { return dataDisclosure; }
	public void setDataDisclosure(String dataDisclosure) { this.dataDisclosure = dataDisclosure; }
}
