#!/usr/bin/env python3
"""
Download all horstmann.com interactivity exercises referenced in assignments
for offline use. Also downloads their shared CSS/JS assets.

Usage:
    python3 download-interactivities.py
"""

import json
import os
import sys
import urllib.request
import urllib.error

ASSIGNMENTS_DIR = "/opt/codecheck/repo/CodeCheckAssignments"
OUTPUT_DIR = "/opt/codecheck/repo/Interactivities"
BASE_URL = "https://horstmann.com/interactivities"

# Shared assets needed by the interactivity pages (relative to BASE_URL)
SHARED_ASSETS = [
    "script/horstmann_all_min.js",
    "css/horstmann_all_min.css",
]

def collect_external_urls():
    """Scan all assignment JSONs and collect unique horstmann.com interactivity URLs."""
    urls = set()
    for fname in os.listdir(ASSIGNMENTS_DIR):
        fpath = os.path.join(ASSIGNMENTS_DIR, fname)
        if not os.path.isfile(fpath):
            continue
        with open(fpath) as f:
            data = json.load(f)
        for group in data.get("problems", []):
            for p in group:
                url = p.get("URL", "")
                if url.startswith("https://horstmann.com/interactivities/"):
                    urls.add(url)
    return sorted(urls)

def download_file(url, dest):
    """Download a URL to a local file path."""
    os.makedirs(os.path.dirname(dest), exist_ok=True)
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = resp.read()
        with open(dest, "wb") as f:
            f.write(data)
        return True
    except (urllib.error.URLError, urllib.error.HTTPError, OSError) as e:
        print(f"  FAILED: {e}")
        return False

def main():
    urls = collect_external_urls()
    print(f"Found {len(urls)} unique interactivity URLs in assignments")

    os.makedirs(OUTPUT_DIR, exist_ok=True)

    # Download shared assets
    print("\n--- Downloading shared assets ---")
    for asset in SHARED_ASSETS:
        url = f"{BASE_URL}/{asset}"
        dest = os.path.join(OUTPUT_DIR, asset)
        print(f"  {asset} ... ", end="", flush=True)
        if os.path.exists(dest):
            print("already exists")
        elif download_file(url, dest):
            print(f"OK ({os.path.getsize(dest)} bytes)")
        else:
            print("FAILED")

    # Download XHTML pages
    print(f"\n--- Downloading {len(urls)} interactivity pages ---")
    success = 0
    skipped = 0
    failed = 0
    for i, url in enumerate(urls, 1):
        # Extract filename from URL: https://horstmann.com/interactivities/NAME.xhtml -> NAME.xhtml
        name = url.split("/interactivities/")[-1]
        dest = os.path.join(OUTPUT_DIR, name)

        if os.path.exists(dest):
            skipped += 1
            continue

        print(f"  [{i}/{len(urls)}] {name} ... ", end="", flush=True)
        if download_file(url, dest):
            print(f"OK ({os.path.getsize(dest)} bytes)")
            success += 1
        else:
            failed += 1

    print(f"\nDone: {success} downloaded, {skipped} already existed, {failed} failed")
    print(f"Total files in {OUTPUT_DIR}: {len(os.listdir(OUTPUT_DIR))}")

if __name__ == "__main__":
    main()
