package com.smartjobportal.resumeparser.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class SkillParser {

	private static final List<String> SKILL_KEYWORDS = Arrays.asList("Java", "Spring", "Spring Boot", "Spring MVC",
			"Spring Security", "Hibernate", "JPA", "JDBC", "Servlet", "JSP", "MySQL", "PostgreSQL", "MongoDB", "React",
			"JavaScript", "TypeScript", "HTML", "CSS", "REST API", "Microservices", "Docker", "Kubernetes", "Git",
			"GitHub", "Maven", "Gradle", "AWS");

	public List<String> parse(String resumeText) {

		List<String> extractedSkills = new ArrayList<>();

		if (resumeText == null || resumeText.isBlank()) {
			return extractedSkills;
		}

		for (String skill : SKILL_KEYWORDS) {

			Pattern pattern = Pattern.compile("(?<![\\p{L}\\p{N}_])" + Pattern.quote(skill)
					+ "(?![\\p{L}\\p{N}_])", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

			if (pattern.matcher(resumeText).find()) {
				extractedSkills.add(skill);
			}
		}

		return extractedSkills;
	}

}