#!/usr/bin/env python3
"""
download-wiley-problems.py
--------------------------
Downloads all Wiley/CodeCheck problem files from the live codecheck.io server
and stores them locally so the server runs fully offline.

Usage:
    python3 download-wiley-problems.py [--dest /opt/codecheck/repo/Problems/wiley]

What it does:
- Reads all problem IDs from the exercise listing HTML pages
- For each problem, calls GET https://codecheck.io/fileData/wiley/{problem}
  which returns the problem's visible file structure as JSON
- Reconstructs a ZIP containing those files and saves it to:
    {dest}/{problem}.zip

Limitations:
- The /fileData endpoint only returns files visible to students (description,
  editable files, use-files). Hidden solution/expected-output files are NOT
  included. Code checking (comparing to expected output) requires the full
  problem ZIP which must be obtained separately (e.g. from the textbook author).
- Display (showing the problem editor) works fully with these ZIPs.

Requirements:
    pip install requests
"""

import argparse
import io
import json
import os
import re
import sys
import zipfile

try:
    import requests
except ImportError:
    print("ERROR: 'requests' library not found. Install it with: pip install requests")
    sys.exit(1)

BASE_URL = "https://codecheck.io"
DEFAULT_DEST = "/opt/codecheck/repo/Problems/wiley"

# All 4 exercise listing pages
HTML_PAGES = [
    "src/main/resources/META-INF/resources/python-questions.html",
    "src/main/resources/META-INF/resources/java-objects-early.html",
    "src/main/resources/META-INF/resources/java-objects-late.html",
    "src/main/resources/META-INF/resources/cpp-questions.html",
]


def extract_problem_ids(html_dir):
    """Read all problem IDs from the local exercise listing HTML pages."""
    problem_ids = set()
    for page in HTML_PAGES:
        path = os.path.join(html_dir, page)
        if not os.path.exists(path):
            print(f"  WARN: page not found: {path}")
            continue
        with open(path, encoding="utf-8") as f:
            content = f.read()
        # Match /files/wiley/codecheck-* links (after our URL fix they are relative)
        # Also match the original https://codecheck.io/files/wiley/... just in case
        matches = re.findall(r'(?:codecheck\.io)?/files/wiley/(codecheck-[a-zA-Z0-9_-]+)', content)
        for m in matches:
            problem_ids.add(m)
    return sorted(problem_ids)


def build_zip(problem_id, data):
    """
    Reconstruct a minimal problem ZIP from the /fileData JSON response.

    The ZIP will contain:
      - description.html  (problem description, if present)
      - <filename>        (each student-editable file, with ##EDIT markers)
      - <filename>        (each read-only use-file)
    """
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as zf:

        # Description
        desc = data.get("description")
        if desc:
            zf.writestr("description.html", desc)

        # Required (editable) files
        required = data.get("requiredFiles") or {}
        for fname, editor_state in required.items():
            editors = (editor_state or {}).get("editors") or []
            # editors is a list of string sections; None means a "show" boundary
            parts = []
            for i, section in enumerate(editors):
                if section is None:
                    parts.append("##SHOW\n")
                elif i == 0:
                    parts.append(section if section else "")
                else:
                    parts.append("\n##EDIT\n" + (section if section else ""))
            content = "".join(parts)
            zf.writestr(fname, content)

        # Use (read-only) files
        use_files = data.get("useFiles") or {}
        for fname, content in use_files.items():
            zf.writestr(fname, content or "")

    return buf.getvalue()


def download_problem(problem_id, dest_dir, session):
    dest_path = os.path.join(dest_dir, f"{problem_id}.zip")
    if os.path.exists(dest_path):
        print(f"  SKIP {problem_id}  (already exists)")
        return "skip"

    url = f"{BASE_URL}/fileData/wiley/{problem_id}"
    try:
        resp = session.get(url, timeout=15)
        if resp.status_code == 404:
            print(f"  MISS {problem_id}  (HTTP 404)")
            return "miss"
        resp.raise_for_status()
        data = resp.json()
    except requests.RequestException as e:
        print(f"  ERR  {problem_id}  ({e})")
        return "error"
    except json.JSONDecodeError as e:
        print(f"  ERR  {problem_id}  (bad JSON: {e})")
        return "error"

    try:
        zip_bytes = build_zip(problem_id, data)
        with open(dest_path, "wb") as f:
            f.write(zip_bytes)
        print(f"  OK   {problem_id}  ({len(zip_bytes)} bytes)")
        return "ok"
    except Exception as e:
        print(f"  ERR  {problem_id}  (zip build failed: {e})")
        return "error"


def main():
    parser = argparse.ArgumentParser(description="Download Wiley problem files for offline CodeCheck")
    parser.add_argument("--dest", default=DEFAULT_DEST,
                        help=f"Directory to store problem ZIPs (default: {DEFAULT_DEST})")
    parser.add_argument("--repo-root", default=".",
                        help="Root of codecheck3 repo (to find exercise HTML pages)")
    args = parser.parse_args()

    print(f"Destination: {args.dest}")
    os.makedirs(args.dest, exist_ok=True)

    print("\nReading problem IDs from exercise pages...")
    problem_ids = extract_problem_ids(args.repo_root)
    print(f"Found {len(problem_ids)} unique problem IDs\n")

    if not problem_ids:
        print("ERROR: No problem IDs found. Run this script from the codecheck3 repo root.")
        sys.exit(1)

    stats = {"ok": 0, "skip": 0, "miss": 0, "error": 0}
    session = requests.Session()
    session.headers["User-Agent"] = "codecheck-offline-setup/1.0"

    for pid in problem_ids:
        result = download_problem(pid, args.dest, session)
        stats[result] += 1

    print(f"\nDone. OK={stats['ok']}  Skipped={stats['skip']}  "
          f"NotFound={stats['miss']}  Errors={stats['error']}")
    print("\nNOTE: Downloaded ZIPs contain visible problem files only.")
    print("      For full code-checking (pass/fail scoring), complete ZIPs")
    print("      (including hidden solution/test files) must be placed in:")
    print(f"      {args.dest}/")


if __name__ == "__main__":
    main()
