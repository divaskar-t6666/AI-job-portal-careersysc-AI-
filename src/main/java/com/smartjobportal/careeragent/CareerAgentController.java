package com.smartjobportal.careeragent;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartjobportal.careeragent.dto.CareerAgentChatRequest;
import com.smartjobportal.careeragent.dto.CareerAgentChatResponse;
import com.smartjobportal.careeragent.dto.CareerAgentStatusDto;
import com.smartjobportal.careeragent.dto.OpportunityReadinessDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/career-agent")
public class CareerAgentController {

	private final CareerAgentService careerAgentService;

	public CareerAgentController(CareerAgentService careerAgentService) {
		this.careerAgentService = careerAgentService;
	}

	@GetMapping("/status")
	public ResponseEntity<CareerAgentStatusDto> getStatus() {
		return ResponseEntity.ok(careerAgentService.getStatus());
	}

	@PostMapping("/chat")
	public ResponseEntity<CareerAgentChatResponse> chat(@Valid @RequestBody CareerAgentChatRequest request) {
		return ResponseEntity.ok(careerAgentService.chat(request));
	}

	@GetMapping("/analysis/{type}/{id}")
	public ResponseEntity<OpportunityReadinessDto> analyze(
			@PathVariable String type, @PathVariable @Positive Integer id) {
		return ResponseEntity.ok(careerAgentService.analyze(type, id));
	}
}
