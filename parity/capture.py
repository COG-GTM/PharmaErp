#!/usr/bin/env python3
"""Replay every recorded user/API flow against a PharmaErp estate and write a normalised transcript.

Usage: python3 parity/capture.py --base http://127.0.0.1:8081 --label legacy --out parity/baseline/legacy-transcripts.json
Only the Python standard library is used. Responses are normalised so legacy and migrated runs can be diffed:
volatile values (dates, session ids, JWT exp/signature) are reduced to stable facts.
"""
import argparse, base64, http.client, json, re, sys, time, urllib.parse

FLOWS = json.load(open(__file__.replace("capture.py", "flows.json")))


def b64json(part):
    part += "=" * (-len(part) % 4)
    return json.loads(base64.urlsafe_b64decode(part))


def jwt_facts(token):
    try:
        h, p, s = token.split(".")
        header, payload = b64json(h), b64json(p)
    except Exception:
        return {"valid_shape": False}
    exp = payload.pop("exp", None)
    payload.pop("iat", None)
    return {"valid_shape": True, "alg": header.get("alg"), "claims": payload,
            "ttl_seconds_approx": None if exp is None else round((exp - time.time()) / 60) * 60,
            "signature_len": len(s)}


def classify_body(ctype, body):
    text = body.decode("utf-8", "replace")
    if "json" in (ctype or ""):
        try:
            val = json.loads(text)
        except ValueError:
            return {"kind": "invalid-json", "text": text[:200]}
        if isinstance(val, str) and val.count(".") == 2:
            return {"kind": "json-jwt-string", "jwt": jwt_facts(val)}
        return {"kind": "json", "value": val}
    if "<form" in text and "password" in text:
        return {"kind": "html-login-page", "has_login_form": True}
    if re.search(r"(Exception|QueryException|at [a-z]+\.)", text):
        return {"kind": "error-text-leak", "text": text.strip()[:200]}
    if not text.strip():
        return {"kind": "empty"}
    return {"kind": "other", "text": text.strip()[:200]}


def run_flow(base, flow):
    u = urllib.parse.urlparse(base)
    conn = http.client.HTTPConnection(u.hostname, u.port or 80, timeout=20)
    path = flow["path"]
    headers = dict(flow.get("headers", {}))
    body = None
    if "form" in flow:
        body = urllib.parse.urlencode(flow["form"])
        headers["Content-Type"] = "application/x-www-form-urlencoded"
        if flow["method"] == "GET":
            path, body = path + "?" + body, None
    conn.request(flow["method"], path, body=body, headers=headers)
    r = conn.getresponse()
    raw = r.read()
    cookies = {}
    for k, v in r.getheaders():
        if k.lower() == "set-cookie":
            name, _, rest = v.partition("=")
            val = rest.split(";")[0]
            cookies[name] = {"jwt": jwt_facts(val)} if name == "token" else "present"
    ctype = r.getheader("Content-Type", "")
    return {"id": flow["id"], "name": flow["name"], "request": {k: flow[k] for k in ("method", "path", "form") if k in flow},
            "status": r.status, "location": r.getheader("Location"), "content_type": ctype.split(";")[0],
            "cookies": cookies, "body": classify_body(ctype, raw)}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", required=True)
    ap.add_argument("--label", required=True)
    ap.add_argument("--out", required=True)
    a = ap.parse_args()
    results = [run_flow(a.base, f) for f in FLOWS]
    json.dump({"label": a.label, "base": a.base, "flows": results}, open(a.out, "w"), indent=2, sort_keys=True)
    for r in results:
        print(f"{r['id']:4} {r['status']} {r['body']['kind']:18} {r['name']}")


if __name__ == "__main__":
    sys.exit(main())
