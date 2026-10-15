/* =====================================================
   CAREERSYNC AI CANDIDATE DASHBOARD
   PART 1
   CONFIGURATION + HELPERS + UI CONTROLS
===================================================== */

/* =====================================================
   API CONFIGURATION
===================================================== */

const API = {

    USER: "/api/users/me",

    PROFILE: "/api/candidate/profile",

    ACTIVE_RESUME: "/api/resumes/active",

    APPLICATIONS: "/api/applications/my-applications",

    JOBS: "/api/jobs",

    SEARCH_JOBS: "/api/jobs/search",

    RECOMMENDATIONS: "/api/jobs/recommendations",

    AI_MATCH: "/api/ai-match/job",

};

let filterJobsPromise = null;

async function loadJobsForFilterOptions() {

    if (!filterJobsPromise) {
        filterJobsPromise = (async () => {
            const jobs = [];
            let page = 0;

            // Use the same authenticated, published-opportunity index as search.
            while (page < 100) {
                const pageData = await postRequest(
                    `/api/opportunities/search?pageNumber=${page}&pageSize=100&sortBy=createdAt&sortDirection=desc`,
                    {}
                );

                if (!pageData || !Array.isArray(pageData.content)) {
                    throw new Error("Invalid opportunities response while loading filter options");
                }

                jobs.push(...pageData.content);

                if (pageData.lastPage || pageData.content.length === 0) {
                    break;
                }

                page += 1;
            }

            return jobs;
        })();
    }

    try {
        return await filterJobsPromise;
    } catch (error) {
        filterJobsPromise = null;
        throw error;
    }
}

function replaceFilterOptions(selectId, placeholder, values) {
    const select = document.getElementById(selectId);
    if (!select || values.length === 0) return;

    const previousValue = select.value;
    select.replaceChildren(new Option(placeholder, ""));

    values.forEach(value => {
        select.add(new Option(value, value));
    });

    if (values.includes(previousValue)) {
        select.value = previousValue;
    }
}

async function loadLocationFilter() {
    const jobs = await loadJobsForFilterOptions();
    const locations = [...new Set(
        jobs
            .map(job => typeof job.location === "string" ? job.location.trim() : "")
            .filter(Boolean)
    )].sort((a, b) => a.localeCompare(b));

    replaceFilterOptions("locationFilter", "All Locations", locations);
}

async function loadSkillFilter() {
    const jobs = await loadJobsForFilterOptions();
    const skills = [...new Set(
        jobs.flatMap(job => typeof job.requiredSkills === "string"
            ? job.requiredSkills.split(",").map(skill => skill.trim()).filter(Boolean)
            : [])
    )].sort((a, b) => a.localeCompare(b));

    replaceFilterOptions("skillFilter", "All Skills", skills);
}

async function loadSearchFilters() {
    const results = await Promise.allSettled([
        loadCompanyFilter(),
        loadLocationFilter(),
        loadSkillFilter()
    ]);

    results.forEach(result => {
        if (result.status === "rejected") {
            console.error("Failed to load one or more search filters", result.reason);
        }
    });
}
/* =====================================================
   GLOBAL STATE
===================================================== */

let currentUser = null;

let candidateProfile = null;

let activeResume = null;

let applications = [];

let recommendations = [];

let selectedResumeId = null;

let activeResumeLoadFailed = false;

/* ==========================================
   SEARCH STATE
========================================== */

let currentSearchPage = 0;

const searchPageSize = 6;

let currentSortBy = "createdAt";

let currentSortDirection = "desc";

let activeSearchController = null;
let selectedOpportunityType = "";
let opportunityApplications = [];
/* =====================================================
   DOM READY
===================================================== */

document.addEventListener("DOMContentLoaded", async () => {

    initializeTheme();

    initializeSidebar();

    initializeMenu();

    initializeQuickActions();

    await initializeDashboard();
});

/* =====================================================
   DASHBOARD INITIALIZATION
===================================================== */



/* =====================================================
   JWT TOKEN
===================================================== */

function getToken() {

    return localStorage.getItem("token");
}

/* =====================================================
   AUTH CHECK
===================================================== */

async function verifyAuthentication() {

    const token =
        localStorage.getItem("token");

    const role =
        localStorage.getItem("role");

    if (!token) {

        redirectToLogin();

        return;
    }

    if (role !== "ROLE_CANDIDATE") {

        redirectToLogin();

        return;
    }
}

/* =====================================================
   LOGIN REDIRECT
===================================================== */

function redirectToLogin() {

    window.location.href =
        "/html/login.html";
}

/* =====================================================
   AUTH HEADERS
===================================================== */

function getAuthHeaders() {

    return {

        "Content-Type": "application/json",

        Authorization:
            `Bearer ${getToken()}`
    };
}

/* =====================================================
   GENERIC GET API
===================================================== */

async function getRequest(url) {

    const response =
        await fetch(url, {

            method: "GET",

            headers:
                getAuthHeaders()
        });

    if (!response.ok) {

        if (response.status === 401) {

            logout();
        }

        if (response.status === 403) {

            throw new Error("Access Denied");
        }

        throw new Error(
            `API Error: ${response.status}`
        );
    }

    if (response.status === 204) {
        return null;
    }

    return await response.json();
}

/* =====================================================
   GENERIC POST API
===================================================== */

async function postRequest(url, body) {

    const response =
        await fetch(url, {

            method: "POST",

            headers:
                getAuthHeaders(),

            body:
                JSON.stringify(body)
        });

    if (!response.ok) {

        if (response.status === 401) {

            logout();

            throw new Error("Authentication expired");
        }

        throw new Error(`POST Error: ${response.status}`);
    }

    return await response.json();
}


/* =====================================================
   GENERIC PUT API
===================================================== */

async function putRequest(url, body) {

    const response =
        await fetch(url, {

            method: "PUT",

            headers:
                getAuthHeaders(),

            body:
                JSON.stringify(body)
        });

    if (!response.ok) {

        if (response.status === 401) {

            logout();

            return;
        }

        throw new Error(`PUT Error: ${response.status}`);
    }

    return await response.json();
}

/* =====================================================
   THEME TOGGLE
===================================================== */

function initializeTheme() {

    const themeBtn =
        document.getElementById(
            "themeToggle"
        );

    if (!themeBtn) return;

    const savedTheme =
        localStorage.getItem(
            "AI_MULTI_JOB_PORTAL-theme"
        );

    if (savedTheme === "dark") {

        document.body.classList.add(
            "dark"
        );

        themeBtn.innerHTML =
            '<i class="ri-sun-line"></i>';
    }

    themeBtn.addEventListener(
        "click",
        toggleTheme
    );
}

/* =====================================================
   TOGGLE THEME
===================================================== */

function toggleTheme() {

    const themeBtn =
        document.getElementById(
            "themeToggle"
        );

    document.body.classList.toggle(
        "dark"
    );

    const darkEnabled =
        document.body.classList.contains(
            "dark"
        );

    if (darkEnabled) {

        localStorage.setItem(
            "AI_MULTI_JOB_PORTAL-theme",
            "dark"
        );

        themeBtn.innerHTML =
            '<i class="ri-sun-line"></i>';

    } else {

        localStorage.setItem(
            "AI_MULTI_JOB_PORTAL-theme",
            "light"
        );

        themeBtn.innerHTML =
            '<i class="ri-moon-line"></i>';
    }
}

/* =====================================================
   SIDEBAR
===================================================== */

function initializeSidebar() {

    const menuToggle =
        document.getElementById(
            "menuToggle"
        );

    const sidebar =
        document.getElementById(
            "sidebar"
        );

    const overlay =
        document.getElementById(
            "overlay"
        );

    if (
        !menuToggle ||
        !sidebar ||
        !overlay
    ) {
        return;
    }

    menuToggle.addEventListener(
        "click",
        () => {

            sidebar.classList.add(
                "show"
            );

            overlay.classList.add(
                "show"
            );
        }
    );

    overlay.addEventListener(
        "click",
        () => {

            sidebar.classList.remove(
                "show"
            );

            overlay.classList.remove(
                "show"
            );
        }
    );
}

