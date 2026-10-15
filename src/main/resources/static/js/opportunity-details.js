(() => {
	"use strict";

	const params = new URLSearchParams(window.location.search);
	const type = (params.get("type") || "").toUpperCase();
	const idText = params.get("id") || "";
	const id = Number(idText);
	const detailsCard = document.getElementById("opportunityDetails");
	const loading = document.getElementById("detailsLoading");
	const errorPanel = document.getElementById("detailsError");
	const applyButton = document.getElementById("applyNowButton");
	const analysisResult = document.getElementById("careerAnalysisResult");
	const analysisLoading = document.getElementById("careerAnalysisLoading");
	const analysisError = document.getElementById("careerAnalysisError");
	const analysisButtons = [
		document.getElementById("analyzeSkillGapButton"),
		document.getElementById("checkReadinessButton")
	];
	let opportunity = null;
	let applicationSubmitted = false;

	function setText(idValue, value) {
		document.getElementById(idValue).textContent =
			value == null || value === "" ? "Not specified" : String(value);
	}

	function addMeta(label, value) {
		if (value == null || value === "") {
			return;
		}
		const item = document.createElement("div");
		item.className = "meta-item";
		const title = document.createElement("strong");
		title.textContent = label;
		const text = document.createElement("span");
		text.textContent = String(value);
		item.append(title, text);
		document.getElementById("opportunityMeta").appendChild(item);
	}

	function addSpecific(label, value) {
		if (value == null || value === "") {
			return;
		}
		const item = document.createElement("div");
		item.className = "meta-item";
		const title = document.createElement("strong");
		title.textContent = label;
		const text = document.createElement("span");
		text.textContent = String(value);
		item.append(title, text);
		document.getElementById("typeSpecificDetails").appendChild(item);
	}

	function formatDate(value) {
		if (!value) {
			return "Not specified";
		}
		const date = new Date(value);
		return Number.isNaN(date.getTime()) ? "Not specified" : date.toLocaleString();
	}

	function isAvailable(item) {
		const status = String(item.status || "").toUpperCase();
		const deadline = item.applicationDeadline ? new Date(item.applicationDeadline) : null;
		return ["OPEN", "ACTIVE", "PUBLISHED"].includes(status)
			&& (!deadline || deadline.getTime() >= Date.now());
	}

	function showToast(message) {
		const toast = document.getElementById("detailsToast");
		toast.textContent = message;
		toast.classList.add("visible");
		window.setTimeout(() => toast.classList.remove("visible"), 4000);
	}

	function showError(message) {
		loading.hidden = true;
		errorPanel.textContent = message;
		errorPanel.hidden = false;
	}

	async function request(url, options = {}) {
		const token = localStorage.getItem("token");
		if (!token) {
			window.location.replace("/html/login.html");
			throw new Error("Please sign in to view this opportunity.");
		}
		const response = await fetch(url, {
			...options,
			headers: {
				"Content-Type": "application/json",
				Authorization: `Bearer ${token}`,
				...(options.headers || {})
			}
		});
		if (!response.ok) {
			if (response.status === 401) {
				localStorage.removeItem("token");
				window.location.replace("/html/login.html");
				throw new Error("Your session expired. Please sign in again.");
			}
			if (response.status === 403) {
				throw new Error("You are not authorized to access this opportunity.");
			}
			if (response.status === 404) {
				throw new Error("This opportunity could not be found.");
			}
			let detail = "";
			try {
				const body = await response.json();
				detail = body.message || body.detail || "";
			} catch (error) {
				detail = "";
			}
			throw new Error(detail || `Request failed (${response.status}).`);
		}
		if (response.status === 204) {
			return null;
		}
		return response.json();
	}

	function render(item) {
		opportunity = item;
		loading.hidden = true;
		detailsCard.hidden = false;
		document.title = `${item.title || "Opportunity"} | CareerSync AI`;
		setText("opportunityType", item.opportunityType);
		setText("opportunityTitle", item.title);
		setText("companyName", item.companyName || "Company / organizer not specified");
		setText("opportunityDescription", item.description || "Not specified");
		setText("requiredSkills", item.requiredSkills || "Not specified");

		addMeta("Location", item.location);
		addMeta("Work mode", item.workMode || item.employmentType);
		addMeta("Application / registration deadline", formatDate(item.applicationDeadline));
		addMeta("Status", item.status || "Not specified");
		addMeta("Posted", formatDate(item.createdAt));
		if (item.experienceRequired != null || item.opportunityType === "JOB" || item.opportunityType === "INTERNSHIP") {
			addMeta("Experience required", item.experienceRequired == null
				? "Not specified"
				: `${item.experienceRequired} years`);
		}
		if (item.opportunityType === "JOB") {
			addMeta("Employment type", item.employmentType);
			addMeta("Salary", item.salaryPackage);
		}

		if (item.eligibilityCriteria) {
			setText("eligibilityCriteria", item.eligibilityCriteria);
			document.getElementById("eligibilitySection").hidden = false;
		}

		addCompanyDetail("Company phone", item.companyPhone);
		addCompanyDetail("Company website", item.companyWebsite, true);
		addCompanyDetail("Company address", item.companyAddress);
		document.getElementById("companySection").hidden =
			document.getElementById("companyDetails").childElementCount === 0;

		if (item.opportunityType === "INTERNSHIP") {
			addSpecific("Duration", item.duration);
			addSpecific("Stipend", item.stipendType === "UNPAID"
				? "Unpaid"
				: item.stipendAmount != null
					? `${item.stipendAmount} (${item.stipendType || "Paid"})`
					: item.stipendType);
			addSpecific("Freshers eligible", item.fresherEligible == null
				? null
				: item.fresherEligible ? "Yes" : "No");
		} else if (item.opportunityType === "HACKATHON") {
			setText("problemStatement", item.problemStatement);
			document.getElementById("problemSection").hidden = !item.problemStatement;
			addSpecific("Duration", item.duration);
			addSpecific("Participation", item.participationOptions);
			addSpecific("Event date", formatDate(item.eventDate));
			addSpecific("Event end date", formatDate(item.eventEndDate));
			addSpecific("Venue / address", item.venueAddress);
			addSpecific("Prizes", item.prizes);
			addSpecific("Judging criteria", item.judgingCriteria);
		}

		if (document.getElementById("typeSpecificDetails").childElementCount > 0) {
			document.getElementById("typeSpecificSection").hidden = false;
		}
		if (item.applicationInstructions) {
			setText("applicationInstructions", item.applicationInstructions);
			document.getElementById("instructionsSection").hidden = false;
		}

		const available = isAvailable(item);
		const externalUrl = safeExternalUrl(item.applicationLink);
		if (externalUrl && available) {
			const container = document.getElementById("registrationLinkContainer");
			const anchor = document.createElement("a");
			anchor.href = externalUrl.href;
			anchor.target = "_blank";
			anchor.rel = "noopener noreferrer";
			anchor.textContent = item.opportunityType === "HACKATHON"
				? "Register Now"
				: "Open application link";
			container.appendChild(anchor);
			container.hidden = false;
		}

		const hasExternalRegistration = item.opportunityType === "HACKATHON" && externalUrl;
		applyButton.textContent = item.opportunityType === "HACKATHON"
			? "Register Now"
			: "Apply Now";
		applyButton.hidden = Boolean(hasExternalRegistration);
		if (!available) {
			applyButton.disabled = true;
			applyButton.hidden = false;
			applyButton.textContent = item.opportunityType === "HACKATHON"
				? "Registration Closed"
				: "Applications Closed";
			const message = document.getElementById("availabilityMessage");
			message.textContent = "This opportunity is closed or its application deadline has passed.";
			message.hidden = false;
		} else if (!hasExternalRegistration) {
			applyButton.hidden = false;
		}
	}

	function addCompanyDetail(label, value, isUrl = false) {
		if (!value) {
			return;
		}
		const item = document.createElement("div");
		item.className = "meta-item";
		const title = document.createElement("strong");
		title.textContent = label;
		const content = document.createElement("span");
		if (isUrl) {
			const url = safeExternalUrl(value);
			if (!url) {
				return;
			}
			const link = document.createElement("a");
			link.href = url.href;
			link.target = "_blank";
			link.rel = "noopener noreferrer";
			link.textContent = value;
			content.appendChild(link);
		} else {
			content.textContent = String(value);
		}
		item.append(title, content);
		document.getElementById("companyDetails").appendChild(item);
	}

	function renderAnalysis(result) {
		analysisResult.replaceChildren();
		const heading = document.createElement("h3");
		heading.textContent = result.eligibilityAssessment;
		analysisResult.appendChild(heading);
		const summary = document.createElement("p");
		summary.textContent = result.resumeAvailable
			? result.resumeParsed
				? "Compared against your active parsed resume."
				: "Your active resume has no saved parsed skills. Verify or re-upload it before relying on this analysis."
			: "No active resume was found. Upload a resume to compare it with this opportunity.";
		analysisResult.appendChild(summary);
		addAnalysisList("Matched skills", result.matchedSkills);
		addAnalysisList("Missing required skills", result.missingRequiredSkills);
		addAnalysisList("Missing preferred skills", result.missingPreferredSkills);
		addAnalysisList("Requirements appearing satisfied", result.requirementsSatisfied);
		addAnalysisList("Not demonstrated by available resume evidence", result.requirementsNotDemonstrated);
		addAnalysisList("Information to confirm", result.informationToConfirm);
		addAnalysisList("Recommended next steps", result.recommendedNextSteps);
		addAnalysisList("Project requirements", result.projectRequirements);
		const experience = document.createElement("p");
		experience.textContent = `Experience: ${result.experienceAssessment}`;
		analysisResult.appendChild(experience);
		analysisResult.hidden = false;
	}

	function addAnalysisList(title, values) {
		if (!Array.isArray(values) || values.length === 0) {
			return;
		}
		const section = document.createElement("section");
		const heading = document.createElement("h4");
		heading.textContent = title;
		const list = document.createElement("ul");
		values.forEach(value => {
			const item = document.createElement("li");
			item.textContent = value;
			list.appendChild(item);
		});
		section.append(heading, list);
		analysisResult.appendChild(section);
	}

	async function loadCareerAnalysis() {
		analysisLoading.hidden = false;
		analysisError.hidden = true;
		analysisButtons.forEach(button => { button.disabled = true; });
		try {
			const result = await request(
				`/api/career-agent/analysis/${encodeURIComponent(type)}/${encodeURIComponent(id)}`
			);
			renderAnalysis(result);
		} catch (error) {
			analysisError.textContent = error.message || "Unable to analyze this opportunity.";
			analysisError.hidden = false;
		} finally {
			analysisLoading.hidden = true;
			analysisButtons.forEach(button => { button.disabled = false; });
		}
	}

	document.getElementById("analyzeSkillGapButton").addEventListener("click", loadCareerAnalysis);
	document.getElementById("checkReadinessButton").addEventListener("click", loadCareerAnalysis);
	document.getElementById("askAgentButton").addEventListener("click", () => {
		window.location.href = `/html/candidate-dashboard.html?careerAgentType=${encodeURIComponent(type)}`
			+ `&careerAgentId=${encodeURIComponent(id)}#careerAgentToggle`;
	});

	function safeExternalUrl(value) {
		if (!value) {
			return null;
		}
		try {
			const url = new URL(value);
			return ["http:", "https:"].includes(url.protocol) ? url : null;
		} catch (error) {
			return null;
		}
	}

	async function updateApplicationStatus() {
		if (type === "JOB") {
			const applications = await request("/api/applications/my-applications");
			applicationSubmitted = Array.isArray(applications)
				&& applications.some(application => Number(application.jobId) === id);
		} else {
			applicationSubmitted = Boolean(await request(`/api/opportunities/${id}/my-application`));
		}
		if (applicationSubmitted) {
			applyButton.disabled = true;
			applyButton.hidden = false;
			applyButton.textContent = type === "HACKATHON" ? "Registered" : "Applied";
			document.getElementById("registrationLinkContainer").hidden = true;
			const status = document.getElementById("applicationStatus");
			status.textContent = type === "HACKATHON" ? "Registration submitted" : "Application submitted";
			status.hidden = false;
		}
	}

	async function applyNow() {
		if (!opportunity || !isAvailable(opportunity) || applicationSubmitted) {
			return;
		}
		applyButton.disabled = true;
		try {
			const resume = await request("/api/resumes/active");
			if (!resume || !resume.resumeId) {
				throw new Error("Upload or select an active resume before applying.");
			}
			if (type === "JOB") {
				await request("/api/applications/apply", {
					method: "POST",
					body: JSON.stringify({
						jobId: id,
						resumeId: resume.resumeId,
						coverLetter: "Interested in this opportunity."
					})
				});
			} else {
				await request(`/api/opportunities/${id}/apply`, {
					method: "POST",
					body: JSON.stringify({
						resumeId: resume.resumeId,
						coverLetter: "Interested in this opportunity."
					})
				});
			}
			applicationSubmitted = true;
			applyButton.textContent = type === "HACKATHON" ? "Registered" : "Applied";
			document.getElementById("applicationStatus").textContent =
				type === "HACKATHON" ? "Registration submitted" : "Application submitted";
			document.getElementById("applicationStatus").hidden = false;
			showToast("Application submitted successfully.");
		} catch (error) {
			applyButton.disabled = !isAvailable(opportunity);
			showToast(error.message || "Unable to submit your application.");
		}
	}

	applyButton.addEventListener("click", applyNow);

	async function initialize() {
		if (!Number.isSafeInteger(id) || id < 1
				|| !["JOB", "INTERNSHIP", "HACKATHON"].includes(type)) {
			showError("The opportunity link is invalid.");
			return;
		}
		try {
			const item = await request(
				`/api/opportunities/details/${encodeURIComponent(type)}/${encodeURIComponent(id)}`
			);
			render(item);
			await updateApplicationStatus();
		} catch (error) {
			showError(error.message || "Unable to load opportunity details.");
		}
	}

	initialize();
})();
