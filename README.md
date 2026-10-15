# CareerSync AI

## AI-Powered Career Guidance Platform

### Overview

CareerSync AI is a web-based career platform designed to help candidates discover job opportunities, manage resumes, and receive AI-powered career guidance. It also provides recruiters with tools to manage job listings and identify suitable candidates.

The platform integrates Google Gemini API to provide career-related assistance and general knowledge responses.

### Key Features

- **User Authentication:** Secure login and registration with Spring Security and JWT.
- **Candidate Dashboard:** Manage profile information, skills, education, experience, and resumes.
- **Resume Management:** Upload and maintain resume details.
- **Job and Internship Discovery:** Explore relevant jobs, internships, and other career opportunities.
- **AI-Based Job Matching:** Match candidate skills with job requirements.
- **Recruiter Dashboard:** Create and manage job listings and review candidate profiles.
- **Candidate Ranking:** Help recruiters identify candidates based on job requirements and relevant skills.
- **AI Career Assistant:** Get career guidance, learning suggestions, and general knowledge responses using Google Gemini API.
- **Application Management:** Track job applications and manage application-related information.

### Technology Stack

**Backend**
- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- JWT Authentication

**Frontend**
- HTML
- CSS
- JavaScript

**Database**
- MySQL

**AI Integration**
- Google Gemini API

**Build Tool**
- Maven

### System Architecture

The application follows a layered architecture:

- **Frontend Layer:** Provides user interfaces for candidates and recruiters.
- **Backend Layer:** Processes requests, manages business logic, and handles authentication.
- **Database Layer:** Stores application data using MySQL.
- **AI Integration Layer:** Connects the backend with Google Gemini API for AI-powered features.

### Installation and Setup

#### Prerequisites

- Java Development Kit (JDK)
- MySQL Server
- Maven or the project's Maven Wrapper
- A compatible code editor, such as Visual Studio Code
- Google Gemini API key

#### Steps

1. Clone the repository.

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
```

2. Open the project directory.

```bash
cd <PROJECT_FOLDER>
```

3. Create the required MySQL database using the database name configured in the project.

4. Configure the database connection and Gemini API key in the application's configuration or environment variables.

5. Start the application using the project's configured Maven command.

```powershell
.\mvnw.cmd spring-boot:run
```

Verify the database name, configuration properties, and startup commands against the actual project files before running the application.

### Testing

Run the project's configured test suite using the Maven Wrapper:

```powershell
.\mvnw.cmd verify
```

Successful automated tests do not necessarily confirm that every feature works with a live MySQL database or a valid Gemini API key. Test the application environment and major user workflows separately.

### Project Objectives

- Simplify job and internship discovery.
- Improve the connection between candidate skills and job requirements.
- Support resume and application management.
- Provide accessible AI-powered career guidance.
- Help recruiters review and identify relevant candidates.

### Future Enhancements

- Advanced resume parsing and skill extraction.
- Improved job recommendation algorithms.
- More detailed application analytics.
- Expanded career-planning and learning recommendations.
- Additional validation and integration testing.

### Disclaimer

AI-generated responses and job recommendations may contain inaccuracies. Users should independently verify important career information and opportunity details.

### License

Add a license only if you have selected and included one for this project.
