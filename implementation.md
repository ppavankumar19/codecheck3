# CodeCheck3 — Implementation Notes

## What is CodeCheck?

CodeCheck is an open-source, web-based auto-grading platform for computer science education. It allows:

- **Instructors** to create programming problems and assignments, upload them, and view student submissions with scores.
- **Students** to write and submit code directly in the browser, get instant feedback, and resume their work later via a private URL.
- **LMS Integration** via the LTI protocol (Moodle, Canvas, etc.) so grades flow back automatically to a Learning Management System.

The live production version runs at **https://codecheck.io** and is maintained by Cay Horstmann.

---

## Project Architecture

### Three Components

| Component | Description |
|---|---|
| `codecheck-webapp` | The main web server (Java + Quarkus). Handles all HTTP routes, assignment management, and problem display. |
| `comrun` | A sandboxed code runner service. Compiles and executes submitted student code in isolation. |
| `cli/codecheck` | A command-line tool for testing problems locally without running the full server. |

---

### Backend — Java / Quarkus (JAX-RS REST)

**Controllers** (`src/main/java/controllers/`)

| Controller | Routes | Purpose |
|---|---|---|
| `CheckController` | `POST /run`, `POST /checkNJS`, `GET /setupData` | Runs code checks and returns results |
| `AssignmentController` | `GET /newAssignment`, `GET /assignment/{id}`, `POST /saveWork`, etc. | Full assignment lifecycle for students and instructors |
| `UploadController` | `POST /uploadProblem`, `GET /private/problem/{id}/{key}`, `POST /codecheck` | Problem creation and editing |
| `LTIAssignmentController` | `POST /lti/createAssignment`, `POST /assignment/{id}` (LTI launch), etc. | LMS/LTI integration |
| `LTIProblemController` | LTI problem launch routes | LTI problem delivery |
| `FilesController` | Serves problem files | File access |
| `Health` | `GET /healthz` | Health check |

**Services** (`src/main/java/services/`)

| Service | Purpose |
|---|---|
| `Assignment` | Assignment CRUD, scoring logic, HTML page generation |
| `LTIAssignment` | LTI-flavored assignment flow with grade passback |
| `Check` | Orchestrates code execution via comrun |
| `Upload` | Problem validation and storage |
| `CodeCheck` | Core checker wiring |
| `StorageConnector` | Abstract storage layer (local filesystem, AWS S3 + DynamoDB, or PostgreSQL) |
| `JWT` | Cookie-based auth tokens for instructor sessions |
| `LTI` | OAuth signature verification for LTI requests |

**Core Checker** (`src/main/java/com/horstmann/codecheck/`)

| Package | Purpose |
|---|---|
| `checker/` | `Plan`, `Problem`, `Substitution`, `Comparison`, `Score`, `Annotations` — the actual test execution logic |
| `language/` | 14 language handlers: Python, Java, C, C++, Kotlin, Rust, Scala, Haskell, Dart, JavaScript, PHP, Matlab, SML, Racket, Bash, C# |
| `report/` | Output formatters: HTML, Text, JSON, NJS (for iframe embedding) |

---

### Frontend — Static Assets (`src/main/resources/META-INF/resources/`)

| File | Served At | Purpose |
|---|---|---|
| `assets/uploadProblem.html` | `/assets/uploadProblem.html` | Instructor creates/edits problems |
| `assets/uploadSingleFile.html` | `/assets/uploadSingleFile.html` | Single-file upload variant |
| `assets/test.html` | `/assets/test.html` | Embeds a single CodeCheck problem via widget |
| `assets/editAssignment.js` | `/assets/editAssignment.js` | Assignment editor UI logic |
| `assets/workAssignment.js` | `/assets/workAssignment.js` | Student assignment UI (iframes, scoring) |
| `assets/viewSubmissions.js` | `/assets/viewSubmissions.js` | Instructor submissions table |
| `assets/horstmann_codecheck.js` | `/assets/horstmann_codecheck.js` | Core embedding widget used in textbooks |
| `assets/codecheck.js` | `/assets/codecheck.js` | Problem display logic |
| `assets/codecheck.css` | `/assets/codecheck.css` | Shared styles across all pages |

---

### Key Routes Summary

