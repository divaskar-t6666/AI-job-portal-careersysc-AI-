package com.smartjobportal.job.dto;

import java.util.List;

public class RecruiterCandidateMatchDto {

	private String candidateName;
	private String currentJobTitle;
	private boolean hasActiveResume;
	private double matchPercentage;
	private List<String> matchedSkills;
	private List<String> missingSkills;

	public String getCandidateName() {
		return candidateName;
	}

	public void setCandidateName(String candidateName) {
		this.candidateName = candidateName;
	}

	public String getCurrentJobTitle() {
		return currentJobTitle;
	}

	public void setCurrentJobTitle(String currentJobTitle) {
		this.currentJobTitle = currentJobTitle;
	}

	public boolean isHasActiveResume() {
		return hasActiveResume;
	}

	public void setHasActiveResume(boolean hasActiveResume) {
		this.hasActiveResume = hasActiveResume;
	}

	public double getMatchPercentage() {
		return matchPercentage;
	}

	public void setMatchPercentage(double matchPercentage) {
		this.matchPercentage = matchPercentage;
	}

	public List<String> getMatchedSkills() {
		return matchedSkills;
	}

	public void setMatchedSkills(List<String> matchedSkills) {
		this.matchedSkills = matchedSkills;
	}

	public List<String> getMissingSkills() {
		return missingSkills;
	}

	public void setMissingSkills(List<String> missingSkills) {
		this.missingSkills = missingSkills;
	}
}
