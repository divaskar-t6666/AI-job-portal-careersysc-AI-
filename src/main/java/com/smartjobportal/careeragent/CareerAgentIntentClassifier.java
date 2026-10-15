package com.smartjobportal.careeragent;

import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class CareerAgentIntentClassifier {

	public boolean isPersonalizedCareerQuestion(String message) {
		String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
		return normalized.contains("resume")
				|| normalized.contains("this job")
				|| normalized.contains("job as")
				|| normalized.contains("job requirements")
				|| normalized.contains("job match")
				|| normalized.contains("which jobs")
				|| normalized.contains("what jobs")
				|| normalized.contains("jobs suit")
				|| normalized.contains("job recommendations")
				|| normalized.contains("recommend jobs")
				|| normalized.contains("recommend me a job")
				|| normalized.contains("recommend opportunities")
				|| normalized.contains("recommend internship")
				|| normalized.contains("career advice")
				|| normalized.contains("career path")
				|| normalized.contains("roles for me")
				|| normalized.contains("suit my skills")
				|| normalized.contains("based on my current skills")
				|| normalized.contains("based on my skills")
				|| normalized.contains("given my skills")
				|| normalized.contains("my strongest skills")
				|| normalized.contains("skills am i missing")
				|| normalized.contains("my skill")
				|| normalized.contains("internship")
				|| normalized.contains("hackathon")
				|| normalized.contains("opportunity")
				|| normalized.contains("eligib")
				|| normalized.contains("apply")
				|| normalized.contains("fresher")
				|| normalized.contains("missing skill")
				|| normalized.contains("match score")
				|| normalized.contains("my skills")
				|| normalized.contains("my experience")
				|| normalized.contains("my qualification")
				|| normalized.contains("requirements");
	}
}