| Route | Who Uses It | What It Does |
|---|---|---|
| `GET /newAssignment` | Instructor | Create a new assignment |
| `GET /private/editAssignment/{id}/{key}` | Instructor | Edit an existing assignment |
| `GET /copyAssignment/{id}` | Instructor | Clone an assignment |
| `GET /assignment/{id}` | Student | Start working on an assignment |
| `GET /private/resume/{id}/{ccid}/{key}` | Student | Resume saved work |
| `GET /private/submission/{id}/{ccid}/{key}` | Instructor | View a student's submission |
| `GET /private/viewSubmissions/{id}/{key}` | Instructor | See all student submissions |
| `GET /private/problem/{id}/{key}` | Instructor | Edit a problem |
| `POST /uploadProblem` | Instructor | Upload a zipped problem |
| `POST /checkNJS` | Browser widget | Check student code, return JSON result |
| `POST /run` | CLI / API | Run code check, return plain text result |

---

### Storage Options

The `StorageConnector` supports three backends, configured in `application.properties`:

| Backend | Use Case |
|---|---|
| Local filesystem (`/opt/codecheck/repo`) | Local development |
| AWS S3 + DynamoDB | Production deployment |
| PostgreSQL | Alternative production option |

---

## What We Did in This Session

### 1. Project Analysis

Performed a full analysis of the codecheck3 repository:
- Mapped all controllers, services, and core checker classes
- Identified all existing frontend static assets
- Understood the assignment workflow for both students and instructors
- Documented all API routes and their purposes

### 2. Created Exercise Listing Pages

Modelled after the live pages on `https://horstmann.com/codecheck/`, we created four static HTML exercise listing pages. All pages are served by the Quarkus server at the root path.

| File | URL | Content |
|---|---|---|
| `python-questions.html` | `http://localhost:8080/python-questions.html` | 15 weekly assignments + ~130 Python problems across Branches, Strings, Lists, 2D Arrays |
| `java-objects-early.html` | `http://localhost:8080/java-objects-early.html` | 15 weekly assignments + 206 Java problems across Objects, Classes, Loops, Arrays, Recursion, Sorting, Data Structures, Generics, Concurrency, XML, Networking |
| `java-objects-late.html` | `http://localhost:8080/java-objects-late.html` | 15 weekly assignments + ~240 Java problems (objects-late curriculum: starts with loops/arrays, adds classes/interfaces later) |
| `cpp-questions.html` | `http://localhost:8080/cpp-questions.html` | 15 weekly assignments + ~130 C++ problems (same structure as Python: Branches, Strings, Arrays, 2D Arrays) |

All problem links point to `https://codecheck.io/files/wiley/...` and all assignment links point to `https://codecheck.io/viewAssignment/...` — the live production server.

Each page has:
- A consistent clean style (blue headings, sans-serif font, 960px max-width)
- Assignments section (15 weekly links)
- Programming Problems section organized by category and subcategory with numbered lists
- A Bug Report form at the bottom

### 3. Set Up and Ran the Server

Installed prerequisites:
```
openjdk-21-jdk, maven, git, curl, zip, unzip
```

Set up the `comrun` code runner:
```bash
cd /home/pavankumar19/codecheck3
mkdir -p comrun/bin/lib && cd comrun/bin/lib
# Downloaded checkstyle, hamcrest, junit JARs
sudo cp -R comrun/bin/* /opt/codecheck
sudo chmod +x /opt/codecheck/comrun
```

Built the project:
```bash
mvn package -Dmaven.test.skip
```

Started the server:
```bash
COMRUN_USER=$(whoami) mvn quarkus:dev
```

Server runs at **http://localhost:8080**

---

## How to Run (Quick Reference)

```bash
cd /home/pavankumar19/codecheck3
COMRUN_USER=$(whoami) mvn quarkus:dev
```

Then open:
- `http://localhost:8080/python-questions.html`
- `http://localhost:8080/java-objects-early.html`
- `http://localhost:8080/java-objects-late.html`
- `http://localhost:8080/cpp-questions.html`
- `http://localhost:8080/assets/uploadProblem.html`

---

## Known Limitations (Local Dev)

| Issue | Cause | Impact |
|---|---|---|
| Database warning on startup | Docker not running, Quarkus cannot auto-start PostgreSQL | Assignment save/load features do not work |
| `comrun` not fully integrated | Requires Docker or manual process setup | Code checking via `/run` may not work locally |
| Static pages work fully | No database needed | All 4 exercise listing pages work fine |
