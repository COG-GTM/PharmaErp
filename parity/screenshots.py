#!/usr/bin/env python3
"""Headless screenshots for the parity report (legacy :8081, migrated front door :8080).

Usage: python3 parity/screenshots.py [--out parity/report/screenshots]
Needs python Playwright; uses its bundled Chromium, or CHROME_PATH / google-chrome if that is not installed.
Every shot is attempted; failures are recorded in <out>/manifest.json instead of aborting the run.
"""
import argparse, json, os, shutil, traceback
from playwright.sync_api import sync_playwright

LEGACY = os.environ.get("LEGACY", "http://127.0.0.1:8081")
FRONT = os.environ.get("FRONT", "http://127.0.0.1:8080")
USER, PASSWORD = "tarang", "parisdkh"


def launch(p):
    try:
        return p.chromium.launch()
    except Exception:
        exe = os.environ.get("CHROME_PATH") or shutil.which("google-chrome") or shutil.which("chromium")
        return p.chromium.launch(executable_path=exe, args=["--no-sandbox"])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="parity/report/screenshots")
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    manifest = []
    with sync_playwright() as p:
        browser = launch(p)
        ctx = browser.new_context(viewport={"width": 1280, "height": 800})
        page = ctx.new_page()

        def shot(name, title, action, full_page=False):
            try:
                action()
                page.wait_for_timeout(400)
                path = os.path.join(a.out, name + ".png")
                page.screenshot(path=path, full_page=full_page)
                manifest.append({"name": name, "title": title, "file": name + ".png", "url": page.url, "ok": True})
            except Exception as e:
                manifest.append({"name": name, "title": title, "ok": False, "error": f"{type(e).__name__}: {e}"})
                traceback.print_exc()

        def legacy_success():
            page.goto(LEGACY + "/redirect")
            page.fill("input[name=email]", USER)
            page.fill("input[name=password]", PASSWORD)
            with page.expect_navigation():
                page.locator("input[type=submit], button[type=submit], button").first.click()

        def spa_login():
            page.goto(FRONT + "/signin")
            page.fill("input[name=email]", USER)
            page.fill("input[name=password]", PASSWORD)
            page.click("button[type=submit]")
            page.wait_for_url(lambda u: "/signin" not in u, timeout=15000)
            page.wait_for_load_state("networkidle")

        def goto(path, wait_text=None):
            def f():
                page.goto(FRONT + path)
                page.wait_for_load_state("networkidle")
                if wait_text:
                    page.get_by_text(wait_text).first.wait_for(timeout=15000)
            return f

        shot("legacy-login", "Legacy JSP login page (GET /redirect, :8081); its error callouts show on first load (legacy UI defect)", lambda: page.goto(LEGACY + "/redirect"))
        shot("legacy-login-success", "Legacy login success: POST /login returns the JWT as a JSON string", legacy_success)
        shot("migrated-login", "Migrated React sign-in page (GET /redirect -> /signin, :8080)",
             lambda: (page.goto(FRONT + "/redirect"), page.wait_for_load_state("networkidle")))
        shot("migrated-login-error", "Migrated failed legacy-style login (F05): /login 302 -> /signin?error (no error banner: known limitation)",
             lambda: (page.goto(FRONT + "/login?email=tarang&password=wrongpass"), page.wait_for_load_state("networkidle")))
        shot("migrated-login-success", "Migrated legacy-compat login success: GET /login returns the JWT as a JSON string",
             lambda: page.goto(FRONT + f"/login?email={USER}&password={PASSWORD}"))
        shot("migrated-dashboard", "Migrated dashboard after SPA sign-in", lambda: (spa_login(), goto("/dashboard")()), True)
        shot("migrated-inventory", "Migrated inventory with expiry / low-stock badges", goto("/inventory", "Expiry"), True)
        shot("migrated-invoice-gst", "Migrated invoice receipt with GST breakdown (INV-000002)",
             goto("/invoices/2", "GST breakdown"), True)
        shot("migrated-clients", "Migrated clients list", goto("/clients"), True)
        browser.close()
    json.dump(manifest, open(os.path.join(a.out, "manifest.json"), "w"), indent=2)
    for m in manifest:
        print(("OK  " if m["ok"] else "FAIL") + " " + m["name"] + ("" if m["ok"] else "  " + m["error"]))


if __name__ == "__main__":
    main()
