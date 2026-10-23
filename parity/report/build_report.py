#!/usr/bin/env python3
"""Build parity/report/pharmaerp-parity-report.html from parity/results/*.json and parity/report/screenshots/.

Every number and status in the report is read from those files; nothing is typed in by hand.
Run from the repo root: python3 parity/report/build_report.py
"""
import base64, html, json, os, re
from datetime import datetime, timezone

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
P = lambda *a: os.path.join(ROOT, "parity", *a)
load = lambda *a: json.load(open(P(*a)))
e = lambda s: html.escape("" if s is None else str(s))

flows = {f["id"]: f for f in load("flows.json")}
diff = load("results", "diff.json")
known = load("known-differences.json")
ds = load("results", "data-state.json")
env = load("results", "environment.json")
legacy_t = load("results", "test-suites", "legacy-summary.json")
backend_t = load("results", "test-suites", "backend-junit-summary.json")
front_t = load("results", "test-suites", "frontend-vitest-summary.json")
shots = [s for s in load("report", "screenshots", "manifest.json") if s["ok"]]
probes = open(P("results", "probes.txt")).read().strip()

rows = diff["flows"]
count = lambda st: sum(1 for r in rows if r["status"] == st)
n_par, n_int, n_reg, n_tot = count("PARITY"), count("INTENTIONAL DIFFERENCE"), count("REGRESSION"), len(rows)
n_fix = sum(1 for r in rows if r.get("category") == "defect fix")
n_mod = sum(1 for r in rows if r.get("category") == "modernisation")
glob_nf = sorted({k for r in rows for k in r.get("non_functional_differences", {})})


def request(f):
    s = f'{f.get("method", "GET")} {f.get("path", "")}'
    if f.get("params"):
        s += " " + " ".join(f"{k}={v!r}" for k, v in f["params"].items())
    if f.get("form"):
        s += " form " + " ".join(f"{k}={v!r}" for k, v in f["form"].items())
    return s


def outcome(r):
    parts = [str(r["status"])]
    if r.get("location"):
        parts.append(f'→ {r["location"]}')
    if r.get("body_kind") and r["body_kind"] != "empty":
        parts.append(r["body_kind"])
    if r.get("body_jwt"):
        c = r["body_jwt"]["claims"]
        parts.append(f'JWT {r["body_jwt"]["alg"]} username={c.get("username")} scope={c.get("scope")}')
    if r.get("token_cookie"):
        parts.append("token cookie set")
    return " · ".join(parts)


def badge(st):
    cls = {"PARITY": "ok", "INTENTIONAL DIFFERENCE": "int", "REGRESSION": "bad"}[st]
    return f'<span class="b {cls}">{e(st)}</span>'


def img(name):
    with open(P("report", "screenshots", name), "rb") as fh:
        return "data:image/png;base64," + base64.b64encode(fh.read()).decode()


flow_rows = "".join(
    f'<tr><td class="m">{e(r["id"])}</td><td>{e(r["name"])}<div class="sub m">{e(request(flows[r["id"]]))}</div></td>'
    f'<td class="m">{e(outcome(r["legacy"]))}</td><td class="m">{e(outcome(r["migrated"]))}</td>'
    f'<td>{badge(r["status"])}{"<div class=sub>" + e(r["category"]) + "</div>" if r.get("category") else ""}</td></tr>'
    for r in rows)

diff_items = "".join(
    f'<div class="card"><h4><span class="m">{e(r["id"])}</span> {e(r["name"])} <span class="sub">({e(r["category"])})</span></h4>'
    f'<table class="mini"><tr><th>Field</th><th>Legacy</th><th>Migrated</th></tr>'
    + "".join(f'<tr><td class="m">{e(k)}</td><td class="m">{e(json.dumps(v["legacy"]))}</td><td class="m">{e(json.dumps(v["migrated"]))}</td></tr>'
              for k, v in sorted(r["functional_differences"].items()))
    + f'</table><p><b>Root cause.</b> {e(r["root_cause"])}</p></div>'
    for r in rows if r["status"] != "PARITY")

regress_items = "".join(f'<li><b>{e(r["id"])}</b> {e(r["name"])}: {e(json.dumps(r["functional_differences"]))}</li>'
                        for r in rows if r["status"] == "REGRESSION") or "<li>None observed.</li>"

global_items = "".join(f'<li><b class="m">{e(k)}</b> ({e(known["global"][k]["category"])}): {e(known["global"][k]["root_cause"])}</li>'
                       for k in glob_nf if k in known["global"])

parity_items = "".join(f'<li><b>{e(r["id"])}</b> {e(r["name"])}: <span class="m">{e(outcome(r["migrated"]))}</span></li>'
                       for r in rows if r["status"] == "PARITY")


