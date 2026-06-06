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

## Exercise Listing Pages

Four static exercise listing pages are included, served at the root path by the Quarkus server:

| Page | URL | Content |
|---|---|---|
| `python-questions.html` | `/python-questions.html` | ~130 Python problems across Branches, Strings, Lists, 2D Arrays |
| `java-objects-early.html` | `/java-objects-early.html` | 206 Java problems (objects-early curriculum) |
| `java-objects-late.html` | `/java-objects-late.html` | ~240 Java problems (objects-late curriculum) |
| `cpp-questions.html` | `/cpp-questions.html` | ~130 C++ problems across Branches, Strings, Arrays, 2D Arrays |

All problem links point to `https://codecheck.io/files/wiley/...` on the live production server.

---

## Quick Start

```bash
cd /home/pavankumar19/codecheck3
COMRUN_USER=$(whoami) mvn quarkus:dev
```

Then open `http://localhost:8080`.

See [build-instructions.md](./build-instructions.md) for full setup including `comrun` and prerequisites.
See [implementation.md](./implementation.md) for a detailed walkthrough of the implementation approach.