/* =====================================================
   MENU ACTIVE STATE
===================================================== */

function initializeMenu() {

    const menuItems =
        document.querySelectorAll(
            ".menu-item"
        );

    menuItems.forEach(item => {

        item.addEventListener(
            "click",
            () => {

                menuItems.forEach(link => {

                    link.classList.remove(
                        "active"
                    );
                });

                item.classList.add(
                    "active"
                );
            }
        );
    });
}

/* =====================================================
   QUICK ACTIONS
===================================================== */

function initializeQuickActions() {

    const uploadBtn =
        document.getElementById(
            "quickUploadResume"
        );

    const searchBtn =
        document.getElementById(
            "quickSearchJobs"
        );

    const applicationsBtn =
        document.getElementById(
            "quickViewApplications"
        );

    const profileBtn =
        document.getElementById(
            "quickEditProfile"
        );

    if (uploadBtn) {

        uploadBtn.addEventListener(
            "click",
            () => {

                document
                    .getElementById(
                        "resumeFileInput"
                    )
                    ?.click();
            }
        );
    }

    if (searchBtn) {

        searchBtn.addEventListener(
            "click",
            () => {

                document
                    .getElementById(
                        "jobSearchInput"
                    )
                    ?.focus();
            }
        );
    }

    if (applicationsBtn) {

        applicationsBtn.addEventListener(
            "click",
            () => {

                document
                    .getElementById(
                        "applicationsSection"
                    )
                    ?.scrollIntoView({
                        behavior: "smooth"
                    });
            }
        );
    }

    if (profileBtn) {

        profileBtn.addEventListener(
            "click",
            () => {

                document
                    .getElementById(
                        "profileSection"
                    )
                    ?.scrollIntoView({
                        behavior: "smooth"
                    });
            }
        );
    }
}

/* =====================================================
   LOADER
===================================================== */

function showLoader() {

    const loader =
        document.getElementById(
            "loadingOverlay"
        );

    if (loader) {

        loader.classList.remove(
            "hidden"
        );
    }
}

function hideLoader() {

    const loader =
        document.getElementById(
            "loadingOverlay"
        );

    if (loader) {

        loader.classList.add(
            "hidden"
        );
    }
}

/* =====================================================
   TOAST NOTIFICATION
===================================================== */

function showToast(
    message,
    type = "success"
) {

    const container =
        document.getElementById(
            "toastContainer"
        );

    if (!container) return;

    const toast =
        document.createElement("div");

    toast.className =
        `toast ${type}`;

    let icon =
        "ri-checkbox-circle-fill";

    if (type === "error") {

        icon =
            "ri-close-circle-fill";
    }

    if (type === "warning") {

        icon =
            "ri-error-warning-fill";
    }

    toast.innerHTML = `
        <i class="${icon}"></i>
        <div class="toast-message">
            ${message}
        </div>
    `;

    container.appendChild(
        toast
    );

    setTimeout(() => {

        toast.remove();

    }, 3500);
}

/* =====================================================
   LOGOUT
===================================================== */

function logout() {

    localStorage.removeItem("token");

    localStorage.removeItem("tokenType");

    localStorage.removeItem("role");

    localStorage.removeItem("AI_MULTI_JOB_PORTAL-theme");

    showToast(
        "Logged out successfully",
        "success"
    );

    setTimeout(() => {

        redirectToLogin();

    }, 1000);
}

document
    .getElementById("logoutBtn")
    ?.addEventListener(
        "click",
        logout
    );

/* =====================================================
   DATE FORMATTER
===================================================== */

function formatDate(dateString) {

    if (!dateString) {

        return "-";
    }

    const date = new Date(dateString);

    return date.toLocaleString(
        "en-IN",
        {
            day: "2-digit",
            month: "short",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        }
    );
}

/* =====================================================
   END OF PART 1
===================================================== */


/* =====================================================
   PART 2
   USER PROFILE
   CANDIDATE PROFILE
   ACTIVE RESUME
   APPLICATIONS
   DASHBOARD STATS
===================================================== */

/* =====================================================
   LOAD ALL DASHBOARD DATA
===================================================== */

async function loadDashboardData() {

    await Promise.all([
        loadCurrentUser(),
        loadCandidateProfile(),
        loadActiveResume(),
        loadApplications(),
        loadOpportunityApplications()
    ]);

    updateDashboardStatistics();
}

/* =====================================================
   UPDATE INITIALIZATION
===================================================== */



/* =====================================================
   LOAD CURRENT USER
   API:
   GET /api/users/me
===================================================== */

async function loadCurrentUser() {

    try {

        currentUser =
            await getRequest(
                API.USER
            );

        populateUserData();

    } catch (error) {

        console.error(
            "User Load Error:",
            error
        );

        showToast(
            "Unable to load user information",
            "error"
        );
    }
}

/* =====================================================
   POPULATE USER DATA
===================================================== */

function populateUserData() {

    if (!currentUser) return;

    setText(
        "headerCandidateName",
        currentUser.fullName
    );

    setText(
        "welcomeUserName",
        `${currentUser.firstName} ${currentUser.lastName}`
    );

    setText(
        "userFullName",
        currentUser.fullName
    );

    setText(
        "userEmail",
        currentUser.email
    );

    setText(
        "userPhone",
        currentUser.phoneNumber
    );

    setText(
        "accountStatus",
        currentUser.accountStatus
    );
}

/* =====================================================
   LOAD CANDIDATE PROFILE
   API:
   GET /api/candidate/profile
===================================================== */

async function loadCandidateProfile() {

    try {

        candidateProfile =
            await getRequest(
                API.PROFILE
            );

        populateCandidateProfile();

    } catch (error) {

        console.error(
            "Profile Load Error:",
            error
        );

        showToast(
            "Unable to load profile",
            "warning"
        );
    }
}

/* =====================================================
   POPULATE PROFILE
===================================================== */

function populateCandidateProfile() {

    if (!candidateProfile) return;

    setText(
        "displayCurrentJobTitle",
        candidateProfile.currentJobTitle
    );

    setText(
        "currentJobTitle",
        candidateProfile.currentJobTitle
    );

    setText(
        "displayCurrentLocation",
        candidateProfile.currentLocation
    );

    setText(
        "displayHighestQualification",
        candidateProfile.highestQualification
    );

    setText(
        "candidateExperience",
        candidateProfile.yearsOfExperience
    );
}

/* =====================================================
   LOAD ACTIVE RESUME
   API:
   GET /api/resumes/active
===================================================== */

async function loadActiveResume() {

    activeResume = null;
    selectedResumeId = null;
    activeResumeLoadFailed = false;

    try {

        activeResume =
            await getRequest(
                API.ACTIVE_RESUME
            );

        selectedResumeId =
            activeResume.resumeId;

        populateResumeData();

    } catch (error) {

        if (error.message === "API Error: 404") {
            showToast(
                "No active resume found",
                "warning"
            );
        } else {
            activeResumeLoadFailed = true;
            console.error("Resume Load Error", error);
        }
    }
}

/* =====================================================
   POPULATE RESUME
===================================================== */

function populateResumeData() {

    if (!activeResume) return;

    setText(
        "dashboardResumeTitle",
        activeResume.resumeTitle
    );

    setText(
        "resumeFileName",
        activeResume.fileName
    );

    setText(
        "resumeUploadDate",
        formatDate(
            activeResume.uploadedAt
        )
    );

    setText(
        "resumeStatus",
        activeResume.isActive
            ? "ACTIVE"
            : "INACTIVE"
    );

    setText(
        "activeResume",
        activeResume.isActive
            ? "YES"
            : "NO"
    );
}

/* =====================================================
   LOAD APPLICATIONS
   API:
   GET /api/applications/my-applications
===================================================== */


/* =====================================================
   RENDER APPLICATIONS
===================================================== */