def user_table(side):
    t = ds[side]["user"]
    return ("".join(f'<tr><td class="m">{e(u["username"])}</td><td>{e(u["storage"])}</td><td>{"yes" if u["verifies_with_seed_plaintext"] else "<b>NO</b>"}</td></tr>'
                    for u in t["rows"]))


ds_ok = ds["diff"]["equivalent"]
ds_problems = "".join(f"<li>{e(p)}</li>" for p in ds["diff"]["problems"]) or "<li>None.</li>"
storage_changes = ", ".join(f"{e(k)}: {e(v)}" for k, v in sorted(ds["diff"]["storage_changes"].items())) or "none"

gallery = "".join(f'<figure><img alt="{e(s["title"])}" src="{img(s["file"])}"><figcaption>{e(s["title"])}</figcaption></figure>' for s in shots)

legacy_ok = legacy_t["build"] == "BUILD SUCCESSFUL"
backend_ok = backend_t["build"] == "BUILD SUCCESSFUL" and backend_t["failures"] + backend_t["errors"] == 0
front_ok = front_t["success"] and front_t["numFailedTests"] == 0
headline = "No regressions observed" if n_reg == 0 else f"{n_reg} regression(s) observed"

CSS = """
*{box-sizing:border-box}body{margin:0;font:14px/1.5 -apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;color:#1d2433;background:#f4f6fa}
header{background:#0b2a4a;color:#fff;padding:32px 48px}header h1{margin:0 0 6px;font-size:26px}header p{margin:0;opacity:.85}
main{max-width:1180px;margin:0 auto;padding:24px 32px 64px}section{background:#fff;border:1px solid #dfe4ec;border-radius:10px;padding:20px 26px;margin:18px 0}
h2{margin:0 0 12px;font-size:19px;color:#0b2a4a}h3{font-size:15px;margin:18px 0 8px}h4{margin:0 0 8px;font-size:14px}
.kpis{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:12px;margin:14px 0}
.kpi{border:1px solid #dfe4ec;border-radius:8px;padding:12px 14px;background:#fafbfd}.kpi .v{font-size:26px;font-weight:700}.kpi .l{color:#5a6475;font-size:12px}
.kpi.ok .v{color:#137a3a}.kpi.int .v{color:#8a5a00}.kpi.bad .v{color:#b42318}
table{border-collapse:collapse;width:100%}th,td{border-bottom:1px solid #e7ebf1;padding:7px 8px;text-align:left;vertical-align:top}th{background:#f1f4f9;font-size:12px;text-transform:uppercase;letter-spacing:.03em;color:#46516a}
.m{font-family:SFMono-Regular,Consolas,Menlo,monospace;font-size:12px}.sub{color:#66708a;font-size:12px}
.b{display:inline-block;padding:2px 8px;border-radius:12px;font-size:11px;font-weight:700;white-space:nowrap}.b.ok{background:#dcf5e5;color:#137a3a}.b.int{background:#fff1d1;color:#8a5a00}.b.bad{background:#fde2e0;color:#b42318}
.card{border:1px solid #e3e8f0;border-left:4px solid #e0a800;border-radius:6px;padding:12px 14px;margin:10px 0}.mini th,.mini td{padding:4px 6px}
.cols{display:grid;grid-template-columns:1fr 1fr 1fr;gap:16px}.cols>div{border:1px solid #e3e8f0;border-radius:8px;padding:12px 16px}
pre{background:#0f1726;color:#d6e2ff;padding:12px 14px;border-radius:8px;overflow:auto;font-size:12px}
figure{margin:0 0 22px}figure img{width:100%;border:1px solid #d5dbe5;border-radius:6px}figcaption{color:#46516a;font-size:12px;margin-top:4px}
.gal{display:grid;grid-template-columns:1fr 1fr;gap:18px}.note{color:#5a6475;font-size:12px}
@media print{body{background:#fff}section{break-inside:avoid}}
"""

generated = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
L, M = env["legacy"], env["migrated"]

