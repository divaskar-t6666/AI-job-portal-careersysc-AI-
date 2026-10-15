package com.smartjobportal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.smartjobportal.entity.Job;
import com.smartjobportal.entity.RecruiterProfile;

public interface JobRepository extends JpaRepository<Job, Integer>, JpaSpecificationExecutor<Job> {

	List<Job> findByJobStatus(String jobStatus);

	List<Job> findByRecruiterProfile(RecruiterProfile recruiterProfile);

}