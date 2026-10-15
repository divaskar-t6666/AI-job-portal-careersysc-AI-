package com.smartjobportal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartjobportal.entity.Opportunity;
import com.smartjobportal.entity.RecruiterProfile;

public interface OpportunityRepository extends JpaRepository<Opportunity, Integer> {

	List<Opportunity> findByRecruiterProfileOrderByCreatedAtDesc(RecruiterProfile recruiterProfile);
}
