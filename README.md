CodeCheck<sup>®</sup>
============

* [Description](https://codecheck.us)
* [Build Instructions](https://github.com/cayhorstmann/codecheck3/blob/main/build-instructions.md)
* [Implementation Notes](./implementation.md) — project architecture, exercise pages, setup steps, and GitHub workflow
* This project is the successor to [codecheck2](https://github.com/cayhorstmann/codecheck2). The web application is separated into a framework-independent service layer and a small framework-dependent layer, which currently uses Quarkus.

---

## What is CodeCheck?

CodeCheck is an open-source, web-based auto-grading platform for computer science education.

- **Instructors** create programming problems and assignments, upload them, and view student submissions with scores.
- **Students** write and submit code directly in the browser, get instant feedback, and resume work later via a private URL.
- **LMS Integration** via the LTI protocol (Moodle, Canvas, etc.) so grades flow back automatically.

Live production: **https://codecheck.io**

---

## Project Structure

| Component | Description |
|---|---|
| `codecheck-webapp` | Main web server (Java + Quarkus). Handles all HTTP routes, assignment management, and problem display. |
| `comrun` | Sandboxed code runner service. Compiles and executes submitted student code in isolation. |
| `cli/codecheck` | Command-line tool for testing problems locally without running the full server. |

---

## Quick Start

```bash
# Kill any old instance first
kill -9 $(ss -anop | grep 8080 | grep LISTEN | grep -oP 'pid=\K[0-9]+') 2>/dev/null; true

cd /home/pavankumar19/codecheck3
COMRUN_USER=$(whoami) mvn quarkus:dev
```

Then open **http://localhost:8080**.

---

## All Available URLs

### Exercise Listing Pages — browse problems by language

| URL | Content |
|---|---|
| `http://localhost:8080/python-questions.html` | ~130 Python problems (Branches, Strings, Lists, 2D Arrays) |
| `http://localhost:8080/java-objects-early.html` | 206 Java problems — objects-early curriculum |
| `http://localhost:8080/java-objects-late.html` | ~240 Java problems — objects-late curriculum |
| `http://localhost:8080/cpp-questions.html` | ~130 C++ problems (Branches, Strings, Arrays, 2D Arrays) |

Each page has two sections:
- **Weekly assignment links** (`/viewAssignment/...`) — grouped by week with scoring
- **Individual problem links** (`/files/wiley/...`) — practice any problem directly

---

### Individual Problem Pages

| URL | What it does |
|---|---|
| `http://localhost:8080/files/wiley/{problemName}` | Open a Wiley problem (e.g. `codecheck-python-Branches-1`) |
| `http://localhost:8080/files/{repo}/{problemName}` | Open a problem from any repo (`ext` = uploaded problems) |
| `http://localhost:8080/files?repo=wiley&problem={name}` | Same via query params |
| `http://localhost:8080/tracer/{repo}/{problemName}` | Open a tracer-style problem |
| `http://localhost:8080/fileData/wiley/{problemName}` | Raw JSON data for a problem |

---

### Assignment Pages

| URL | What it does |
|---|---|
| `http://localhost:8080/viewAssignment/{assignmentID}` | View a weekly assignment as instructor (48 available offline) |
| `http://localhost:8080/assignment/{assignmentID}` | **Student entry point** — starts a new session for an assignment |
| `http://localhost:8080/newAssignment` | Create a new assignment |
| `http://localhost:8080/copyAssignment/{assignmentID}` | Clone an existing assignment |
| `http://localhost:8080/private/assignment/{id}/{editKey}` | Instructor view of own assignment (shows edit/submission links) |
| `http://localhost:8080/private/editAssignment/{id}/{editKey}` | Edit an assignment |
| `http://localhost:8080/private/resume/{id}/{ccid}/{editKey}` | Student resumes work via private URL |
| `http://localhost:8080/private/submission/{id}/{ccid}/{editKey}` | Instructor views one student's submission |
| `http://localhost:8080/private/viewSubmissions/{id}/{editKey}` | Instructor views all submissions for an assignment |

> **Student assignment flow:** Students visit `/assignment/{id}` → get a unique session → must check **"I saved a copy of the private URL"** checkbox → problems appear in the page → submit code → click **Submit Assignment** to save score.

---

### Create / Upload Problems

| URL | What it does |
|---|---|
| `http://localhost:8080/assets/uploadProblem.html` | Create or upload your own problem (text editor + ZIP upload) |
| `http://localhost:8080/assets/uploadSingleFile.html` | Upload a single-file problem |
| `http://localhost:8080/private/problem/{id}/{editKey}` | Edit an uploaded problem |

---

### API Endpoints (called by browser JS — not meant for direct use)

| URL | Method | What it does |
|---|---|---|
| `/checkNJS` | POST JSON | Submit student code, returns score + HTML report + zip |
| `/run` | POST | Run code with input (Core Java pages) |
| `/saveWork` | POST JSON | Save student assignment progress |
| `/saveAssignment` | POST JSON | Save assignment definition |
| `/codecheck` | POST JSON | Create/update uploaded problem |
| `/health` | GET | Server health check |

---

## Offline Setup

713 problem ZIPs are pre-downloaded to `/opt/codecheck/repo/Problems/wiley/`.
48 weekly assignment JSONs are stored in `/opt/codecheck/repo/CodeCheckAssignments/`.
No internet required at runtime.

To re-download (if needed):
```bash
python3 download-wiley-problems.py   # problem ZIPs
python3 download-assignments.py      # weekly assignment data
```

> **Note:** Console errors starting with `chrome-extension://...` or mentioning `/generate/tone`, `/writing/get_template_list`, `/site_integration` are from the **Edge Copilot browser extension** — they are completely unrelated to this server and can be ignored.

---

See [build-instructions.md](./build-instructions.md) for full setup including `comrun` and prerequisites.
See [implementation.md](./implementation.md) for a detailed walkthrough of the implementation approach.

