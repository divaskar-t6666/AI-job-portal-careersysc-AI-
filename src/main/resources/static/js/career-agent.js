(() => {
	"use strict";

	const toggle = document.getElementById("careerAgentToggle");
	const panel = document.getElementById("careerAgentPanel");
	const closeButton = document.getElementById("careerAgentClose");
	const form = document.getElementById("careerAgentForm");
	const input = document.getElementById("careerAgentMessage");
	const historyElement = document.getElementById("careerAgentHistory");
	const disclosure = document.getElementById("careerAgentDisclosure");
	const loading = document.getElementById("careerAgentLoading");
	const sendButton = document.getElementById("careerAgentSend");
	const conversation = [];
	const params = new URLSearchParams(window.location.search);
	const selectedType = (params.get("careerAgentType") || "").toUpperCase();
	const selectedId = Number(params.get("careerAgentId"));
	const hasOpportunityContext = ["JOB", "INTERNSHIP", "HACKATHON"].includes(selectedType)
		&& Number.isSafeInteger(selectedId) && selectedId > 0;
	let sending = false;

	function authHeaders() {
		const token = localStorage.getItem("token");
		if (!token) {
			window.location.replace("/html/login.html");
			throw new Error("Please sign in to use the Career Agent.");
		}
		return {
			"Content-Type": "application/json",
			Authorization: `Bearer ${token}`
		};
	}

	async function request(url, options = {}) {
		const response = await fetch(url, {
			...options,
			headers: { ...authHeaders(), ...(options.headers || {}) }
		});
		if (response.status === 401) {
			localStorage.removeItem("token");
			window.location.replace("/html/login.html");
			throw new Error("Your session expired. Please sign in again.");
		}
		if (response.status === 403) {
			const role = localStorage.getItem("role");
			if (role && role !== "ROLE_CANDIDATE") {
				const readableRole = role.replace(/^ROLE_/, "").toLowerCase();
				throw new Error(`You are signed in as ${readableRole}. Career AI is available only to candidate accounts. Sign out and log in with a candidate account.`);
			}
			throw new Error("The server rejected this session for candidate access. Sign out and log in again with a candidate account. If you already use a candidate account, check that your email has ROLE_CANDIDATE in the users table; do not remove the backend access restriction.");
		}
		if (!response.ok) {
			let message = "";
			try {
				const body = await response.json();
				message = body.message || body.detail || "";
			} catch (error) {
				message = "";
			}
			throw new Error(message || `Career Agent request failed (${response.status}).`);
		}
		return response.json();
	}

	function addMessage(role, text, notice, suggestions) {
		const bubble = document.createElement("div");
		bubble.className = `career-agent-message ${role}`;
		bubble.textContent = text;
		if (notice) {
			const note = document.createElement("div");
			note.className = "career-agent-notice";
			note.textContent = notice;
			bubble.appendChild(note);
		}
		if (Array.isArray(suggestions) && suggestions.length) {
			const list = document.createElement("div");
			list.className = "career-agent-suggestions";
			suggestions.forEach(item => {
				const link = document.createElement("a");
				link.href = `/html/opportunity-details.html?type=${encodeURIComponent(item.opportunityType)}&id=${encodeURIComponent(item.opportunityId)}`;
				link.textContent = `${item.title} · ${item.companyName || "Company not specified"}`
					+ (item.matchedSkills ? ` — matched: ${item.matchedSkills}` : "");
				list.appendChild(link);
			});
			bubble.appendChild(list);
		}
		historyElement.appendChild(bubble);
		historyElement.scrollTop = historyElement.scrollHeight;
	}

	function setOpen(open) {
		panel.hidden = !open;
		toggle.setAttribute("aria-expanded", String(open));
		if (open) {
			input.focus();
		}
	}

	async function loadStatus() {
		try {
			const status = await request("/api/career-agent/status");
			disclosure.textContent = status.dataDisclosure;
		} catch (error) {
			disclosure.textContent = error.message;
		}
	}

	toggle.addEventListener("click", () => setOpen(panel.hidden));
	closeButton.addEventListener("click", () => setOpen(false));
	input.addEventListener("keydown", event => {
		if (event.key === "Enter" && !event.shiftKey) {
			event.preventDefault();
			form.requestSubmit();
		}
	});

	form.addEventListener("submit", async event => {
		event.preventDefault();
		const message = input.value.trim();
		if (!message || sending) {
			return;
		}
		addMessage("user", message);
		input.value = "";
		sending = true;
		sendButton.disabled = true;
		loading.hidden = false;
		const requestBody = {
			message,
			history: conversation.slice(-10)
		};
		if (hasOpportunityContext) {
			requestBody.opportunityType = selectedType;
			requestBody.opportunityId = selectedId;
		}
		try {
			const response = await request("/api/career-agent/chat", {
				method: "POST",
				body: JSON.stringify(requestBody)
			});
				addMessage("assistant", response.answer, response.providerNotice, response.suggestions);
			conversation.push({ role: "user", content: message });
			conversation.push({ role: "assistant", content: response.answer });
		} catch (error) {
			addMessage("assistant", error.message || "Unable to get career guidance.");
		} finally {
			sending = false;
			sendButton.disabled = false;
			loading.hidden = true;
			input.focus();
		}
	});

	loadStatus();
	if (hasOpportunityContext) {
		addMessage("assistant", `I’m ready to answer questions about the selected ${selectedType.toLowerCase()}. Its details will be loaded from the saved record.`);
		setOpen(true);
	} else {
		addMessage("assistant", "Hi! I can chat about anything, or help you understand your saved resume skills and opportunities.");
	}
})();