function renderApplications() {

    const container =
        document.getElementById(
            "applicationsContainer"
        );

    if (!container) return;

    if (
        !applications ||
        applications.length === 0
    ) {

        container.innerHTML = `

            <div class="empty-state">

                <i class="ri-file-list-3-line"></i>

                <h3>No Applications Found</h3>

                <p>
                    Start applying for jobs
                    to track them here.
                </p>

            </div>
        `;

        return;
    }

    container.innerHTML = "";

    applications.forEach(application => {

        const card =
            document.createElement("div");

        card.className =
            "application-card";

        const statusClass =
            getApplicationStatusClass(
                application.applicationStatus
            );

        card.innerHTML = `

			    <div class="application-left">

			        <h3>
			            ${application.jobTitle}
			        </h3>

			        <p>
			            Applied:
			            ${formatDate(application.appliedAt)}
			        </p>

			        <p>
			            Last Updated:
			            ${formatDate(application.updatedAt)}
			        </p>

			    </div>

			    <div class="application-right">

			        <div class="
			            application-status
			            ${statusClass}
			        ">
			            ${application.applicationStatus
                .replaceAll("_", " ")}
			        </div>

			        <button
			            class="timeline-btn"
			            onclick="viewApplicationHistory(${application.applicationId})">

			            View Timeline

			        </button>

			    </div>
			`;

        container.appendChild(card);
    });
}
/* =====================================================
   STATUS CLASS
===================================================== */

function getApplicationStatusClass(
    status
) {

    if (!status) {

        return "status-applied";
    }

    switch (
    status.toUpperCase()
    ) {

        case "UNDER_REVIEW":
            return "status-review";

        case "SHORTLISTED":
            return "status-shortlisted";

        case "INTERVIEW_SCHEDULED":
            return "status-interview";

        case "SELECTED":
            return "status-selected";

        case "REJECTED":
            return "status-rejected";

        default:
            return "status-applied";
    }
}

async function viewApplicationHistory(applicationId) {

    try {

        const history = await getRequest(
            `/api/applications/${applicationId}/history`
        );

        renderTimelineModal(history);

    } catch (error) {

        console.error(error);

        showToast(
            "Unable to load timeline",
            "error"
        );
    }
}

function renderTimelineModal(historyList) {

    const container =
        document.getElementById(
            "timelineContainer"
        );

    if (!container) {
        return;
    }

    if (!historyList || historyList.length === 0) {

        container.innerHTML = `
		    <div class="timeline-item">

		        <div class="timeline-status">

		            APPLIED

		        </div>

		        <div class="timeline-date">

		            Application Submitted

		        </div>

		    </div>
		`;
    } else {

        container.innerHTML =
            historyList.map(history => `

		        <div class="timeline-item">

		            <div class="timeline-status">

		                ${history.newStatus.replaceAll("_", " ")}

		            </div>

		            <div class="timeline-date">

		                ${formatDate(history.changedAt)}

		            </div>

		            <div class="timeline-meta">

		                <strong>Updated By:</strong>
		                ${history.changedBy || "System"}

		            </div>

		            <div class="timeline-meta">

		                <strong>Remarks:</strong>
		                ${history.remarks || "No remarks provided"}

		            </div>

		        </div>

		    `).join("");
    }

    document
        .getElementById(
            "timelineModal"
        )
        .classList.add("show");
}

function closeTimelineModal() {

    document
        .getElementById(
            "timelineModal"
        )
        .classList.remove("show");
}
/* =====================================================
   DASHBOARD STATISTICS
===================================================== */

function updateDashboardStatistics() {

    setText(
        "totalApplications",
        applications.length
    );

    setText(
        "accountStatus",
        currentUser
            ? currentUser.accountStatus
            : "-"
    );

    setText(
        "candidateExperience",
        candidateProfile
            ? candidateProfile.yearsOfExperience
            : "0"
    );

    setText(
        "recommendedJobsCount",
        recommendations.length
    );
}

/* =====================================================
   SAFE TEXT SETTER
===================================================== */

function setText(
    elementId,
    value
) {

    const element =
        document.getElementById(
            elementId
        );

    if (!element) return;

    element.textContent =
        value ?? "-";
}

/* =====================================================
   RESUME BUTTONS
===================================================== */

//document
//    .getElementById(
//        "viewResumeBtn"
//    )
//    ?.addEventListener(
//        "click",
//        () => {
//
//            if (!activeResume) {
//
//                showToast(
//                    "No resume available",
//                    "warning"
//                );
//
//                return;
//            }
//
//            showToast(
//                "Resume viewer will be connected in Part 4",
//                "success"
//            );
//        }
//    );
//


/* =====================================================
   END OF PART 2
===================================================== */

/* =====================================================
   PART 3
   RECOMMENDED JOBS
   JOB SEARCH
   AI MATCH ANALYSIS
===================================================== */

/* =====================================================
   LOAD RECOMMENDATIONS
   API:
   GET /api/jobs/recommendations
===================================================== */

async function loadRecommendations() {

    try {

        if (activeResumeLoadFailed) {
            throw new Error("The active resume could not be loaded");
        }

        if (!selectedResumeId) {
            recommendations = [];
            renderRecommendations();
            return;
        }

        const response =
            await getRequest(API.RECOMMENDATIONS);

        if (!Array.isArray(response)) {
            throw new Error("Invalid recommendations response");
        }

        recommendations = response;

        setText(
            "recommendedJobsCount",
            recommendations.length
        );

        renderRecommendations();

    } catch (error) {

        console.error(
            "Recommendations Error:",
            error
        );

        const container = document.getElementById("recommendedJobsContainer");
        if (container) {
            container.innerHTML = `
                <div class="empty-state">
                    <h3>Recommendations unavailable</h3>
                    <p>Unable to load recommendations. Refresh the dashboard to try again.</p>
                </div>
            `;
        }

        showToast(
            "Unable to load job recommendations",
            "error"
        );
    }
}

/* =====================================================
   UPDATE DASHBOARD INITIALIZATION
===================================================== */

/*
Add this line inside loadDashboardData()

await loadRecommendations();

AFTER:

await loadApplications();
*/

/* =====================================================
   RENDER RECOMMENDATIONS
===================================================== */

function renderRecommendations() {

    const container =
        document.getElementById(
            "recommendedJobsContainer"
        );

    if (!container) return;

    if (
        !recommendations ||
        recommendations.length === 0
    ) {

        const emptyMessage = selectedResumeId
            ? "No open jobs meet the recommendation threshold for this resume yet."
            : "Upload an active resume to receive recommendations.";

        container.innerHTML = `

            <div class="empty-state">

                <i class="ri-award-line"></i>

                <h3>
                    No Recommendations
                </h3>

                <p>${emptyMessage}</p>

            </div>
        `;

        return;
    }

    container.innerHTML = "";

    recommendations.forEach(job => {

        const card =
            document.createElement("div");

        card.className =
            "job-card";

        card.innerHTML = `

            <div class="job-header">

                <div class="job-company">

                    <div class="company-icon">

                        <i class="ri-briefcase-4-line"></i>

                    </div>

                    <div>

                        <div class="job-title">

                            ${job.jobTitle}

                        </div>

						<div class="company-name">
						    ${job.companyName || "CareerSync AI"}
						</div>

                    </div>

                </div>

                <div class="match-badge">

                    ${Math.round(job.matchPercentage)}%

                </div>

            </div>

            <div class="job-info">

                <span>
                    AI Recommended
                </span>

                <span>
                    Resume Match
                </span>

            </div>

            <div class="job-actions">

                <button
                    class="primary-btn match-btn"
                    data-job-id="${job.jobId}">

                    <i class="ri-brain-line"></i>

                    View Match

                </button>

                <a
                    class="primary-btn view-details-btn"
                    href="/html/opportunity-details.html?type=JOB&id=${encodeURIComponent(job.jobId)}">
                    View Details
                </a>

            </div>
        `;

        container.appendChild(
            card
        );
    });

    initializeMatchButtons();
}

/* =====================================================
   VIEW MATCH BUTTONS
===================================================== */

