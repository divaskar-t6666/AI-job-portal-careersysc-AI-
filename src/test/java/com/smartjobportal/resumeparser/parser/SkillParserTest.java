package com.smartjobportal.resumeparser.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class SkillParserTest {

	private final SkillParser skillParser = new SkillParser();

	@Test
	void extractsWholeSkillNamesWithoutSubstringMatchesOrDuplicates() {
		assertEquals(
				List.of("Spring", "Spring Boot", "JavaScript"),
				skillParser.parse("JavaScript, Spring Boot, SPRING BOOT"));
	}

	@Test
	void nullOrBlankResumeTextHasNoExtractedSkills() {
		assertEquals(List.of(), skillParser.parse(null));
		assertEquals(List.of(), skillParser.parse("   "));
	}
}
