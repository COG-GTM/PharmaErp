#!/usr/bin/env python3
"""Compare a legacy and a migrated transcript (from parity/capture.py) flow by flow.

Usage: python3 parity/diff.py [--legacy parity/results/legacy.json] [--migrated parity/results/migrated.json]
                              [--known parity/known-differences.json] [--out parity/results/diff.json]
Each flow is classified as
  PARITY                 functional fields identical,
  INTENTIONAL DIFFERENCE functional fields differ and the migrated response matches the documented expectation
                         in known-differences.json (defect fix or modernisation, with root cause),
  REGRESSION             anything else.
Functional fields: status, Location, content type, body kind, JSON body, JWT alg/claims/TTL (body and token cookie)
and whether a token cookie is set. Non-functional differences (JWT signature length, JSESSIONID) are reported
separately as global differences and must be documented in known-differences.json too, otherwise they count as
regressions of every affected flow. Exit code 1 when any flow is a REGRESSION.
"""
import argparse, json, sys

GLOBAL_KEYS = ("jwt.signature_len", "cookie.JSESSIONID")


def jwt_functional(j):
    return None if j is None else {k: j.get(k) for k in ("valid_shape", "alg", "claims", "ttl_seconds_approx")}


def functional(flow):
    body = flow["body"]
    token = flow["cookies"].get("token")
    return {
        "status": flow["status"],
        "location": flow["location"],
        "content_type": flow["content_type"] or None,
        "body_kind": body["kind"],
        "body_value": body.get("value"),
        "body_jwt": jwt_functional(body.get("jwt")),
        "token_cookie": None if token is None else jwt_functional(token.get("jwt")),
    }


def non_functional(flow):
    sigs = {flow["body"].get("jwt", {}).get("signature_len"),
            (flow["cookies"].get("token") or {}).get("jwt", {}).get("signature_len")} - {None}
    return {"jwt.signature_len": sorted(sigs), "cookie.JSESSIONID": "JSESSIONID" in flow["cookies"]}


def classify(fid, legacy, migrated, known):
    lf, mf = functional(legacy), functional(migrated)
    changed = {k: {"legacy": lf[k], "migrated": mf[k]} for k in lf if lf[k] != mf[k]}
    ln, mn = non_functional(legacy), non_functional(migrated)
    nf_changed = {k: {"legacy": ln[k], "migrated": mn[k]} for k in ln if ln[k] != mn[k]}
    undocumented_nf = [k for k in nf_changed if k not in known.get("global", {})]
    entry = {"id": fid, "name": legacy["name"], "legacy": lf, "migrated": mf,
             "functional_differences": changed, "non_functional_differences": nf_changed}
    if not changed:
        entry["status"] = "PARITY" if not undocumented_nf else "REGRESSION"
        if undocumented_nf:
            entry["reason"] = f"undocumented non-functional difference(s): {undocumented_nf}"
        return entry
    spec = known.get("flows", {}).get(fid)
    if spec is None:
        entry.update(status="REGRESSION", reason="behaviour changed and no documented intentional difference")
        return entry
    mismatched = {k: {"expected": v, "actual": mf.get(k)} for k, v in spec["expect_migrated"].items() if mf.get(k) != v}
    if mismatched or undocumented_nf:
        entry.update(status="REGRESSION", reason=f"documented expectation not met: {mismatched or undocumented_nf}")
        return entry
    entry.update(status="INTENTIONAL DIFFERENCE", category=spec["category"], root_cause=spec["root_cause"])
    return entry


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--legacy", default="parity/results/legacy.json")
    ap.add_argument("--migrated", default="parity/results/migrated.json")
    ap.add_argument("--known", default="parity/known-differences.json")
    ap.add_argument("--out", default="parity/results/diff.json")
    a = ap.parse_args()
    legacy = {f["id"]: f for f in json.load(open(a.legacy))["flows"]}
    migrated = {f["id"]: f for f in json.load(open(a.migrated))["flows"]}
    known = json.load(open(a.known))
    flows = []
    for fid in legacy:
        if fid not in migrated:
            flows.append({"id": fid, "name": legacy[fid]["name"], "status": "REGRESSION", "reason": "missing from migrated run"})
            continue
        flows.append(classify(fid, legacy[fid], migrated[fid], known))
    globals_seen = {}
    for f in flows:
        for k in f.get("non_functional_differences", {}):
            globals_seen.setdefault(k, []).append(f["id"])
    summary = {s: sum(1 for f in flows if f["status"] == s) for s in ("PARITY", "INTENTIONAL DIFFERENCE", "REGRESSION")}
    summary["total"] = len(flows)
    result = {"summary": summary, "flows": flows,
              "global_differences": {k: {"flows": v, **known.get("global", {}).get(k, {"root_cause": "UNDOCUMENTED"})}
                                     for k, v in globals_seen.items()}}
    json.dump(result, open(a.out, "w"), indent=2, sort_keys=True)
    for f in flows:
        print(f"{f['id']:4} {f['status']:23} {f['name']}")
    for k, v in result["global_differences"].items():
        print(f"global {k}: flows {', '.join(v['flows'])}")
    print(json.dumps(summary))
    return 1 if summary["REGRESSION"] else 0


if __name__ == "__main__":
    sys.exit(main())