function initializeMatchButtons() {

    const buttons =
        document.querySelectorAll(
            ".match-btn"
        );

    buttons.forEach(button => {

        button.addEventListener(
            "click",
            async () => {

                const jobId =
                    button.dataset.jobId;

                await loadMatchAnalysis(
                    jobId
                );
            }
        );
    });
}

/* =====================================================
   AI MATCH API
   POST
   /api/ai-match/job/{jobId}/resume/{resumeId}
===================================================== */

async function loadMatchAnalysis(
    jobId
) {

    try {

        if (!selectedResumeId) {

            showToast(
                activeResumeLoadFailed
                    ? "Unable to load active resume"
                    : "Active resume required",
                activeResumeLoadFailed
                    ? "error"
                    : "warning"
            );

            return;
        }

        showLoader();

        const result =
            await postRequest(
                `${API.AI_MATCH}/${jobId}/resume/${selectedResumeId}`,
                {}
            );

        if (
            !result ||
            result.jobId !== Number(jobId) ||
            result.resumeId !== Number(selectedResumeId) ||
            !Number.isFinite(result.matchPercentage)
        ) {
            throw new Error("Invalid match analysis response");
        }

        renderMatchResult(
            result
        );

        document
            .getElementById(
                "aiMatchSection"
            )
            ?.scrollIntoView({

                behavior: "smooth"
            });

    } catch (error) {

        console.error(
            "Match Error:",
            error
        );

        showToast(
            "Unable to generate match",
            "error"
        );

    } finally {

        hideLoader();
    }
}

/* =====================================================
   RENDER MATCH RESULT
===================================================== */

function renderMatchResult(
    match
) {

    if (!match) return;

    setText(
        "matchPercentage",
        `${Math.round(match.matchPercentage)}%`
    );

    renderMatchedSkills(
        match.matchedSkills
    );

    renderMissingSkills(
        match.missingSkills
    );
}

/* =====================================================
   MATCHED SKILLS
===================================================== */

function renderMatchedSkills(
    skills
) {

    const container =
        document.getElementById(
            "matchedSkills"
        );

    if (!container) return;

    container.innerHTML = "";

    const skillList =
        parseSkills(skills);

    skillList.forEach(skill => {

        const tag =
            document.createElement(
                "span"
            );

        tag.className =
            "skill-tag matched";

        tag.textContent =
            skill;

        container.appendChild(
            tag
        );
    });
}

/* =====================================================
   MISSING SKILLS
===================================================== */

function renderMissingSkills(
    skills
) {

    const container =
        document.getElementById(
            "missingSkills"
        );

    if (!container) return;

    container.innerHTML = "";

    const skillList =
        parseSkills(skills);

    skillList.forEach(skill => {

        const tag =
            document.createElement(
                "span"
            );

        tag.className =
            "skill-tag missing";

        tag.textContent =
            skill;

        container.appendChild(
            tag
        );
    });
}

/* =====================================================
   PARSE SKILLS STRING
===================================================== */

function parseSkills(
    skills
) {

    if (!skills) {

        return [];
    }

    return skills
        .split(",")
        .map(skill =>
            skill.trim()
        )
        .filter(skill =>
            skill.length > 0
        );
}

/* =====================================================
   SEARCH INPUT
===================================================== */

const searchInput =
    document.getElementById(
        "jobSearchInput"
    );

document.getElementById("opportunitySearchForm")?.addEventListener("submit", event => {
    event.preventDefault();
    searchJobs(0);
});

document.getElementById("clearOpportunitySearchBtn")?.addEventListener("click", () => {
    if (searchInput) {
        searchInput.value = "";
    }
    searchJobs(0);
});

document.querySelectorAll("[data-opportunity-type]").forEach(button => {
    button.addEventListener("click", () => {
        selectedOpportunityType = button.dataset.opportunityType || "";
        document.querySelectorAll("[data-opportunity-type]").forEach(tab => {
            const selected = tab === button;
            tab.classList.toggle("active", selected);
            tab.setAttribute("aria-selected", String(selected));
        });
        searchJobs(0);
    });
});
/* =====================================================
   LOAD COMPANY FILTER
===================================================== */

async function loadCompanyFilter() {

    const jobs = await loadJobsForFilterOptions();
    const companies = [...new Set(
        jobs
            .map(job => typeof job.companyName === "string" ? job.companyName.trim() : "")
            .filter(Boolean)
    )].sort((a, b) => a.localeCompare(b));

    replaceFilterOptions("companyFilter", "All Companies", companies);

}
/* =====================================================
   SEARCH JOBS
===================================================== */

async function searchJobs(page = 0, scrollToResults = true) {

    if (activeSearchController) {
        activeSearchController.abort();
    }

    const controller = new AbortController();
    activeSearchController = controller;
    const resultsContainer = document.getElementById("searchResultsContainer");
    const resultCount = document.getElementById("searchResultCount");
    const pagination = document.getElementById("searchPaginationContainer");
    if (resultsContainer) {
        resultsContainer.replaceChildren();
    }
    if (resultCount) {
        resultCount.textContent = "Searching...";
    }
    if (pagination) {
        pagination.replaceChildren();
    }

    try {

        currentSearchPage = page;

        const payload = {

            keyword:
                document.getElementById("jobSearchInput")?.value.trim() || null,

            location:
                document.getElementById("locationFilter")?.value || null,

            skill:
                document.getElementById("skillFilter")?.value || null,

            companyName:
                document.getElementById("companyFilter")?.value || null,

            employmentType:
                document.getElementById("employmentTypeFilter")?.value || null,

            experienceRequired:
                document.getElementById("experienceFilter")?.value
                    ? parseInt(document.getElementById("experienceFilter").value)
                    : null,

            jobStatus:
                document.getElementById("statusFilter")?.value || null,

            opportunityType:
                selectedOpportunityType || null
        };

        const sortValue =
            document.getElementById("sortFilter")?.value || "createdAt-desc";

        const sort =
            sortValue.split("-");

        currentSortBy = sort[0];

        currentSortDirection = sort[1];

        const query = new URLSearchParams({
            pageNumber: String(currentSearchPage),
            pageSize: String(searchPageSize),
            sortBy: currentSortBy,
            sortDirection: currentSortDirection
        });

        const response = await fetch(

            `/api/opportunities/search?${query.toString()}`,

            {

                method: "POST",

                headers: getAuthHeaders(),

                body: JSON.stringify(payload),

                signal: controller.signal

            }

        );

        if (!response.ok) {

            throw new Error("Search Failed");

        }

        const pageData =
            await response.json();

        if (activeSearchController !== controller) {
            return;
        }

        if (!pageData || !Array.isArray(pageData.content) || !Number.isFinite(pageData.totalElements)) {
            throw new Error("Invalid job search response");
        }

        renderSearchResults(pageData.content, pageData.totalElements);

        renderPagination(pageData);

        if (scrollToResults) {
            document
                .getElementById("searchResultsSection")
                ?.scrollIntoView({

                    behavior: "smooth"

                });
        }

    }

    catch (error) {

        if (error.name === "AbortError" || activeSearchController !== controller) {
            return;
        }

        console.error(error);

        if (resultsContainer) {
            resultsContainer.textContent = "Search could not be completed. Please try again.";
        }
        if (resultCount) {
            resultCount.textContent = "Search unavailable";
        }

        showToast(

            "Unable to search opportunities",

            "error"

        );

    }

}

/* =====================================================
   SEARCH RESULTS
===================================================== */

