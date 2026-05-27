# ExamGuard Frontend | JavaFX

Frontend application for ExamGuard, a secure examination and monitoring system for students, faculty, and administrators.

---

## System Requirements

* Java JDK 21+
* Maven 3.9+
* Python 3.11+
* IntelliJ IDEA (recommended)

Built using:

* JavaFX
* Maven
* Python (AI Proctoring)
* REST API

---

## How to Use

### Preparation

* Clone repository

```bash
git clone <frontend-repository-url>
cd examguard-frontend
```

* Install Java dependencies

```bash
mvn clean install
```

* Create Python virtual environment

```bash
cd ai-runtime/mediapipe-face

python3 -m venv .venv
```

* Activate environment

Mac/Linux:

```bash
source .venv/bin/activate
```

Windows:

```bash
.venv\Scripts\activate
```

* Install Python dependencies

```bash
pip install -r requirements.txt
```

* Return to project root

```bash
cd ../..
```

* Run application

```bash
mvn javafx:run
```

Or run directly from IntelliJ:

```text
Open project → Run Main.java
```

---

Backend server must be running:

```text
http://localhost:8080
```

## Notes

Ensure backend and PostgreSQL are running before launching the application.

Camera permissions may be required on first launch.
