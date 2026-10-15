package com.smartjobportal.resumeparser.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartjobportal.entity.CandidateEducation;
import com.smartjobportal.entity.CandidateExperience;
import com.smartjobportal.entity.CandidateProfile;
import com.smartjobportal.entity.CandidateSkill;
import com.smartjobportal.entity.Resume;
import com.smartjobportal.repository.CandidateEducationRepository;
import com.smartjobportal.repository.CandidateExperienceRepository;
import com.smartjobportal.repository.CandidateProfileRepository;
import com.smartjobportal.repository.CandidateSkillRepository;
import com.smartjobportal.repository.ResumeRepository;
import com.smartjobportal.repository.UserRepository;
import com.smartjobportal.matching.util.SkillMatchingUtil;
import com.smartjobportal.resumeparser.exception.ResumeParsingException;
import com.smartjobportal.resumeparser.dto.CandidateInfoDto;
import com.smartjobportal.resumeparser.dto.EducationDto;
import com.smartjobportal.resumeparser.dto.ExperienceDto;
import com.smartjobportal.resumeparser.dto.ResumeParserResultDto;
import com.smartjobportal.resumeparser.extractor.TextExtractionService;
import com.smartjobportal.resumeparser.parser.CandidateInfoParser;
import com.smartjobportal.resumeparser.parser.EducationParser;
import com.smartjobportal.resumeparser.parser.ExperienceParser;
import com.smartjobportal.resumeparser.parser.SkillParser;
import com.smartjobportal.resumeparser.service.ResumeParserService;

@Service
public class ResumeParserServiceImpl implements ResumeParserService {

	private static final Logger log = LoggerFactory.getLogger(ResumeParserServiceImpl.class);

	@Autowired
	private ResumeRepository resumeRepository;

	@Autowired
	private CandidateProfileRepository candidateProfileRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TextExtractionService textExtractionService;

	@Autowired
	private CandidateInfoParser candidateInfoParser;

	@Autowired
	private SkillParser skillParser;

	@Autowired
	private EducationParser educationParser;

	@Autowired
	private ExperienceParser experienceParser;

	@Autowired
	private CandidateSkillRepository candidateSkillRepository;

	@Autowired
	private CandidateEducationRepository candidateEducationRepository;

	@Autowired
	private CandidateExperienceRepository candidateExperienceRepository;

	@Override
	@Transactional
	public ResumeParserResultDto parseResume(Integer resumeId) {

		try {

			Resume resume = resumeRepository.findById(resumeId)
					.orElseThrow(() -> new RuntimeException("Resume not found"));

			CandidateProfile candidateProfile = resume.getCandidateProfile();

			assertLoggedInCandidateOwnsResume(candidateProfile, resumeId);

			File resumeFile = new File(resume.getFilePath());

			String resumeText = textExtractionService.extractText(resumeFile);

			if (resumeText == null || resumeText.isBlank()) {
				throw new ResumeParsingException(
						"No readable text was found. Upload a text-based PDF or DOCX resume.");
			}

			CandidateInfoDto candidateInfo = candidateInfoParser.parse(resumeText);

			List<String> extractedSkills = skillParser.parse(resumeText);
			List<CandidateSkill> previousProfileSkills = candidateSkillRepository
					.findByCandidateProfile(candidateProfile);
			List<String> manuallyAddedSkills = findManuallyAddedSkills(resume.getParsedSkills(),
					previousProfileSkills);
			List<String> skills = mergeSkills(extractedSkills, manuallyAddedSkills);

			List<EducationDto> education = educationParser.parse(resumeText);

			List<ExperienceDto> experience = experienceParser.parse(resumeText);

			resume.setParsedSkills(String.join(", ", skills));

			resume.setParsedExperience(formatExperience(experience));

			resume.setParsedEducation(formatEducation(education));

			resumeRepository.save(resume);

			candidateSkillRepository.deleteByCandidateProfile(candidateProfile);

			candidateEducationRepository.deleteByCandidateProfile(candidateProfile);

			candidateExperienceRepository.deleteByCandidateProfile(candidateProfile);

			saveSkills(candidateProfile, skills);

			saveEducation(candidateProfile, education);

			saveExperience(candidateProfile, experience);

			ResumeParserResultDto result = new ResumeParserResultDto();

			result.setCandidateInfo(candidateInfo);

			result.setSkills(skills);

			result.setEducation(education);

			result.setExperience(experience);

			return result;

		} catch (IOException | IllegalArgumentException exception) {
			log.error("Failed to extract text from resumeId={}", resumeId, exception);
			String message = exception instanceof IllegalArgumentException
					? "Automatic parsing supports PDF and DOCX files only."
					: "Could not read the uploaded resume. Please upload a valid PDF or DOCX file.";
			throw new ResumeParsingException(message, exception);
		}
	}