function renderSearchResults(
    opportunities,
    totalElements = opportunities?.length || 0
) {

    const container =
        document.getElementById(
            "searchResultsContainer"
        );

    const resultCount =
        document.getElementById(
            "searchResultCount"
        );

    const paginationContainer =
        document.getElementById(
            "searchPaginationContainer"
        );

    if (!container) return;

    if (!opportunities || opportunities.length === 0) {

        if (resultCount) {

            resultCount.textContent =
                `${totalElements} Opportunities Found`;

        }

        if (paginationContainer) {

            paginationContainer.innerHTML = "";

        }

        container.innerHTML = `

            <div class="empty-state">

                <i class="ri-search-line"></i>

                <h3>
                    No Opportunities Found
                </h3>

            </div>
        `;

        return;
    }

    container.innerHTML = "";

    if (resultCount) {

        resultCount.textContent =
            `${totalElements} Opportunities Found`;

    }

    opportunities.forEach(opportunity => {
        const isJob = opportunity.opportunityType === "JOB";
        const alreadyApplied = isJob
            ? isJobAlreadyApplied(opportunity.id)
            : opportunityApplications.some(application =>
                application.opportunityId === opportunity.id
            );

        const card =
            document.createElement("div");

        card.className = "job-card";
        const header = document.createElement("div");
        header.className = "job-header";
        const companyGroup = document.createElement("div");
        companyGroup.className = "job-company";
        const companyIcon = document.createElement("div");
        companyIcon.className = "company-icon";
        companyIcon.innerHTML = '<i class="ri-building-line" aria-hidden="true"></i>';
        const companyCopy = document.createElement("div");
        const title = document.createElement("div");
        title.className = "job-title";
        title.textContent = opportunity.title || "Untitled opportunity";
        const company = document.createElement("div");
        company.className = "company-name";
        company.textContent = opportunity.companyName || "Company not listed";
        companyCopy.append(title, company);
        companyGroup.append(companyIcon, companyCopy);
        header.appendChild(companyGroup);

        const type = document.createElement("span");
        type.className = "opportunity-card-type";
        type.textContent = opportunity.opportunityType;

        const details = document.createElement("div");
        details.className = "job-info";
        [
            opportunity.location,
            opportunity.workMode || opportunity.employmentType,
            opportunity.experienceRequired != null
                ? `${opportunity.experienceRequired} Years`
                : null
        ].filter(Boolean).forEach(value => {
            const detail = document.createElement("span");
            detail.textContent = value;
            details.appendChild(detail);
        });

        const actions = document.createElement("div");
        actions.className = "job-actions";
        const viewButton = document.createElement("button");
        viewButton.className = "view-job-btn";
        viewButton.type = "button";
        viewButton.textContent = "View Details";
        viewButton.addEventListener("click", () => {
            window.location.href =
                `/html/opportunity-details.html?type=${encodeURIComponent(opportunity.opportunityType)}` +
                `&id=${encodeURIComponent(opportunity.id)}`;
        });
        actions.appendChild(viewButton);

        if (alreadyApplied) {
            const appliedButton = document.createElement("button");
            appliedButton.className = "apply-job-btn applied-btn";
            appliedButton.disabled = true;
            appliedButton.textContent = "Applied";
            actions.appendChild(appliedButton);
        } else if (isJob) {
            const applyButton = document.createElement("button");
            applyButton.className = "apply-job-btn";
            applyButton.type = "button";
            applyButton.textContent = "Apply Now";
            applyButton.addEventListener("click", () => applyJob(opportunity.id));
            actions.appendChild(applyButton);
        }

        card.append(header, type, details, actions);

        container.appendChild(card);
    });
}

/* ==========================================
   SEARCH PAGINATION
========================================== */

function renderPagination(pageData) {

    const container =
        document.getElementById(
            "searchPaginationContainer"
        );

    if (!container)
        return;

    container.innerHTML = "";

    if (pageData.totalPages <= 1)
        return;

    /* Previous Button */

    const previousButton =
        document.createElement("button");

    previousButton.className =
        "pagination-btn";

    previousButton.innerText =
        "Previous";

    previousButton.disabled =
        pageData.pageNumber === 0;

    previousButton.onclick = () => {

        searchJobs(pageData.pageNumber - 1);

    };

    container.appendChild(previousButton);

    /* Page Numbers */

    for (

        let i = 0;

        i < pageData.totalPages;

        i++

    ) {

        const pageButton =
            document.createElement("button");

        pageButton.className =
            "pagination-btn";

        pageButton.innerText =
            i + 1;

        if (

            i === pageData.pageNumber

        ) {

            pageButton.classList.add("active");

        }

        pageButton.onclick = () => {

            searchJobs(i);

        };

        container.appendChild(pageButton);

    }

    /* Next Button */

    const nextButton =
        document.createElement("button");

    nextButton.className =
        "pagination-btn";

    nextButton.innerText =
        "Next";

    nextButton.disabled =
        pageData.lastPage;

    nextButton.onclick = () => {

        searchJobs(pageData.pageNumber + 1);

    };

    container.appendChild(nextButton);

}

function isJobAlreadyApplied(jobId) {

    return applications.some(
        application =>
            application.jobId === jobId
    );
}
async function applyJob(jobId) {

    try {

        const activeResume = await getRequest(
            "/api/resumes/active"
        );

        if (!activeResume) {

            showToast(
                "Please upload a resume first",
                "error"
            );

            return;
        }

        const requestBody = {

            jobId: jobId,

            resumeId: activeResume.resumeId,

            coverLetter:
                "Interested in this opportunity."
        };

        await postRequest(
            "/api/applications/apply",
            requestBody
        );

        showToast(
            "Application submitted successfully",
            "success"
        );

        await loadApplications();

        await loadDefaultJobs();

        updateDashboardStatistics();

    } catch (error) {

        console.error(error);

        const message =
            error?.message ||
            "Application failed";

        showToast(
            message,
            "error"
        );
    }
}

/* =====================================================
   DEBOUNCE
===================================================== */

function debounce(
    callback,
    delay
) {

    let timeout;

    return (...args) => {

        clearTimeout(
            timeout
        );

        timeout =
            setTimeout(
                () =>
                    callback(...args),
                delay
            );
    };
}

/* =====================================================
   END OF PART 3
===================================================== */

/* =====================================================
   PART 4
   RESUME UPLOAD
   DASHBOARD REFRESH
   LOGOUT
   FINAL INITIALIZATION
===================================================== */

/* =====================================================
   RESUME FILE INPUT
===================================================== */

const resumeFileInput =
    document.getElementById(
        "resumeFileInput"
    );

if (resumeFileInput) {

    resumeFileInput.addEventListener(
        "change",
        uploadResume
    );
}

/* =====================================================
   UPLOAD RESUME BUTTON
===================================================== */

document
    .getElementById(
        "uploadResumeBtn"
    )
    ?.addEventListener(
        "click",
        () => {

            document
                .getElementById(
                    "resumeFileInput"
                )
                ?.click();
        }
    );

/* =====================================================
   UPLOAD RESUME
   API:
   POST /api/resume-upload
===================================================== */

async function uploadResume(
    event
) {

    const file =
        event.target.files[0];

    if (!file) {

        return;
    }

    const formData =
        new FormData();

    formData.append(
        "file",
        file
    );

    try {

        showLoader();

        const response =
            await fetch(
                "/api/resume-upload",
                {
                    method: "POST",

                    headers: {

                        Authorization:
                            `Bearer ${getToken()}`
                    },

                    body:
                        formData
                }
            );

        if (!response.ok) {

            throw new Error(
                "Upload failed"
            );
        }

        const uploadData = await response.json();
        let parsingErrorMessage = "";

        try {
            await parseUploadedResume(uploadData.resumeId, file);
        } catch (parseError) {
            if (parseError.status === 401) throw parseError;
            parsingErrorMessage = parseError.message;
            // The upload endpoint already stored the file/resume. Do not leave the
            // dashboard stale just because automatic parsing was denied or failed.
            console.error("Resume uploaded, but automatic parsing failed", parseError);
        }

        await refreshDashboard();

        showToast(
            parsingErrorMessage
                ? `Resume uploaded, but automatic parsing failed: ${parsingErrorMessage}. Edit the resume to verify the extracted details.`
                : "Resume uploaded successfully",
            parsingErrorMessage ? "warning" : "success"
        );

    } catch (error) {

        console.error(
            error
        );

        showToast(
            error.message || "Resume upload failed",
            "error"
        );

    } finally {

        hideLoader();

        resumeFileInput.value = "";
    }
}

