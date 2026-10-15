package com.smartjobportal.job.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.smartjobportal.entity.CandidateProfile;
import com.smartjobportal.entity.Resume;
import com.smartjobportal.entity.User;
import com.smartjobportal.job.dto.ResumeUploadResponseDto;
import com.smartjobportal.job.service.ResumeUploadService;
import com.smartjobportal.repository.CandidateProfileRepository;
import com.smartjobportal.repository.ResumeRepository;
import com.smartjobportal.repository.UserRepository;

@Service
public class ResumeUploadServiceImpl implements ResumeUploadService {

	private static final int MAX_FILE_NAME_LENGTH = 100;

	@Value("${file.upload-dir}")
	private String uploadDir;

	@Autowired
	private ResumeRepository resumeRepository;

	@Autowired
	private CandidateProfileRepository candidateProfileRepository;

	@Autowired
	private UserRepository userRepository;

	/*
	 * Logged-in candidate (same lookup that ResumeServiceImpl uses)
	 */
	private CandidateProfile getLoggedInCandidate() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null) {

			throw new RuntimeException("User not authenticated");
		}

		String email = authentication.getName();

		User loggedInUser = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

		return candidateProfileRepository.findByUserUserId(loggedInUser.getUserId())
				.orElseThrow(() -> new RuntimeException("Candidate profile not found"));
	}

	/*
	 * Saves the file AND the resume record (resumes table) for the logged-in
	 * candidate. The new resume becomes the active one.
	 */
	@Override
	@Transactional
	public ResumeUploadResponseDto uploadResume(MultipartFile file) {

		/*
		 * Logged-in candidate - resolved first, so nothing is written to disk
		 * for a user who has no candidate profile
		 */
		CandidateProfile candidateProfile = getLoggedInCandidate();

		/*
		 * Empty file validation
		 */
		if (file == null || file.isEmpty()) {

			throw new RuntimeException("Please select a file");
		}

		/*
		 * File type validation - on the cleaned name, never on the raw client value
		 */
		String cleanFileName = cleanFileName(file.getOriginalFilename());

		String extension = getExtension(cleanFileName);

		if (!extension.equals("PDF") && !extension.equals("DOC") && !extension.equals("DOCX")) {

			throw new RuntimeException("Only PDF, DOC and DOCX files are allowed");
		}

		/*
		 * Server-generated unique file name
		 */
		String uniqueFileName = UUID.randomUUID() + "_" + cleanFileName;

		/*
		 * The file must end up inside the upload directory
		 */
		Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();

		Path targetFile = uploadRoot.resolve(uniqueFileName).normalize();

		if (!targetFile.startsWith(uploadRoot)) {

			throw new RuntimeException("Invalid file name");
		}

		/*
		 * Path stored in the database (same format as before)
		 */
		Path filePath = Paths.get(uploadDir).resolve(uniqueFileName);

		/*
		 * Save file
		 */
		try (InputStream inputStream = file.getInputStream()) {

			Files.createDirectories(uploadRoot);

			Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);

		} catch (IOException exception) {

			throw new RuntimeException("Failed to upload file", exception);
		}

		try {

			/*
			 * Previous resumes of this candidate become inactive
			 */
			for (Resume existingResume : resumeRepository.findByCandidateProfile(candidateProfile)) {

				existingResume.setIsActive(false);

				resumeRepository.save(existingResume);
			}

			/*
			 * Save the resume record for the logged-in candidate
			 */
			Resume resume = new Resume();

			resume.setCandidateProfile(candidateProfile);

			resume.setResumeTitle(buildTitle(cleanFileName));

			resume.setFileName(uniqueFileName);

			resume.setFilePath(filePath.toString());

			resume.setFileType(contentTypeFor(extension));

			resume.setFileSizeKb(BigDecimal.valueOf(file.getSize() / 1024.0).setScale(2, RoundingMode.HALF_UP));

			resume.setIsActive(true);

			Resume savedResume = resumeRepository.save(resume);

			ResumeUploadResponseDto response = new ResumeUploadResponseDto();

			response.setResumeId(savedResume.getResumeId());

			response.setFileName(uniqueFileName);

			response.setFilePath(filePath.toString());

			response.setMessage("Resume uploaded successfully");

			return response;

		} catch (RuntimeException exception) {

			/*
			 * Database problem: do not leave an orphan file behind
			 */
			try {

				Files.deleteIfExists(targetFile);

			} catch (IOException ignored) {

				// nothing more can be done here
			}

			throw exception;
		}
	}

	/*
	 * Client file name -> safe file name (letters, digits, dot, dash, underscore)
	 */
	private String cleanFileName(String originalFileName) {

		String name = originalFileName == null ? "" : originalFileName;

		// keep only the last path segment (some browsers send the full path)
		name = name.replace('\\', '/');

		name = name.substring(name.lastIndexOf('/') + 1);

		// allow only safe characters
		name = name.replaceAll("[^A-Za-z0-9._-]", "_");

		// no ".." sequences and no leading dots
		name = name.replaceAll("\\.{2,}", ".").replaceAll("^\\.+", "");

		if (name.isEmpty()) {

			name = "resume";
		}

		// keep it database-safe, but never cut off the extension
		if (name.length() > MAX_FILE_NAME_LENGTH) {

			int dot = name.lastIndexOf('.');

			String extensionPart = (dot >= 0 && name.length() - dot <= 10) ? name.substring(dot) : "";

			name = name.substring(0, MAX_FILE_NAME_LENGTH - extensionPart.length()) + extensionPart;
		}

		return name;
	}

	private String getExtension(String fileName) {

		int dot = fileName.lastIndexOf('.');

		if (dot < 0 || dot == fileName.length() - 1) {

			return "";
		}

		return fileName.substring(dot + 1).toUpperCase(Locale.ROOT);
	}

	private String buildTitle(String cleanFileName) {

		int dot = cleanFileName.lastIndexOf('.');

		String title = dot > 0 ? cleanFileName.substring(0, dot) : cleanFileName;

		title = title.replace('_', ' ').replaceAll("\\s+", " ").trim();

		return title.isEmpty() ? "My Resume" : title;
	}

	private String contentTypeFor(String extension) {

		switch (extension) {

		case "PDF":
			return "application/pdf";

		case "DOC":
			return "application/msword";

		default:
			return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
		}
	}

	@Override
	public byte[] downloadResume(String fileName) {

		try {

			Path filePath = Paths.get(uploadDir).resolve(fileName);

			return Files.readAllBytes(filePath);

		} catch (IOException exception) {

			throw new RuntimeException("File not found", exception);
		}
	}

	@Override
	public void deleteResumeFile(String fileName) {

		try {

			Path filePath = Paths.get(uploadDir).resolve(fileName);

			if (Files.exists(filePath)) {

				Files.delete(filePath);
			}

		} catch (IOException exception) {

			throw new RuntimeException("Failed to delete resume file", exception);
		}
	}

	@Override
	public String replaceResumeFile(MultipartFile newFile) {

		if (newFile == null || newFile.isEmpty()) {
			throw new RuntimeException("Please select a file");
		}

		String cleanFileName = cleanFileName(newFile.getOriginalFilename());
		String extension = getExtension(cleanFileName);

		if (!extension.equals("PDF") && !extension.equals("DOC") && !extension.equals("DOCX")) {
			throw new RuntimeException("Only PDF, DOC and DOCX files are allowed");
		}

		String uniqueFileName = UUID.randomUUID() + "_" + cleanFileName;
		Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
		Path targetFile = uploadRoot.resolve(uniqueFileName).normalize();

		if (!targetFile.startsWith(uploadRoot)) {
			throw new RuntimeException("Invalid file name");
		}

		try (InputStream inputStream = newFile.getInputStream()) {
			Files.createDirectories(uploadRoot);
			Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
			return uniqueFileName;
		} catch (IOException exception) {
			throw new RuntimeException("Failed to replace resume file", exception);
		}
	}
}