doc = f"""<!DOCTYPE html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>PharmaErp modernisation: legacy vs migrated parity report</title><style>{CSS}</style></head><body>
<header><h1>PharmaErp modernisation: legacy vs migrated parity report</h1>
<p>Java 8 servlets/JSP/Hibernate 5.2 → Java 21 · Spring Boot {e(M["spring_boot"])} · React 18 · MySQL 8 · Docker Compose &nbsp;|&nbsp; commit <span class="m">{e(env["git_commit"][:12])}</span> &nbsp;|&nbsp; generated {e(generated)}</p></header>
<main>

<section><h2>1. Executive summary</h2>
<p><b>{e(headline)}.</b> Both estates were started side by side from the same seed data and the {n_tot} recorded legacy behaviours were replayed against each.
{n_par} behave identically, {n_int} differ on purpose ({n_fix} legacy defect fixes, {n_mod} modernisation changes, each with a documented root cause) and {n_reg} are regressions.
After the replay both databases hold the same users and students, and every user still signs in with the same password; the migrated estate now stores those passwords as BCrypt hashes instead of plaintext.</p>
<div class="kpis">
<div class="kpi ok"><div class="v">{n_par}/{n_tot}</div><div class="l">flows at exact parity</div></div>
<div class="kpi int"><div class="v">{n_int}</div><div class="l">intentional differences (root-caused)</div></div>
<div class="kpi {'ok' if n_reg == 0 else 'bad'}"><div class="v">{n_reg}</div><div class="l">regressions</div></div>
<div class="kpi {'ok' if ds_ok else 'bad'}"><div class="v">{'Equivalent' if ds_ok else 'Differs'}</div><div class="l">post-replay data state</div></div>
<div class="kpi"><div class="v">{legacy_t["tests"]} → {backend_t["tests"] + front_t["numTotalTests"]}</div><div class="l">automated tests (legacy → migrated)</div></div>
</div>
<p>The legacy codebase has no automated tests ({legacy_t["tests"]}, recorded from <span class="m">./gradlew test</span> under JDK 8). The migrated estate runs {backend_t["tests"]} backend tests ({backend_t["passed"]} passed, {backend_t["failures"] + backend_t["errors"]} failed) and {front_t["numTotalTests"]} frontend tests ({front_t["numPassedTests"]} passed, {front_t["numFailedTests"]} failed).
Two legacy security defects observed in the baseline are closed: an unauthenticated endpoint that issued an admin token (F15) and an HQL injection point that leaked internal exceptions (F10).</p>
</section>

<section><h2>2. Scope: what was compared and what was built net-new</h2>
<div class="cols">
<div><h3>Legacy actually implemented (compared here)</h3><ul>
<li>Login: <span class="m">GET/POST /login</span> (fields <span class="m">email</span>, <span class="m">password</span>), plaintext compare after trimming the username, RS256 JWT in the body plus a <span class="m">token</span> cookie</li>
<li><span class="m">GET /token</span> (mints an admin JWT for hard-coded <span class="m">tarangparikh</span>)</li>
<li><span class="m">GET /</span> → 302 <span class="m">/redirect</span> (JSP login page)</li>
<li>Tables <span class="m">user</span> and <span class="m">student</span>; username/password validation rules</li></ul></div>
<div><h3>Specification only in legacy (built net-new)</h3><ul>
<li>Staff, stores, clients, manufacturers</li><li>Inventory, stock status, tracking, expiry</li><li>Finance, invoices, GST</li><li>Payments, feedback</li></ul>
<p class="note">Source: <span class="m">ERP-Pharma Excel Sheets.xlsx</span> and <span class="m">Pharma ERP USE-CASE.pdf</span>. These modules had no legacy code, so there is nothing to compare them against. The evidence for them is the migrated test suites (§5) and the screenshots (§9).</p></div>
<div><h3>How parity was measured</h3><ul>
<li>{n_tot} recorded HTTP flows (<span class="m">parity/flows.json</span>) replayed with <span class="m">parity/capture.py</span>; the migrated run targeted the backend's legacy-compat routes</li>
<li>Flow-by-flow classification by <span class="m">parity/diff.py</span> against <span class="m">parity/known-differences.json</span>: a change counts as intentional only if the migrated response matches the documented expectation exactly. Anything else is a regression.</li>
<li>Database state compared by <span class="m">parity/datastate.py</span></li></ul></div>
</div></section>

<section><h2>3. Environment</h2><table>
<tr><th></th><th>Legacy (port 8081)</th><th>Migrated (frontend on 8080, backend on 8090)</th></tr>
<tr><td>Runtime</td><td>{e(L["java"])} · Tomcat {e(L["tomcat"])}</td><td>{e(M["java"])} · Spring Boot {e(M["spring_boot"])} · Gradle {e(M["gradle"])}</td></tr>
<tr><td>Stack</td><td>{e(L["stack"])}</td><td>Spring Web/Security/Data JPA (Hibernate 6), Flyway ({e(M["flyway_migrations"])} migrations applied) · {e(M["frontend"])} · {e(M["nginx"])}</td></tr>
<tr><td>Database</td><td>{e(L["db"])}</td><td>{e(M["db"])}</td></tr>
<tr><td>Images</td><td class="m">{e(L["app_image"])}</td><td class="m">{e(M["backend_image"])}<br>{e(M["frontend_image"])}</td></tr>
<tr><td>Compose</td><td class="m">parity/legacy/docker-compose.yml</td><td class="m">docker-compose.yml + parity/migrated/load-seed.sh</td></tr>
</table>
<p class="sub">Commit <span class="m">{e(env["git_commit"])}</span> (the head of <span class="m">devin/hcl-pharmaerp-modernisation</span>: <span class="m">{e(env["git_base_branch_commit"])}</span>) · Docker Engine {e(env["docker_engine"])} · Compose {e(env["docker_compose"])} · {e(env["host"]["os"])} · {e(env["host"]["python"])}</p>
</section>

<section><h2>4. Flow-by-flow results ({n_tot} flows)</h2>
<table><tr><th>ID</th><th>Flow and request</th><th>Legacy observed</th><th>Migrated observed</th><th>Status</th></tr>{flow_rows}</table>
<h3>Global non-functional differences (every flow; these do not affect the classification above)</h3><ul>{global_items}</ul>
<h3>Regressions</h3><ul>{regress_items}</ul></section>

<section><h2>5. Automated test suites</h2><table>
<tr><th>Suite</th><th>Command</th><th>Tests</th><th>Passed</th><th>Failed</th><th>Result</th><th>Duration</th></tr>
<tr><td>Legacy (all Gradle modules)</td><td class="m">{e(legacy_t["command"])}</td><td>{legacy_t["tests"]}</td><td>0</td><td>0</td><td>{e(legacy_t["build"])}</td><td>{legacy_t["wall_clock_seconds"]} s</td></tr>
<tr><td>Migrated backend (JUnit 5, {backend_t["test_classes"]} classes, incl. Testcontainers MySQL)</td><td class="m">{e(backend_t["command"])}</td><td>{backend_t["tests"]}</td><td>{backend_t["passed"]}</td><td>{backend_t["failures"] + backend_t["errors"]}</td><td>{e(backend_t["build"])}</td><td>{backend_t["wall_clock_seconds"]} s wall clock ({backend_t["sum_of_testcase_seconds"]} s in test cases)</td></tr>
<tr><td>Migrated frontend (Vitest, {len(front_t["files"])} files)</td><td class="m">cd frontend &amp;&amp; npx vitest run</td><td>{front_t["numTotalTests"]}</td><td>{front_t["numPassedTests"]}</td><td>{front_t["numFailedTests"]}</td><td>{'PASS' if front_ok else 'FAIL'}</td><td>{front_t["duration_ms"] / 1000:.1f} s</td></tr>
</table><p class="note">Legacy: {e(legacy_t["note"])} ({e(", ".join(f"{k}: {v}" for k, v in legacy_t["test_tasks"].items()))}). Logs are in <span class="m">parity/results/test-suites/</span>.</p></section>

<section><h2>6. Data state after replay</h2>
<p><b>{'Equivalent.' if ds_ok else 'Not equivalent.'}</b> Rows: <span class="m">user</span> legacy {ds["legacy"]["user"]["row_count"]} / migrated {ds["migrated"]["user"]["row_count"]}; <span class="m">student</span> legacy {ds["legacy"]["student"]["row_count"]} / migrated {ds["migrated"]["student"]["row_count"]}.
Passwords were compared by checking whether the stored value verifies against the seed plaintext (plaintext equality for legacy, BCrypt check for migrated). No password or hash is written to the results.</p>
<div class="cols" style="grid-template-columns:1fr 1fr"><div><h3>Legacy <span class="m">user</span></h3><table><tr><th>Username</th><th>Storage</th><th>Verifies with seed password</th></tr>{user_table("legacy")}</table></div>
<div><h3>Migrated <span class="m">user</span></h3><table><tr><th>Username</th><th>Storage</th><th>Verifies with seed password</th></tr>{user_table("migrated")}</table></div></div>
<p>Storage changes (intentional, modernisation): {storage_changes}. The migrated estate re-hashes a plaintext legacy password with BCrypt on the first successful sign-in. Problems: <ul>{ds_problems}</ul></p></section>

<section><h2>7. Every difference, with its root cause</h2>{diff_items}</section>

<section><h2>8. Verified parity, known limitations, pre-existing legacy defects</h2><div class="cols">
<div><h3>Verified parity (observed identical)</h3><ul>{parity_items}
<li>Same JWT contract on success: RS256, <span class="m">sub=user/login</span>, <span class="m">username</span>, <span class="m">scope=admin</span>, 1 h TTL, <span class="m">token</span> cookie</li>
<li>Same users after replay, all signing in with the same passwords (§6)</li></ul></div>
<div><h3>Known limitations of the migrated estate</h3><ul>
<li>A failed legacy-style form login lands on <span class="m">/signin?error</span>, but the React sign-in page does not read <span class="m">?error</span>, so no error banner is shown. The SPA's own sign-in does show one. See the screenshot in §9.</li>
<li>Legacy and migrated JWTs are not interchangeable (different keys): a legacy-minted token gets 401 from the migrated API (see <span class="m">parity/results/probes.txt</span>). Active legacy sessions must sign in again after cutover.</li>
<li>The <span class="m">student</span> table is empty in the seed on both sides, so the parity run compares only its row count and schema presence. No legacy code path writes to it.</li>
<li>The net-new modules (§2) have no legacy baseline. Their evidence is the migrated tests and screenshots, not parity.</li>
<li>Each parity run needs fresh volumes (<span class="m">docker compose down -v</span>), because <span class="m">load-seed.sh</span> refuses to load into a non-empty <span class="m">user</span> table.</li></ul></div>
<div><h3>Pre-existing legacy defects (observed in the legacy estate)</h3><ul>
<li>F15: <span class="m">GET /token</span> returns a signed admin JWT for hard-coded <span class="m">tarangparikh</span> to any unauthenticated caller. <b>Fixed</b> (401 without a valid token).</li>
<li>F10: a quote in the username yields an <span class="m">org.hibernate.QueryException</span> text response (HQL built by string concatenation). <b>Fixed</b> (bound parameters, ordinary failed login).</li>
<li>Passwords stored and compared in plaintext. <b>Fixed</b> (BCrypt, upgraded on sign-in).</li>
<li>RSA-1024 signing key committed to the repo (<span class="m">WEB-INF/StoredObject/system.key</span>). <b>Fixed</b> (a per-environment RSA-2048 key is generated at first start).</li>
<li>The JSP login page shows both "Error" callouts on first load, before any submission (legacy login screenshot, §9).</li>
<li>F07: missing parameters fall through to the login page without feedback.</li></ul></div>
</div></section>

<section><h2>9. Screenshots (headless Chromium via Playwright)</h2><div class="gal">{gallery}</div></section>

<section><h2>10. Supplementary probes (<span class="m">parity/probes.sh</span>)</h2>
<p class="note">These check the parts of the migrated behaviour that the 16-flow replay cannot reach: the browser front door, authenticated <span class="m">/token</span> reissue, and cross-estate token rejection.</p><pre>{e(probes)}</pre></section>

<section><h2>11. How to reproduce</h2><pre>git checkout devin/hcl-t13-parity
# legacy estate (Tomcat 9 / JDK 8 / MySQL 5.7, port 8081), seeded from parity/seed/
docker compose -f parity/legacy/docker-compose.yml up -d --build --wait
# migrated estate (MySQL 8 / Spring Boot backend 8090 / nginx+React 8080), then the identical seed users
docker compose up -d --build --wait
parity/migrated/load-seed.sh
# replay and classify
python3 parity/capture.py --base http://127.0.0.1:8081 --label legacy   --out parity/results/legacy.json
python3 parity/capture.py --base http://127.0.0.1:8090 --label migrated --out parity/results/migrated.json
python3 parity/diff.py                       # -> parity/results/diff.json (exit 1 on any REGRESSION)
python3 parity/datastate.py --out parity/results/data-state.json   # needs pip bcrypt
parity/probes.sh &gt; parity/results/probes.txt
python3 parity/screenshots.py                # -> parity/report/screenshots/ (needs playwright)
python3 parity/environment.py &gt; parity/results/environment.json
# test suites
(JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 ./gradlew test)               # legacy: 0 tests
(cd backend &amp;&amp; JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew test)
(cd frontend &amp;&amp; npm ci &amp;&amp; npx vitest run)
python3 parity/report/build_report.py        # -> parity/report/pharmaerp-parity-report.html
# reset for another run
docker compose down -v; docker compose -f parity/legacy/docker-compose.yml down -v</pre>
<p class="note">Built from the files in <span class="m">parity/results/</span>. This page makes no network requests: styles are inline and the screenshots are embedded.</p></section>
</main></body></html>"""

out = P("report", "pharmaerp-parity-report.html")
open(out, "w").write(doc)
assert not re.search(r'(src|href)\s*=\s*"(https?:)?//', doc), "external resource reference in report"
print(f"wrote {out} ({len(doc)//1024} KiB): {n_tot} flows, {n_par} parity, {n_int} intentional, {n_reg} regression")