async function getResponseErrorMessage(response, fallbackMessage) {

    try {
        const errorBody = await response.json();
        return errorBody.message || errorBody.error ||
            `${fallbackMessage} (HTTP ${response.status})`;
    } catch {
        return `${fallbackMessage} (HTTP ${response.status})`;
    }
}

async function parseUploadedResume(resumeId, file) {

    const extension = file.name.split(".").pop().toLowerCase();

    if (extension === "doc") {
        throw new Error(
            "Automatic parsing supports PDF and DOCX files; DOC files must be entered manually."
        );
    }

    const response = await fetch(
        `/api/resume-parser/parse/${encodeURIComponent(resumeId)}`,
        {
            method: "POST",
            headers: {
                Authorization: `Bearer ${getToken()}`
            }
        }
    );

    if (!response.ok) {
        const responseBody = await response.text();
        let errorMessage = `Resume parsing failed (HTTP ${response.status})`;

        if (responseBody.trim()) {
            try {
                const errorData = JSON.parse(responseBody);
                errorMessage = errorData.message || errorData.error || errorMessage;
            } catch {
                if (!responseBody.trimStart().startsWith("<")) {
                    errorMessage = responseBody.trim();
                }
            }
        }

        const error = new Error(errorMessage);
        error.status = response.status;
        if (response.status === 401) {
            logout();
        }
        throw error;
    }

    const parsedResume = await response.json();

    if (
        !parsedResume ||
        !Array.isArray(parsedResume.skills) ||
        !Array.isArray(parsedResume.experience) ||
        !Array.isArray(parsedResume.education)
    ) {
        throw new Error("Invalid resume parsing response");
    }

    return {
        skills: [...new Set(
            parsedResume.skills
            .filter(skill => typeof skill === "string")
            .map(skill => skill.trim())
            .filter(Boolean)
        )],
        experience: parsedResume.experience,
        education: parsedResume.education
    };
}

/* =====================================================
   VIEW RESUME
===================================================== */

document
    .getElementById(
        "viewResumeBtn"
    )
    ?.addEventListener(
        "click",
        viewResume
    );

document
    .getElementById(
        "downloadResumeBtn"
    )
    ?.addEventListener(
        "click",
        downloadResume
    );

async function viewResume() {

    if (!activeResume) {

        showToast(
            "No active resume available",
            "warning"
        );

        return;
    }

    try {

        const response =
            await fetch(
                '/api/resume-upload/download/' +
                encodeURIComponent(
                    activeResume.fileName
                ),
                {
                    headers: {
                        Authorization:
                            'Bearer ' +
                            getToken()
                    }
                }
            );

        if (!response.ok) {

            throw new Error(
                'Download failed'
            );
        }

        const blob =
            await response.blob();

        const url =
            URL.createObjectURL(blob);

        window.open(
            url,
            "_blank"
        );

        setTimeout(() => {

            URL.revokeObjectURL(
                url
            );

        }, 10000);

    } catch (error) {

        console.error(error);

        showToast(
            'Unable to open resume',
            'error'
        );
    }
}

async function downloadResume() {

    if (!activeResume) {

        showToast(
            "No active resume found",
            "warning"
        );

        return;
    }

    try {

        const response = await fetch(
            "/api/resume-upload/download/" +
            encodeURIComponent(
                activeResume.fileName
            ),
            {
                headers: {
                    Authorization:
                        "Bearer " +
                        getToken()
                }
            }
        );

        if (!response.ok) {

            throw new Error(
                "Download failed"
            );
        }

        const blob =
            await response.blob();

        const url =
            window.URL.createObjectURL(
                blob
            );

        const a =
            document.createElement("a");

        a.href = url;

        a.download =
            activeResume.fileName;

        document.body.appendChild(a);

        a.click();

        a.remove();

        window.URL.revokeObjectURL(
            url
        );

    } catch (error) {

        console.error(
            "Download Error:",
            error
        );

        showToast(
            "Failed to download resume",
            "error"
        );
    }
}
/* =====================================================
   QUICK ACTION BUTTONS
===================================================== */

document
    .getElementById(
        "quickUploadResume"
    )
    ?.addEventListener(
        "click",
        () => {

            document
                .getElementById(
                    "resumeFileInput"
                )
                ?.click();
        }
    );

document
    .getElementById(
        "quickSearchJobs"
    )
    ?.addEventListener(
        "click",
        () => {

            document
                .getElementById(
                    "jobSearchInput"
                )
                ?.focus();
        }
    );

document
    .getElementById(
        "quickViewApplications"
    )
    ?.addEventListener(
        "click",
        () => {

            document
                .getElementById(
                    "applicationsSection"
                )
                ?.scrollIntoView({

                    behavior: "smooth"
                });
        }
    );

document
    .getElementById(
        "quickEditProfile"
    )
    ?.addEventListener(
        "click",
        () => {

            document
                .getElementById(
                    "profileSection"
                )
                ?.scrollIntoView({

                    behavior: "smooth"
                });
        }
    );

/* =====================================================
   REFRESH DASHBOARD
===================================================== */

async function refreshDashboard() {

    try {

        showLoader();

        await loadCurrentUser();

        await loadCandidateProfile();

        await loadActiveResume();

        await loadApplications();

        await loadRecommendations();

        updateDashboardStatistics();

    } catch (error) {

        console.error(
            error
        );

    } finally {

        hideLoader();
    }
}

/* =====================================================
   LOAD DEFAULT JOBS
===================================================== */

async function loadDefaultJobs() {

    try {
        await searchJobs(0, false);

    } catch (error) {

        console.error(
            "Default Opportunities Error",
            error
        );
    }
}

/* =====================================================
   ENTER KEY SEARCH
===================================================== */

document

    .getElementById("applyFiltersBtn")

    ?.addEventListener(

        "click",

        () => {

            searchJobs(0);

        }

    );

document
    .getElementById("resetFiltersBtn")
    ?.addEventListener("click", () => {
        document.getElementById("jobSearchInput").value = "";
        [
            "locationFilter",
            "skillFilter",
            "companyFilter",
            "employmentTypeFilter",
            "experienceFilter",
            "statusFilter"
        ].forEach(id => {
            const filter = document.getElementById(id);
            if (filter) {
                filter.value = "";
            }
        });
        document.getElementById("sortFilter").value = "createdAt-desc";
        selectedOpportunityType = "";
        document.querySelectorAll("[data-opportunity-type]").forEach(tab => {
            const selected = !tab.dataset.opportunityType;
            tab.classList.toggle("active", selected);
            tab.setAttribute("aria-selected", String(selected));
        });
        searchJobs(0);
    });

/* =====================================================
   KEYBOARD SHORTCUTS
===================================================== */

document.addEventListener(
    "keydown",
    event => {

        /*
         * Alt + S
         * Focus Search
         */

        if (
            event.altKey &&
            event.key.toLowerCase() === "s"
        ) {

            event.preventDefault();

            document
                .getElementById(
                    "jobSearchInput"
                )
                ?.focus();
        }

        /*
         * Alt + U
         * Upload Resume
         */

        if (
            event.altKey &&
            event.key.toLowerCase() === "u"
        ) {

            event.preventDefault();

            document
                .getElementById(
                    "resumeFileInput"
                )
                ?.click();
        }
    }
);

/* =====================================================
   NETWORK ERROR HANDLER
===================================================== */

window.addEventListener(
    "offline",
    () => {

        showToast(
            "Internet connection lost",
            "error"
        );
    }
);

window.addEventListener(
    "online",
    () => {

        showToast(
            "Connection restored",
            "success"
        );
    }
);

/* =====================================================
   UPDATE INITIALIZATION
===================================================== */

/*
Replace initializeDashboard()
with:
*/