	private List<String> findManuallyAddedSkills(String parsedSkills, List<CandidateSkill> previousProfileSkills) {

		Set<String> previouslyExtractedSkills = previousProfileSkills.stream()
				.map(CandidateSkill::getSkillName)
				.map(SkillMatchingUtil::normalizeSkill)
				.filter(skill -> !skill.isEmpty())
				.collect(Collectors.toSet());

		if (parsedSkills == null || parsedSkills.isBlank()) {
			return List.of();
		}

		return Arrays.stream(parsedSkills.split(","))
				.map(String::trim)
				.filter(skill -> !skill.isEmpty())
				.filter(skill -> !previouslyExtractedSkills.contains(SkillMatchingUtil.normalizeSkill(skill)))
				.toList();
	}

	private List<String> mergeSkills(List<String> extractedSkills, List<String> manuallyAddedSkills) {

		Map<String, String> uniqueSkills = new LinkedHashMap<>();
		Stream.concat(extractedSkills.stream(), manuallyAddedSkills.stream())
				.map(String::trim)
				.filter(skill -> !skill.isEmpty())
				.forEach(skill -> uniqueSkills.putIfAbsent(SkillMatchingUtil.normalizeSkill(skill), skill));

		return new ArrayList<>(uniqueSkills.values());
	}

	private String formatExperience(List<ExperienceDto> experienceList) {

		return experienceList.stream()
				.map(dto -> Stream.of(dto.getDesignation(), dto.getCompanyName(), dto.getDuration())
						.filter(value -> value != null && !value.isBlank())
						.collect(Collectors.joining(" | ")))
				.filter(value -> !value.isBlank())
				.collect(Collectors.joining("\n"));
	}

	private String formatEducation(List<EducationDto> educationList) {

		return educationList.stream()
				.map(dto -> Stream.of(dto.getDegree(), dto.getInstitution(), dto.getYear())
						.filter(value -> value != null && !value.isBlank())
						.collect(Collectors.joining(" | ")))
				.filter(value -> !value.isBlank())
				.collect(Collectors.joining("\n"));
	}

	private void assertLoggedInCandidateOwnsResume(CandidateProfile resumeOwner, Integer resumeId) {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getAuthorities().stream()
						.noneMatch(authority -> "ROLE_CANDIDATE".equals(authority.getAuthority()))) {
			log.warn("Resume parser denied for resumeId={}: missing authenticated candidate authority", resumeId);
			throw new AccessDeniedException("Candidate authentication is required");
		}

		com.smartjobportal.entity.User user = userRepository.findByEmail(authentication.getName()).orElse(null);
		if (user == null) {
			log.warn("Resume parser denied for resumeId={}: authenticated principal did not resolve to a user record", resumeId);
			throw new AccessDeniedException("Candidate account was not found");
		}

		CandidateProfile loggedInCandidate = candidateProfileRepository
				.findByUserUserId(user.getUserId()).orElse(null);

		boolean ownsResume = loggedInCandidate != null
				&& resumeOwner != null
				&& loggedInCandidate.getCandidateProfileId() != null
				&& loggedInCandidate.getCandidateProfileId().equals(resumeOwner.getCandidateProfileId());

		if (!ownsResume) {
			log.warn(
					"Resume parser denied for resumeId={}: ownerProfileId={}, authenticatedCandidateProfileId={}",
					resumeId,
					resumeOwner == null ? null : resumeOwner.getCandidateProfileId(),
					loggedInCandidate == null ? null : loggedInCandidate.getCandidateProfileId());
			throw new AccessDeniedException("You cannot parse another candidate's resume");
		}
	}

	private void saveSkills(CandidateProfile candidateProfile, List<String> skills) {

		for (String skill : skills) {

			CandidateSkill candidateSkill = new CandidateSkill();

			candidateSkill.setCandidateProfile(candidateProfile);

			candidateSkill.setSkillName(skill);

			candidateSkillRepository.save(candidateSkill);
		}
	}

	private void saveEducation(CandidateProfile candidateProfile, List<EducationDto> educationList) {

		for (EducationDto dto : educationList) {

			CandidateEducation education = new CandidateEducation();

			education.setCandidateProfile(candidateProfile);

			education.setDegree(dto.getDegree());

			education.setInstitutionName(dto.getInstitution());

			education.setUniversityName(dto.getInstitution());

			try {

				Integer year = Integer.parseInt(dto.getYear());

				education.setStartYear(year);

				education.setEndYear(year);

			} catch (Exception ignored) {

				education.setStartYear(2000);

				education.setEndYear(2000);
			}

			candidateEducationRepository.save(education);
		}
	}

	private void saveExperience(CandidateProfile candidateProfile, List<ExperienceDto> experienceList) {

		for (ExperienceDto dto : experienceList) {

			CandidateExperience experience = new CandidateExperience();

			experience.setCandidateProfile(candidateProfile);

			experience.setCompanyName(dto.getCompanyName());

			experience.setJobTitle(dto.getDesignation());

			experience.setStartDate(java.time.LocalDate.of(2000, 1, 1));

			experience.setEndDate(java.time.LocalDate.of(2000, 1, 1));

			candidateExperienceRepository.save(experience);
		}
	}
}