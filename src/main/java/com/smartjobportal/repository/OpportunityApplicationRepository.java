package com.smartjobportal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartjobportal.entity.CandidateProfile;
import com.smartjobportal.entity.Opportunity;
import com.smartjobportal.entity.OpportunityApplication;

public interface OpportunityApplicationRepository extends JpaRepository<OpportunityApplication, Integer> {

	boolean existsByCandidateProfileAndOpportunity(CandidateProfile candidateProfile, Opportunity opportunity);

	Optional<OpportunityApplication> findByCandidateProfileAndOpportunity(
			CandidateProfile candidateProfile,
			Opportunity opportunity);

	List<OpportunityApplication> findByCandidateProfileOrderByAppliedAtDesc(CandidateProfile candidateProfile);
}