async function initializeDashboard() {

    try {

        showLoader();

        await verifyAuthentication();

        await loadDashboardData();

        try {

            await loadRecommendations();

        } catch (e) {

            console.error("Recommendation Error", e);

        }

        try {

            await loadSearchFilters();

        } catch (e) {

            console.error("Filter Error", e);

        }

        try {

            await loadDefaultJobs();

        } catch (e) {

            console.error("Default Job Error", e);

        }

        showToast(
            "Dashboard loaded successfully",
            "success"
        );

    } catch (error) {

        console.error(
            error
        );

        showToast(
            "Failed to load dashboard",
            "error"
        );

    } finally {

        hideLoader();
    }
}

/* =====================================================
   FINAL SAFETY CHECK
===================================================== */

setTimeout(() => {

    const loader =
        document.getElementById(
            "loadingOverlay"
        );

    if (
        loader &&
        !loader.classList.contains(
            "hidden"
        )
    ) {

        hideLoader();
    }

}, 10000);
/* ==========================================
   PROFILE MODAL
========================================== */

const profileModal =
    document.getElementById(
        "profileModal"
    );

const editProfileBtn =
    document.getElementById(
        "editProfileBtn"
    );

const closeProfileModal =
    document.getElementById(
        "closeProfileModal"
    );

editProfileBtn?.addEventListener(
    "click",
    openProfileModal
);

closeProfileModal?.addEventListener(
    "click",
    closeProfilePopup
);
function openProfileModal() {

    profileModal.classList.add(
        "active"
    );

    populateProfileForm();
}
function populateProfileForm() {

    if (!currentUser) {
        return;
    }

    document.getElementById(
        "firstName"
    ).value =
        currentUser.firstName || "";

    document.getElementById(
        "lastName"
    ).value =
        currentUser.lastName || "";

    document.getElementById(
        "phoneNumber"
    ).value =
        currentUser.phoneNumber || "";

    if (!candidateProfile) {
        return;
    }

    document.getElementById(
        "profileHeadline"
    ).value =
        candidateProfile.profileHeadline || "";

    document.getElementById(
        "professionalSummary"
    ).value =
        candidateProfile.professionalSummary || "";

    document.getElementById(
        "dateOfBirth"
    ).value =
        candidateProfile.dateOfBirth || "";

    document.getElementById(
        "gender"
    ).value =
        candidateProfile.gender || "";

    document.getElementById(
        "yearsOfExperience"
    ).value =
        candidateProfile.yearsOfExperience || "";

    document.getElementById(
        "currentLocation"
    ).value =
        candidateProfile.currentLocation || "";

    document.getElementById(
        "linkedinUrl"
    ).value =
        candidateProfile.linkedinUrl || "";

    document.getElementById(
        "githubUrl"
    ).value =
        candidateProfile.githubUrl || "";

    document.getElementById(
        "portfolioUrl"
    ).value =
        candidateProfile.portfolioUrl || "";

    document.getElementById(
        "highestQualification"
    ).value =
        candidateProfile.highestQualification || "";

    document.getElementById(
        "currentJobTitle"
    ).value =
        candidateProfile.currentJobTitle || "";

    document.getElementById(
        "expectedSalary"
    ).value =
        candidateProfile.expectedSalary || "";
}

const resumeForm =
    document.getElementById(
        "resumeForm"
    );

resumeForm?.addEventListener(
    "submit",
    saveResume
);

async function saveResume(
    event
) {

    event.preventDefault();

    try {

        showLoader();

        const fileInput =
            document.getElementById(
                "resumeFile"
            );

        const file =
            fileInput.files[0];

        let uploadedFileName = "";

        let uploadedFilePath = "";

        /*
         * GET ACTIVE RESUME FIRST
         */

        let activeResume = null;

        const currentResumeResponse =
            await fetch(API.ACTIVE_RESUME, {
                headers: {
                    Authorization: `Bearer ${getToken()}`
                }
            });

        if (currentResumeResponse.ok) {
            activeResume = await currentResumeResponse.json();
        } else if (currentResumeResponse.status !== 404) {
            throw new Error(
                `Unable to load active resume (HTTP ${currentResumeResponse.status})`
            );
        }

        /* ==========================
           UPLOAD FILE
        ========================== */

        if (file) {

            const formData =
                new FormData();

            formData.append(
                "file",
                file
            );

            if (activeResume) {
                const replaceResponse = await fetch(
                    `/api/resumes/${activeResume.resumeId}/replace-file`,
                    {
                        method: "PUT",
                        headers: {
                            Authorization: `Bearer ${getToken()}`
                        },
                        body: formData
                    }
                );

                if (!replaceResponse.ok) {
                    throw new Error(
                        await getResponseErrorMessage(
                            replaceResponse,
                            "Resume replacement and parsing failed"
                        )
                    );
                }

                activeResume = await replaceResponse.json();
                uploadedFileName = activeResume.fileName;
                uploadedFilePath = activeResume.filePath;

                document.getElementById("parsedSkills").value =
                    activeResume.parsedSkills || "";
                document.getElementById("parsedExperience").value =
                    activeResume.parsedExperience || "";
                document.getElementById("parsedEducation").value =
                    activeResume.parsedEducation || "";
            } else {
                const uploadResponse = await fetch(
                    "/api/resume-upload",
                    {
                        method: "POST",
                        headers: {
                            Authorization: `Bearer ${getToken()}`
                        },
                        body: formData
                    }
                );

                if (!uploadResponse.ok) {
                    throw new Error(
                        await getResponseErrorMessage(uploadResponse, "Resume upload failed")
                    );
                }

                const uploadData = await uploadResponse.json();
                uploadedFileName = uploadData.fileName;
                uploadedFilePath = uploadData.filePath;

                const parsedResume = await parseUploadedResume(uploadData.resumeId, file);
                document.getElementById("parsedSkills").value =
                    parsedResume.skills.join(", ");
                document.getElementById("parsedExperience").value =
                    parsedResume.experience
                        .map(item => [item.designation, item.companyName, item.duration]
                            .filter(value => typeof value === "string" && value.trim())
                            .join(" | "))
                        .filter(Boolean)
                        .join("\n");
                document.getElementById("parsedEducation").value =
                    parsedResume.education
                        .map(item => [item.degree, item.institution, item.year]
                            .filter(value => typeof value === "string" && value.trim())
                            .join(" | "))
                        .filter(Boolean)
                        .join("\n");
            }
        }

        /* ==========================
           CREATE RESUME PAYLOAD
        ========================== */

        const payload = {

            resumeTitle:
                document.getElementById(
                    "resumeTitleInput"
                ).value,

            fileName:
                file
                    ? uploadedFileName
                    : (activeResume ? activeResume.fileName : ""),

            filePath:
                file
                    ? uploadedFilePath
                    : (activeResume ? activeResume.filePath : ""),

            fileType:
                file
                    ? (activeResume ? activeResume.fileType : "application/pdf")
                    : (activeResume ? activeResume.fileType : ""),

            fileSizeKb:
                file
                    ? (activeResume ? activeResume.fileSizeKb : (file.size / 1024).toFixed(2))
                    : (activeResume ? activeResume.fileSizeKb : 0),

            parsedSkills:
                document.getElementById("parsedSkills")
                    .value
                    .replace(/\n/g, ", "),

            parsedExperience:
                document.getElementById(
                    "parsedExperience"
                ).value,

            parsedEducation:
                document.getElementById(
                    "parsedEducation"
                ).value
        };

        //        /* ==========================
        //           CHECK ACTIVE RESUME
        //        ========================== */
        //
        //        let activeResume =
        //            null;
        //
        //        try {
        //
        //            const activeResponse =
        //                await fetch(
        //                    "/api/resumes/active",
        //                    {
        //                        headers: {
        //                            Authorization:
        //                                "Bearer " +
        //                                getToken()
        //                        }
        //                    }
        //                );
        //
        //            if (
        //                activeResponse.ok
        //            ) {
        //
        //                activeResume =
        //                    await activeResponse.json();
        //            }
        //
        //        } catch (e) {
        //
        //            console.log(
        //                "No active resume found"
        //            );
        //        }

        /* ==========================
           CREATE
        ========================== */

        let savedResume;

        if (
            !activeResume
        ) {

            const createResponse =
                await fetch(
                    "/api/resumes",
                    {
                        method: "POST",

                        headers: {

                            "Content-Type":
                                "application/json",

                            Authorization:
                                "Bearer " +
                                getToken()
                        },

                        body:
                            JSON.stringify(
                                payload
                            )
                    }
                );

            if (!createResponse.ok) {
                throw new Error(`Resume creation failed (HTTP ${createResponse.status})`);
            }

            savedResume = await createResponse.json();
        }

        /* ==========================
           UPDATE
        ========================== */

        else {

            const updateResponse =
                await fetch(
                    `/api/resumes/${activeResume.resumeId}`,
                    {
                        method: "PUT",

                        headers: {

                            "Content-Type":
                                "application/json",

                            Authorization:
                                "Bearer " +
                                getToken()
                        },

                        body:
                            JSON.stringify(
                                payload
                            )
                    }
                );

            if (!updateResponse.ok) {
                throw new Error(`Resume update failed (HTTP ${updateResponse.status})`);
            }

            savedResume = await updateResponse.json();
        }

        if (!savedResume || !Number.isInteger(savedResume.resumeId) || savedResume.isActive !== true) {
            throw new Error("Resume save response did not confirm an active saved resume");
        }

        const activeResponse = await fetch(API.ACTIVE_RESUME, {
            headers: {
                Authorization: `Bearer ${getToken()}`
            }
        });

        if (!activeResponse.ok) {
            throw new Error(`Unable to reload saved resume (HTTP ${activeResponse.status})`);
        }

        const refreshedResume = await activeResponse.json();

        if (
            refreshedResume.resumeId !== savedResume.resumeId ||
            refreshedResume.isActive !== true ||
            refreshedResume.parsedSkills !== payload.parsedSkills ||
            refreshedResume.parsedExperience !== payload.parsedExperience ||
            refreshedResume.parsedEducation !== payload.parsedEducation
        ) {
            throw new Error("Saved resume data did not match the active resume response");
        }

        activeResume = refreshedResume;
        selectedResumeId = refreshedResume.resumeId;
        activeResumeLoadFailed = false;

        populateResumeData();
        populateResumeForm();

        await loadRecommendations();

        if (fileInput) {
            fileInput.value = "";
        }

        closeResumePopup();

        showToast("Resume saved successfully", "success");

    }
    catch (error) {

        console.error(error);

        showToast(
            error.message || "Resume operation failed",
            "error"
        );
    }
    finally {

        hideLoader();
    }
}

