package com.smartjobportal.job.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.smartjobportal.entity.CandidateProfile;
import com.smartjobportal.entity.Resume;
import com.smartjobportal.entity.User;
import com.smartjobportal.exception.ResourceNotFoundException;

import com.smartjobportal.job.dto.CreateResumeDto;
import com.smartjobportal.job.dto.ResumeResponseDto;
import com.smartjobportal.job.dto.UpdateResumeDto;
import com.smartjobportal.job.mapper.ResumeMapper;
import com.smartjobportal.job.service.ResumeService;
import com.smartjobportal.job.service.ResumeUploadService;
import com.smartjobportal.repository.CandidateProfileRepository;
import com.smartjobportal.repository.JobApplicationRepository;
import com.smartjobportal.repository.JobMatchRepository;
import com.smartjobportal.repository.ResumeRepository;
import com.smartjobportal.repository.UserRepository;
import com.smartjobportal.job.service.JobMatchingService;
import com.smartjobportal.resumeparser.service.ResumeParserService;

@Service
public class ResumeServiceImpl implements ResumeService {

	private static final Logger log = LoggerFactory.getLogger(ResumeServiceImpl.class);

	@Autowired
	private ResumeRepository resumeRepository;

	@Autowired
	private CandidateProfileRepository candidateProfileRepository;

	@Autowired
	private JobMatchRepository jobMatchRepository;
	
	@Autowired
	private JobMatchingService jobMatchingService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JobApplicationRepository jobApplicationRepository;

	@Autowired
	private ResumeUploadService resumeUploadService;

	@Autowired
	private ResumeParserService resumeParserService;

	private CandidateProfile getLoggedInCandidate() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		String email = authentication.getName();

		User loggedInUser = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

