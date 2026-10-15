package com.smartjobportal.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.smartjobportal.matching.util.SkillMatchingUtil;

class SkillMatchingUtilTest {

	@Test
	void normalizesCaseAndRemovesBlankAndDuplicateSkills() {
		var skills = SkillMatchingUtil.convertSkillsToSet(" Java, java, , SPRING BOOT ");

		assertEquals(List.of("java", "spring boot"), List.copyOf(skills));
	}

	@Test
	void emptyRequiredSkillsProduceZeroPercent() {
		assertTrue(SkillMatchingUtil.convertSkillsToSet(" , ").isEmpty());
		assertEquals(0.0, SkillMatchingUtil.calculatePercentage(0, 0));
	}
}