function populateResumeForm() {

    if (
        !activeResume
    ) {
        return;
    }

    document.getElementById(
        "resumeTitleInput"
    ).value =
        activeResume.resumeTitle || "";

    document.getElementById(
        "parsedSkills"
    ).value =
        activeResume.parsedSkills || "";

    document.getElementById(
        "parsedExperience"
    ).value =
        activeResume.parsedExperience || "";

    document.getElementById(
        "parsedEducation"
    ).value =
        activeResume.parsedEducation || "";
}

function closeProfilePopup() {

    profileModal.classList.remove(
        "active"
    );
}

/* ==========================================
   RESUME MODAL
========================================== */

const resumeModal =
    document.getElementById(
        "resumeModal"
    );

const editResumeBtn =
    document.getElementById(
        "editResumeBtn"
    );

const closeResumeModal =
    document.getElementById(
        "closeResumeModal"
    );

editResumeBtn?.addEventListener(
    "click",
    openResumeModal
);

closeResumeModal?.addEventListener(
    "click",
    closeResumePopup
);

function openResumeModal() {

    resumeModal.classList.add(
        "active"
    );

    populateResumeForm();
}

function closeResumePopup() {

    resumeModal.classList.remove(
        "active"
    );
}

/* ==========================================
   PROFILE FORM SUBMIT
========================================== */

const profileForm =
    document.getElementById(
        "profileForm"
    );

profileForm?.addEventListener(
    "submit",
    saveProfile
);

async function saveProfile(
    event
) {

    event.preventDefault();

    try {

        showLoader();

        /* ==========================
           USER UPDATE
        ========================== */

        const userPayload = {

            firstName:
                document.getElementById(
                    "firstName"
                ).value,

            lastName:
                document.getElementById(
                    "lastName"
                ).value,

            phoneNumber:
                document.getElementById(
                    "phoneNumber"
                ).value
        };

        const userResponse =
            await fetch(
                "/api/users/profile",
                {
                    method: "PUT",

                    headers: {
                        "Content-Type":
                            "application/json",

                        Authorization:
                            "Bearer " +
                            getToken()
                    },

                    body:
                        JSON.stringify(
                            userPayload
                        )
                }
            );

        if (!userResponse.ok) {

            throw new Error(
                "User update failed"
            );
        }

        /* ==========================
           CANDIDATE UPDATE
        ========================== */

        const dateOfBirth =
            document.getElementById("dateOfBirth").value;

        const expectedSalary =
            document.getElementById("expectedSalary").value;

        const candidatePayload = {

            profileHeadline:
                document.getElementById(
                    "profileHeadline"
                ).value,

            professionalSummary:
                document.getElementById(
                    "professionalSummary"
                ).value,

            dateOfBirth:
                dateOfBirth || null,

            gender:
                document.getElementById(
                    "gender"
                ).value,

            yearsOfExperience:
                parseFloat(
                    document.getElementById(
                        "yearsOfExperience"
                    ).value || 0
                ),

            currentLocation:
                document.getElementById(
                    "currentLocation"
                ).value,

            linkedinUrl:
                document.getElementById(
                    "linkedinUrl"
                ).value,

            githubUrl:
                document.getElementById(
                    "githubUrl"
                ).value,

            portfolioUrl:
                document.getElementById(
                    "portfolioUrl"
                ).value,

            highestQualification:
                document.getElementById(
                    "highestQualification"
                ).value,

            currentJobTitle:
                document.getElementById(
                    "currentJobTitle"
                ).value,

            expectedSalary:
                expectedSalary === "" ? null : Number(expectedSalary),

            profilePicture: null
        };
        const isNewProfile = !candidateProfile;
        const profileRequestPayload = isNewProfile
            ? {
                profileHeadline: candidatePayload.profileHeadline,
                professionalSummary: candidatePayload.professionalSummary,
                currentJobTitle: candidatePayload.currentJobTitle,
                location: candidatePayload.currentLocation,
                linkedinUrl: candidatePayload.linkedinUrl,
                githubUrl: candidatePayload.githubUrl,
                portfolioUrl: candidatePayload.portfolioUrl,
                expectedSalary: candidatePayload.expectedSalary
            }
            : candidatePayload;

        const response =
            await fetch(
                "/api/candidate/profile",
                {
                    method: isNewProfile ? "POST" : "PUT",

                    headers: {

                        "Content-Type":
                            "application/json",

                        Authorization:
                            "Bearer " +
                            getToken()
                    },

                    body:
                        JSON.stringify(
                            profileRequestPayload
                        )
                }
            );

        if (!response.ok) {

            throw new Error(
                "Profile update failed"
            );
        }

        showToast(
            "Profile updated successfully",
            "success"
        );

        closeProfilePopup();

        await refreshDashboard();

    }
    catch (error) {

        console.error(error);

        showToast(
            "Profile update failed",
            "error"
        );
    }
    finally {

        hideLoader();
    }
}

async function loadApplications() {

    try {

        applications =
            await getRequest(
                "/api/applications/my-applications"
            );

        renderApplications(
            applications
        );

    } catch (error) {

        console.error(error);
    }
}

async function loadOpportunityApplications() {
    try {
        const result = await getRequest("/api/opportunities/my-applications");
        opportunityApplications = Array.isArray(result) ? result : [];
    } catch (error) {
        opportunityApplications = [];
        console.error("Failed to load internship and hackathon application statuses", error);
    }
}

setInterval(async () => {

    try {

        await loadApplications();

        updateDashboardStatistics();

    }
    catch (error) {

        console.error(
            "Auto Refresh Error",
            error
        );
    }

}, 90000);
/* =====================================================
   END OF FILE
===================================================== */