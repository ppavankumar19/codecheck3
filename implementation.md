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

## Implementation Approach

### Step 1 — Project Analysis

Explored the full repository structure using file-tree traversal and targeted searches:
- Mapped all controllers, services, and core checker classes in `src/main/java/`
- Identified all frontend static assets in `src/main/resources/META-INF/resources/`
- Read key source files: `AssignmentController.java`, `Assignment.java`, `CheckController.java`, `uploadProblem.html`
- Understood the assignment workflow for both students and instructors
- Read `build-instructions.md` to understand the comrun setup and Maven build process

### Step 2 — Scraping the Reference Site

The reference site at `https://horstmann.com/codecheck/` already had a Python page. We used it as the model:

1. Fetched `https://horstmann.com/codecheck/python-questions.html` with a web fetch tool to extract all problem/assignment URLs, category structure, and subcategory names
2. Repeated the same fetch process for Java (objects-early), Java (objects-late), and C++ pages from the same domain
3. Extracted all `https://codecheck.io/files/wiley/codecheck-*` problem links and `https://codecheck.io/viewAssignment/...` assignment links
4. Noted URL patterns per language:
   - Python: `codecheck-python-[Category]-[N]`
   - Java Early: `codecheck-bj-4-[category]-[N]`
   - Java Late: `codecheck-bjlo-1-*` and `codecheck-bj-4-*` combined
   - C++: `codecheck-cpp-[Category]-[N]` (note: `LoopsAlongaRoworCcolumn` with double `c` in 2D array sections)

### Step 3 — Created Exercise Listing Pages

Built four static HTML pages placed at `src/main/resources/META-INF/resources/` so Quarkus serves them at the root path.

| File | URL | Content |
|---|---|---|
| `python-questions.html` | `http://localhost:8080/python-questions.html` | 15 weekly assignments + ~130 Python problems across Branches, Strings, Lists, 2D Arrays |
| `java-objects-early.html` | `http://localhost:8080/java-objects-early.html` | 15 weekly assignments + 206 Java problems across Objects, Classes, Loops, Arrays, Recursion, Sorting, Data Structures, Generics, Concurrency, XML, Networking |
| `java-objects-late.html` | `http://localhost:8080/java-objects-late.html` | 15 weekly assignments + ~240 Java problems (objects-late curriculum: starts with loops/arrays, adds classes/interfaces later) |
| `cpp-questions.html` | `http://localhost:8080/cpp-questions.html` | 15 weekly assignments + ~130 C++ problems (same structure as Python: Branches, Strings, Arrays, 2D Arrays) |

All problem links point to `https://codecheck.io/files/wiley/...` and all assignment links point to `https://codecheck.io/viewAssignment/...` — the live production server.

Each page uses a consistent layout:
- `font-family: sans-serif; max-width: 960px; margin: 0 auto; padding: 1em 2em`
- Blue headings (`#0054a8`): `h3` for categories, `h4` for subcategories
- Numbered `<ol>` lists for problems within each subcategory
- Assignments section at the top (15 weekly links)
- Bug Report form at the bottom

### Step 4 — Set Up and Ran the Server

Installed prerequisites:
```
openjdk-21-jdk, maven, git, curl, zip, unzip
```

Set up the `comrun` code runner (must be run from the project root):
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

### Step 5 — GitHub Repository

Created a public GitHub repository and pushed each new file as an individual commit:

```bash
gh repo create ppavankumar19/codecheck3 --public
git remote add myfork https://github.com/ppavankumar19/codecheck3.git
git push myfork main   # push existing upstream history first
```

Individual commits pushed:
1. `python-questions.html` — "Add Python exercises listing page"
2. `java-objects-early.html` — "Add Java (Objects Early) exercises listing page"
3. `java-objects-late.html` — "Add Java (Objects Late) exercises listing page"
4. `cpp-questions.html` — "Add C++ exercises listing page"
5. `implementation.md` — "Add implementation notes documenting project architecture and session work"

**Repo:** `https://github.com/ppavankumar19/codecheck3`

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

## Offline Operation — 100% Offline

### All fixes applied to achieve full offline operation

| # | File | Problem | Fix |
|---|---|---|---|
| 1 | `application.properties` | Quarkus DevServices tried to start Docker/PostgreSQL on every boot | Added `quarkus.datasource.devservices.enabled=false` |
| 2 | All 4 exercise listing pages | ~600 problem/assignment links pointed to `https://codecheck.io/...` | Changed to relative URLs (`/files/wiley/...`, `/viewAssignment/...`) |
| 3 | `Files.java`, `LTIProblem.java` | Tracer CSS hard-coded to `https://horstmann.com/codecheck/css/codecheck_tracer.css` | Created local copy at `/assets/codecheck_tracer.css`, updated both Java files |
| 4 | `horstmann_codecheck.js` | Uses `window.location.origin` for all API calls | Already relative — no change needed |
| 5 | `Main.java` | `comrun.remote` vs `comrun.local` resolution | Already falls back to local; no change needed |
| 6 | 134 interactive exercises | Assignment problems referenced `https://horstmann.com/interactivities/*.xhtml` — loaded in iframes, unavailable offline | Downloaded all 134 XHTML files + shared JS/CSS assets to `/opt/codecheck/repo/Interactivities/`. Created `InteractivitiesController.java` to serve them at `/interactivities/{name}`. Updated `localizeAssignment()` in `StorageConnector.java` to rewrite external interactivity URLs to local paths |
| 7 | Bug report forms | All 4 exercise listing pages had forms posting to `https://horstmann.com/codecheck/bugReport` | Replaced with offline-friendly handler |
| 8 | `test.html` | Hardcoded `https://codecheck.me/checkNJS` | Changed to relative `/checkNJS` |
| 9 | `uploadProblem.html`, `Upload.java` | External link to `https://horstmann.com/codecheck/authoring.html` | Replaced with offline notice |

### Offline data

| Data | Location | Count |
|---|---|---|
| Problem ZIPs | `/opt/codecheck/repo/Problems/wiley/` | 713 |
| Assignment JSONs | `/opt/codecheck/repo/CodeCheckAssignments/` | 48 |
| Interactive exercises | `/opt/codecheck/repo/Interactivities/` | 134 XHTML + shared JS/CSS |

All 48 weekly assignments include **all** lessons with zero omissions.

### Download scripts

```bash
python3 download-wiley-problems.py      # 713 problem ZIPs
python3 download-assignments.py         # 48 weekly assignment JSONs
python3 download-interactivities.py     # 134 interactive exercises + shared assets
```

### Offline architecture

```
Student browser ──► localhost:8080 ──► LocalStorageConnection (/opt/codecheck/repo/)
                                   ──► comrun (/opt/codecheck/comrun)  [code execution]
                                   ──► Static assets served locally
                                   ──► Problem ZIPs served locally
                                   ──► Interactivities served locally via InteractivitiesController
                                   ──► Assignment work saved to local filesystem
```
**Zero** external network calls are made at runtime.

---

## Known Limitations (Local Dev)

| Issue | Cause | Impact |
|---|---|---|
| Database warning on startup | Docker not running, Quarkus cannot auto-start PostgreSQL | Non-critical; local filesystem storage works fine |
| `comrun` not fully integrated | Requires Docker or manual process setup | Code checking via `/run` may not work locally |
| Static pages work fully | No database needed | All 4 exercise listing pages work fine |
