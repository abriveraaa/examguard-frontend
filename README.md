# ExamGuard Frontend (JavaFX)

Frontend application for ExamGuard, a secure examination and monitoring system designed for students, faculty members, and administrators.

---

## Overview

ExamGuard is a desktop-based examination platform that provides secure exam delivery, monitoring, result management, and academic administration features. This repository contains the JavaFX frontend application that communicates with the ExamGuard backend services.

---

## Technology Stack

* Java 21
* JavaFX
* Maven Wrapper (Included)
* REST API Integration
* IntelliJ IDEA (Recommended)

---

## Prerequisites

Before running the project, ensure the following software is installed on your machine:

### Required

* Java JDK 21 or later
* Git

### Recommended

* IntelliJ IDEA Community or Ultimate Edition

### Not Required

Maven installation is **not required** because the project already includes Maven Wrapper (`mvnw` and `mvnw.cmd`).

---

## Verifying Java Installation

Open a terminal and run:

```bash
java -version
```

Expected output:

```text
java version "21.x.x"
```

If Java is not installed, download and install JDK 21 before proceeding.

---

## Cloning the Repository

```bash
git clone <repository-url>
cd examguard-frontend
```

---

## Opening the Project

### IntelliJ IDEA

1. Open IntelliJ IDEA.
2. Select **Open**.
3. Navigate to the cloned repository.
4. Open the project folder.
5. Wait for IntelliJ to import the Maven project and download all dependencies.

---

## Backend Configuration

The frontend requires a running ExamGuard backend server.

Verify the backend URL configured in the application properties files:

```text
src/main/resources/application.properties
src/main/resources/application-dev.properties
src/main/resources/application-prod.properties
```

Example:

```properties
api.base.url=http://localhost:8080
```

For production deployments, replace the URL with the deployed backend endpoint.

---

## Running the Application

### Option 1: IntelliJ IDEA (Recommended)

Run:

```text
Main.java
```

### Option 2: Command Line

#### Windows

```cmd
mvnw.cmd javafx:run
```

#### macOS / Linux

```bash
./mvnw javafx:run
```

---

## Building the Application

### Windows

```cmd
mvnw.cmd clean package
```

### macOS / Linux

```bash
./mvnw clean package
```

Build artifacts will be generated inside:

```text
target/
```

---

### Backend Connection Failed

Verify that:

* The backend server is running.
* The configured backend URL is correct.
* The backend environment is accessible from your network.
* Firewalls are not blocking the connection.

---

## Notes

* The backend server must be running before launching the frontend application.
* PostgreSQL is managed through the backend and does not need to be configured in the frontend project.
* Camera permissions may be requested by the operating system depending on enabled exam monitoring features.
* The first application startup may take longer while dependencies are being downloaded.