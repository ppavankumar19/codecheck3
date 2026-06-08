#!/usr/bin/env python3
"""
Download weekly assignment data from codecheck.io and store locally so
viewAssignment links work offline.

Run once while online:
    python3 download-assignments.py
"""

import json
import os
import re
import sys
import time
import urllib.request
from pathlib import Path

STORAGE_DIR = Path("/opt/codecheck/repo/CodeCheckAssignments")
RESOURCES_DIR = Path("src/main/resources/META-INF/resources")
BASE_URL = "https://codecheck.io/viewAssignment/"

# Extract all viewAssignment IDs from the 4 exercise pages
def collect_ids():
    ids = {}  # id -> label
    pattern = re.compile(r'/viewAssignment/([a-zA-Z0-9]+)"[^>]*>([^<]+)')
    for html_file in RESOURCES_DIR.glob("*.html"):
        text = html_file.read_text()
        for m in pattern.finditer(text):
            ids[m.group(1)] = m.group(2).strip()
    return ids

def fetch_assignment(assignment_id):
    url = BASE_URL + assignment_id
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            html = resp.read().decode("utf-8")
    except Exception as e:
        return None, str(e)

    m = re.search(r'const assignment\s*=\s*(\{.*?\})\s*\n', html)
    if not m:
        return None, "could not find 'const assignment' in page"

    try:
        data = json.loads(m.group(1))
    except json.JSONDecodeError as e:
        return None, f"JSON parse error: {e}"

    return data, None

def rewrite_urls(problems):
    """Keep locally cached problems and rewrite their URLs to local-relative."""
    offline_problems = []
    for p in problems:
        url = p.get("URL", "")
        qid = p.get("qid", "")
        for prefix in ("https://codecheck.us/files/wiley/",
                        "http://codecheck.us/files/wiley/",
                        "https://codecheck.io/files/wiley/"):
            if url.startswith(prefix):
                p["URL"] = "/files/wiley/" + url[len(prefix):]
                break
        if p["URL"].startswith(("/", "http://localhost", "http://127.0.0.1")):
            offline_problems.append(p)
        elif qid and (STORAGE_DIR.parent / "Problems" / "wiley" / f"{qid}.zip").is_file():
            p["URL"] = f"/files/wiley/{qid}"
            offline_problems.append(p)
    return offline_problems

def main():
    os.makedirs(STORAGE_DIR, exist_ok=True)
    ids = collect_ids()
    if not ids:
        print("No viewAssignment IDs found — run from the codecheck3 directory")
        sys.exit(1)

    print(f"Found {len(ids)} assignment IDs to download\n")

    ok = 0
    skip = 0
    fail = 0

    for assignment_id, label in sorted(ids.items(), key=lambda x: x[1]):
        dest = STORAGE_DIR / assignment_id
        if dest.exists():
            data = json.loads(dest.read_text())
            problems = rewrite_urls([p for group in data.get("problems", []) for p in group])
            if problems:
                data["problems"] = [problems]
                dest.write_text(json.dumps(data))
                print(f"  local {label}  ({len(problems)} problems)")
                skip += 1
            else:
                print(f"  WARN  {label}: no problems are available offline")
                fail += 1
            continue

        data, err = fetch_assignment(assignment_id)
        if err:
            print(f"  FAIL  {label}: {err}")
            fail += 1
            time.sleep(0.5)
            continue

        # Remove server-injected runtime fields
        for field in ("isStudent", "comment", "cloneURL", "clearIDURL",
                       "returnToWorkURL", "editKeySaved", "sentAt",
                       "viewSubmissionsURL", "privateURL", "publicURL",
                       "editAssignmentURL"):
            data.pop(field, None)

        # Ensure assignmentID is present
        data["assignmentID"] = assignment_id

        # Rewrite codecheck.us URLs to local /files/wiley/...
        problems = rewrite_urls(data.get("problems", []))
        if not problems:
            print(f"  WARN  {label}: no problems are available offline")
            fail += 1
            continue

        # The local server's work() expects problems as [[p1,p2,...]] (array of groups).
        # The page embeds one pre-selected flat group — wrap it back.
        data["problems"] = [problems]

        dest.write_text(json.dumps(data))
        print(f"  OK    {label}  ({len(problems)} problems)")
        ok += 1
        time.sleep(0.3)  # be polite

    print(f"\nDone: {ok} downloaded, {skip} already cached, {fail} failed")
    if fail:
        print("Re-run to retry failed downloads.")

if __name__ == "__main__":
    main()
