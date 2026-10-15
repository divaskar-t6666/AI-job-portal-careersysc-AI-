package com.smartjobportal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartjobportal.entity.CandidateProfile;
import com.smartjobportal.entity.Resume;

public interface ResumeRepository extends JpaRepository<Resume, Integer> {

	List<Resume> findByCandidateProfile(CandidateProfile candidateProfile);

	List<Resume> findByCandidateProfileAndIsActive(CandidateProfile candidateProfile, Boolean isActive);

	Optional<Resume> findFirstByCandidateProfileAndIsActiveOrderByUpdatedAtDescResumeIdDesc(
			CandidateProfile candidateProfile, Boolean isActive);

	List<Resume> findByCandidateProfileCandidateProfileIdInAndIsActiveTrueOrderByUpdatedAtDescResumeIdDesc(
			List<Integer> candidateProfileIds);
}