		return candidateProfileRepository.findByUserUserId(loggedInUser.getUserId())
				.orElseThrow(() -> new RuntimeException("Candidate profile not found"));
	}

	/*
	 * A request may only REFER to a file that belongs to one of THIS candidate's
	 * resumes (the upload step creates the resume record and the file name).
	 * File names / paths sent by the browser are never stored or trusted.
	 * Returns null when no file name was sent.
	 */
	private Resume findOwnResumeByFileName(List<Resume> candidateResumes, String fileName) {

		if (fileName == null || fileName.isBlank()) {

			return null;
		}

		for (Resume candidateResume : candidateResumes) {

			if (fileName.equals(candidateResume.getFileName())) {

				return candidateResume;
			}
		}

		throw new RuntimeException("Resume file not found. Please upload the file first.");
	}

	@Override
	@Transactional
	public ResumeResponseDto createResume(CreateResumeDto createResumeDto) {

		CandidateProfile candidateProfile = getLoggedInCandidate();

		List<Resume> existingResumes = resumeRepository.findByCandidateProfile(candidateProfile);

		/*
		 * The upload step already saved the file and its resume record.
		 * Reuse that record (no duplicate row). fileName / filePath / fileType /
		 * fileSizeKb from the request body are never stored.
		 */
		Resume resume = findOwnResumeByFileName(existingResumes, createResumeDto.getFileName());

		if (resume == null) {

			resume = new Resume();

			resume.setCandidateProfile(candidateProfile);
		}

		// Deactivate the candidate's other resumes
		for (Resume existingResume : existingResumes) {

			if (existingResume.getResumeId() != null && existingResume.getResumeId().equals(resume.getResumeId())) {

				continue;
			}

			existingResume.setIsActive(false);

			resumeRepository.save(existingResume);
		}

		if (createResumeDto.getResumeTitle() != null && !createResumeDto.getResumeTitle().isBlank()) {

			resume.setResumeTitle(createResumeDto.getResumeTitle());
		}

		resume.setParsedSkills(createResumeDto.getParsedSkills());

		resume.setParsedExperience(createResumeDto.getParsedExperience());

		resume.setParsedEducation(createResumeDto.getParsedEducation());

		// New resume becomes active
		resume.setIsActive(true);

		Resume savedResume = resumeRepository.save(resume);

		return ResumeMapper.convertToDto(savedResume);
	}

	@Override
	public List<ResumeResponseDto> getMyResumes() {

		CandidateProfile candidateProfile = getLoggedInCandidate();

		List<Resume> resumes = resumeRepository.findByCandidateProfile(candidateProfile);

		return resumes.stream().map(ResumeMapper::convertToDto).collect(Collectors.toList());
	}

	@Override
	public ResumeResponseDto getResumeById(Integer resumeId) {

		CandidateProfile candidateProfile = getLoggedInCandidate();

		Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> new RuntimeException("Resume not found"));

		if (!resume.getCandidateProfile().getCandidateProfileId().equals(candidateProfile.getCandidateProfileId())) {

			throw new RuntimeException("You cannot access another candidate resume");
		}

		return ResumeMapper.convertToDto(resume);
	}

	@Override
	@Transactional
	public ResumeResponseDto updateResume(Integer resumeId, UpdateResumeDto updateResumeDto) {

		CandidateProfile candidateProfile = getLoggedInCandidate();

		Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> new RuntimeException("Resume not found"));

		if (!resume.getCandidateProfile().getCandidateProfileId().equals(candidateProfile.getCandidateProfileId())) {

			throw new RuntimeException("You cannot update another candidate resume");
		}

		/*
		 * If the form uploaded a new file, the upload step has already saved it as
		 * one of this candidate's resumes - apply the edits to that resume.
		 * File name / path / type / size from the request body are never stored.
		 */
		String requestedFileName = updateResumeDto.getFileName();

		if (requestedFileName != null && !requestedFileName.isBlank()
				&& !requestedFileName.equals(resume.getFileName())) {

			resume = findOwnResumeByFileName(resumeRepository.findByCandidateProfile(candidateProfile),
					requestedFileName);
		}

		if (updateResumeDto.getResumeTitle() != null && !updateResumeDto.getResumeTitle().isBlank()) {

			resume.setResumeTitle(updateResumeDto.getResumeTitle());
		}

		resume.setParsedSkills(updateResumeDto.getParsedSkills());

		resume.setParsedExperience(updateResumeDto.getParsedExperience());

		resume.setParsedEducation(updateResumeDto.getParsedEducation());

		for (Resume candidateResume : resumeRepository.findByCandidateProfile(candidateProfile)) {

			if (!candidateResume.getResumeId().equals(resume.getResumeId())) {
				candidateResume.setIsActive(false);
			}
		}

		resume.setIsActive(true);

		Resume updatedResume = resumeRepository.save(resume);

		/*
		 * Remove old AI cache
		 */
		jobMatchRepository.deleteAll(
		        jobMatchRepository.findByResume(updatedResume)
		);

		/*
		 * Generate fresh matches for every OPEN job
		 */
		jobMatchingService.getRecommendedJobs(updatedResume.getResumeId());

		return ResumeMapper.convertToDto(updatedResume);
	}

	@Override
	public String deleteResume(Integer resumeId) {

		CandidateProfile candidateProfile = getLoggedInCandidate();

		Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> new RuntimeException("Resume not found"));

		if (!resume.getCandidateProfile().getCandidateProfileId().equals(candidateProfile.getCandidateProfileId())) {

			throw new RuntimeException("You cannot delete another candidate resume");
		}

		boolean usedInApplication = jobApplicationRepository.existsByResume(resume);

		if (usedInApplication) {

			throw new RuntimeException("Resume is already used in job applications");
		}

		resumeUploadService.deleteResumeFile(resume.getFileName());

		resumeRepository.delete(resume);

		return "Resume deleted successfully";
	}

	@Override
	public ResumeResponseDto activateResume(Integer resumeId) {

		CandidateProfile candidateProfile = getLoggedInCandidate();

		Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> new RuntimeException("Resume not found"));

		if (!resume.getCandidateProfile().getCandidateProfileId().equals(candidateProfile.getCandidateProfileId())) {

			throw new RuntimeException("You cannot activate another candidate resume");
		}

		List<Resume> resumes = resumeRepository.findByCandidateProfile(candidateProfile);

		for (Resume existingResume : resumes) {

			existingResume.setIsActive(false);

			resumeRepository.save(existingResume);
		}

		resume.setIsActive(true);

		Resume updatedResume = resumeRepository.save(resume);

		return ResumeMapper.convertToDto(updatedResume);
	}

	@Override
	public ResumeResponseDto getActiveResume() {

		CandidateProfile candidateProfile = getLoggedInCandidate();

		List<Resume> activeResumes = resumeRepository.findByCandidateProfileAndIsActive(candidateProfile, true);

		if (activeResumes.isEmpty()) {

			throw new ResourceNotFoundException("No active resume found");
		}

		return ResumeMapper.convertToDto(activeResumes.get(0));
	}

	@Override
	@Transactional
	public ResumeResponseDto replaceResumeFile(Integer resumeId, MultipartFile file) {

		CandidateProfile candidateProfile = getLoggedInCandidate();

		Resume resume = resumeRepository.findById(resumeId).orElseThrow(() -> new RuntimeException("Resume not found"));

		if (!resume.getCandidateProfile().getCandidateProfileId().equals(candidateProfile.getCandidateProfileId())) {

			throw new RuntimeException("You cannot update another candidate resume");
		}

		String previousFileName = resume.getFileName();
		String previousFilePath = resume.getFilePath();
		String previousFileType = resume.getFileType();
		java.math.BigDecimal previousFileSizeKb = resume.getFileSizeKb();
		String previousParsedSkills = resume.getParsedSkills();
		String previousParsedExperience = resume.getParsedExperience();
		String previousParsedEducation = resume.getParsedEducation();
		Boolean previouslyActive = resume.getIsActive();
		String newFileName = resumeUploadService.replaceResumeFile(file);

		try {
			resume.setFileName(newFileName);
			resume.setFilePath(previousFilePath == null
					? java.nio.file.Paths.get("uploads", newFileName).toString()
					: java.nio.file.Paths.get(previousFilePath).resolveSibling(newFileName).toString());
			resume.setFileType(contentTypeFor(newFileName));
			resume.setFileSizeKb(java.math.BigDecimal.valueOf(file.getSize() / 1024.0));
			resume.setIsActive(true);

			resumeRepository.save(resume);

			for (Resume candidateResume : resumeRepository.findByCandidateProfile(candidateProfile)) {
				if (!candidateResume.getResumeId().equals(resume.getResumeId())) {
					candidateResume.setIsActive(false);
				}
			}

			resumeParserService.parseResume(resumeId);

			jobMatchRepository.deleteAll(jobMatchRepository.findByResume(resume));
			jobMatchingService.getRecommendedJobs(resumeId);

			Resume updatedResume = resumeRepository.save(resume);
			removePreviousFileAfterCommit(previousFileName);

			return ResumeMapper.convertToDto(updatedResume);
		} catch (RuntimeException exception) {
			resume.setFileName(previousFileName);
			resume.setFilePath(previousFilePath);
			resume.setFileType(previousFileType);
			resume.setFileSizeKb(previousFileSizeKb);
			resume.setParsedSkills(previousParsedSkills);
			resume.setParsedExperience(previousParsedExperience);
			resume.setParsedEducation(previousParsedEducation);
			resume.setIsActive(previouslyActive);
			try {
				resumeUploadService.deleteResumeFile(newFileName);
			} catch (RuntimeException cleanupException) {
				exception.addSuppressed(cleanupException);
			}
			throw exception;
		}
	}

	private void removePreviousFileAfterCommit(String fileName) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			resumeUploadService.deleteResumeFile(fileName);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					resumeUploadService.deleteResumeFile(fileName);
				} catch (RuntimeException exception) {
					log.error("Replacement resume saved, but the previous file could not be deleted: {}", fileName,
							exception);
				}
			}
		});
	}

	private String contentTypeFor(String fileName) {
		String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase(java.util.Locale.ROOT);

		return switch (extension) {
		case "PDF" -> "application/pdf";
		case "DOC" -> "application/msword";
		default -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
		};
	}
}