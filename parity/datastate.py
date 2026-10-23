#!/usr/bin/env python3
"""Dump and diff the legacy and migrated `user` / `student` tables after a parity replay.

Usage (repo root, both estates up): python3 parity/datastate.py --out parity/results/data-state.json
Passwords are never written out. Each stored value is checked against the plaintext in parity/seed/seed-users.sql:
legacy keeps plaintext, the migrated estate may have upgraded the row to BCrypt on a successful login, so the
comparison is "verifies with the same seed plaintext", plus the storage format. BCrypt checks need `pip install bcrypt`.
"""
import argparse, json, os, re, subprocess, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LEGACY = ["docker", "compose", "-f", "parity/legacy/docker-compose.yml", "exec", "-T", "-e", "MYSQL_PWD=admin",
          "legacy-db", "mysql", "-uroot", "PHARMA_ERP"]
MIGRATED_PWD = os.environ.get("PHARMA_DB_PASSWORD", "pharma")
MIGRATED = ["docker", "compose", "exec", "-T", "-e", "MYSQL_PWD", "mysql", "mysql",
            "-u" + os.environ.get("PHARMA_DB_USER", "pharma"), os.environ.get("PHARMA_DB_NAME", "PHARMA_ERP")]
BCRYPT = re.compile(r"^(\{bcrypt\})?\$2[aby]?\$\d\d\$[./0-9A-Za-z]{53}$")


def query(cmd, sql):
    env = dict(os.environ, MYSQL_PWD=MIGRATED_PWD)
    out = subprocess.run(cmd + ["-N", "-B", "-e", sql], cwd=ROOT, env=env, check=True,
                         capture_output=True, text=True).stdout
    return [line.split("\t") for line in out.splitlines() if line]


def seed_passwords():
    sql = open(os.path.join(ROOT, "parity/seed/seed-users.sql")).read()
    return dict(re.findall(r"\('([^']+)',\s*'([^']*)'\)", sql))


def verifies(stored, plaintext):
    if plaintext is None:
        return False
    if BCRYPT.match(stored):
        import bcrypt
        h = stored[len("{bcrypt}"):] if stored.startswith("{bcrypt}") else stored
        return bcrypt.checkpw(plaintext.encode(), h.encode())
    return stored == plaintext


def snapshot(cmd):
    seed = seed_passwords()
    users = []
    for username, stored in query(cmd, "SELECT username, password FROM `user` ORDER BY username"):
        users.append({"username": username, "storage": "bcrypt" if BCRYPT.match(stored) else "plaintext",
                      "verifies_with_seed_plaintext": verifies(stored, seed.get(username))})
    students = query(cmd, "SELECT id, first_name, last_name, email FROM student ORDER BY id")
    return {"user": {"row_count": len(users), "rows": users},
            "student": {"row_count": len(students), "rows": students}}


def diff(legacy, migrated):
    lu = {r["username"]: r for r in legacy["user"]["rows"]}
    mu = {r["username"]: r for r in migrated["user"]["rows"]}
    problems = []
    if set(lu) != set(mu):
        problems.append(f"usernames differ: legacy-only={sorted(set(lu) - set(mu))} migrated-only={sorted(set(mu) - set(lu))}")
    for name in sorted(set(lu) & set(mu)):
        if not lu[name]["verifies_with_seed_plaintext"] or not mu[name]["verifies_with_seed_plaintext"]:
            problems.append(f"{name}: seed password does not verify in both estates")
    if legacy["student"] != migrated["student"]:
        problems.append("student rows differ")
    storage = {n: f'{lu[n]["storage"]} -> {mu[n]["storage"]}' for n in sorted(set(lu) & set(mu))
               if lu[n]["storage"] != mu[n]["storage"]}
    return {"equivalent": not problems, "problems": problems, "storage_changes": storage}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    a = ap.parse_args()
    legacy, migrated = snapshot(LEGACY), snapshot(MIGRATED)
    result = {"legacy": legacy, "migrated": migrated, "diff": diff(legacy, migrated)}
    json.dump(result, open(a.out, "w"), indent=2, sort_keys=True)
    d = result["diff"]
    print(f"user rows legacy={legacy['user']['row_count']} migrated={migrated['user']['row_count']}; "
          f"student rows legacy={legacy['student']['row_count']} migrated={migrated['student']['row_count']}")
    print("storage changes:", d["storage_changes"] or "none")
    print("EQUIVALENT" if d["equivalent"] else "DIFFERENT: " + "; ".join(d["problems"]))
    return 0 if d["equivalent"] else 1


if __name__ == "__main__":
    sys.exit(main())
