#!/usr/bin/env python3
"""Verify native CMS media over HTTP in the shell's owned disposable runtime."""
import hashlib
from html.parser import HTMLParser
import http.cookiejar
import json
from pathlib import Path
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
SOURCE_PATHS = (
    "backend/src/cljs/knoxx/backend/law/publication_media.cljc",
    "backend/src/cljs/knoxx/backend/domain/publication_content.cljc",
    "backend/src/cljs/knoxx/backend/infra/publication_runtime.cljs",
    "backend/test/cljs/knoxx/backend/infra/publication_runtime_test.cljs",
    "backend/test/cljs/knoxx/backend/extern/approved_media_verifier.cljs",
)
SONG = "https://suno.com/song/a7d0fb41-785e-4919-84d8-0c076814192b"
ART = "/graphics/Sutured_Signal.svg"
CONTENT = (
    "Selected artwork and music from CMS-owned source text.\n\n"
    "![Sutured Signal](/graphics/Sutured_Signal.svg)\n\n"
    f"[We Are The Place]({SONG})\n\n"
    "Ordinary & <unsafe> prose stays escaped.\n\n"
    "![Private](/api/workspace-media/raw?path=private.svg)\n\n"
    "[Unsafe](javascript:alert(1))\n\n"
    "<script>alert(1)</script>\n\n"
    "```markdown\n\n![Code example](/graphics/Sutured_Signal.svg)\n\n```\n"
)


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def source_proof(compiled=None):
    """Bind the physical checkout, current source bytes and compiled fixture."""
    checkout = ROOT.resolve()
    git_root = subprocess.check_output(
        ["git", "-C", str(checkout), "rev-parse", "--show-toplevel"], text=True
    ).strip()
    if Path(git_root).resolve() != checkout:
        raise ValueError("Git root does not match verifier source checkout")
    head = subprocess.check_output(
        ["git", "-C", str(checkout), "rev-parse", "--verify", "HEAD^{commit}"], text=True
    ).strip()
    if len(head) != 40 or any(character not in "0123456789abcdef" for character in head):
        raise ValueError("Verifier needs a full Git commit identity")
    files = {name: sha256((checkout / name).read_bytes()) for name in SOURCE_PATHS}
    proof = {"checkout": str(checkout), "head": head, "files": files}
    if compiled is not None:
        proof["compiled-sha256"] = sha256(Path(compiled).read_bytes())
    return proof


class MediaMarkup(HTMLParser):
    """Inspect semantic tags without treating strings inside prose as markup."""
    def __init__(self):
        super().__init__()
        self.images = []
        self.links = []
        self.active = []

    def handle_starttag(self, tag, attrs):
        attributes = dict(attrs)
        if tag == "img":
            self.images.append(attributes)
        if tag == "a":
            self.links.append(attributes)
        if tag in {"iframe", "script", "audio", "video"} or "autoplay" in attributes:
            self.active.append(tag)


class Client:
    """A proxy-free HTTP client restricted to its ephemeral loopback origin."""
    def __init__(self, origin, cookies=False):
        parsed = urllib.parse.urlsplit(origin)
        if (parsed.scheme != "http" or parsed.hostname != "127.0.0.1"
                or parsed.username or parsed.password or parsed.path or not parsed.port):
            raise ValueError("Fixture origin must be exact ephemeral loopback HTTP")
        self.origin = origin
        handlers = [urllib.request.ProxyHandler({})]
        if cookies:
            handlers.append(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.opener = urllib.request.build_opener(*handlers)

    def request(self, method, path, payload=None):
        if not path.startswith("/") or path.startswith("//"):
            raise ValueError("Expected an origin-relative fixture route")
        body = None if payload is None else json.dumps(payload).encode()
        request = urllib.request.Request(self.origin + path, data=body, method=method,
                                         headers={"Content-Type": "application/json"})
        try:
            response = self.opener.open(request, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            return response.status, response.read(), dict(response.headers)

    def json(self, method, path, payload=None, expected=200):
        status, body, _ = self.request(method, path, payload)
        if status != expected:
            raise AssertionError(f"{method} {path}: expected {expected}, got {status}: {body[:300]!r}")
        return json.loads(body)


def check(condition, message):
    if not condition:
        raise AssertionError(message)
    print("PASS " + message, flush=True)


def verify(run_root, proof_file):
    """Seed through real routes; inspect public bytes and fail-closed controls."""
    run_root = Path(run_root)
    receipt = json.loads((run_root / "receipt.json").read_text())
    proof = json.loads(Path(proof_file).read_text())
    anonymous = Client(receipt["origin"])
    cms = Client(receipt["origin"], cookies=True)
    public = Client(receipt["public-origin"])
    start_path = urllib.parse.urlsplit(receipt["start-url"]).path
    cms.json("GET", start_path)
    check(cms.json("GET", "/_verify/proof") == proof == receipt["proof"],
          "live process serves this physical checkout, frozen source hashes and compiled fixture digest")
    check(not cms.json("GET", "/api/cms/documents")["documents"],
          "dedicated CMS starts empty before any verifier writes")
    for method, route, payload in (
        ("GET", "/_verify/proof", None),
        ("GET", "/api/auth/context", None),
        ("GET", "/api/cms/documents", None),
        ("POST", "/api/cms/documents", {"title": "Anonymous", "content": CONTENT, "parents": []}),
        ("GET", "/api/cms/publications/documents", None),
        ("PATCH", "/api/cms/publications/intents/unknown", {"state": "published"}),
        ("POST", "/api/publications/reconcile", {"publicationId": "unknown"}),
        ("GET", "/api/publications/receipts", None),
    ):
        status, _, _ = anonymous.request(method, route, payload)
        check(status in {401, 403}, f"anonymous {method} {route} is denied before effects")
    check(public.request("GET", "/api/cms/documents")[0] == 404,
          "public reader exposes manifest publications without exposing private CMS routes")
    value = {"title": "Approved CMS art and music verification", "content": CONTENT,
             "visibility": "review", "parents": [], "source_path": "verification/selected-media.md",
             "metadata": {"blocks": [{"type": "image", "src": "/graphics/unapproved.svg"}]}}
    document = cms.json("POST", "/api/cms/documents", value)
    document_path = "/api/cms/documents/" + urllib.parse.quote(document["doc_id"], safe="")
    check(cms.json("GET", document_path)["content"] == CONTENT,
          "native CMS retained exact selected source bytes including final newline")
    anonymous.json("GET", document_path, expected=403)
    anonymous.json("PATCH", document_path, value, expected=403)
    anonymous.json("GET", document_path + "/history", expected=403)
    topology = cms.json("GET", "/api/cms/publications/documents")
    check(len(topology["documents"]) == 1, "native save produced one dedicated publication document")
    publications = {row["locale"]: row for row in topology["documents"][0]["publications"]}
    check(set(publications) == {"en", "es"} and all(row["desired"] == "withheld" for row in publications.values()),
          "native save created withheld English and Spanish intents; save alone published nothing")
    check(public.request("GET", publications["en"]["path"])[0] == 404,
          "public reader cannot serve withheld source content")
    for row in publications.values():
        intent = "/api/cms/publications/intents/" + urllib.parse.quote(row["id"], safe="")
        cms.json("PATCH", intent, {"state": "published"})
    english = cms.json("POST", "/api/publications/reconcile", {"publicationId": publications["en"]["id"]})
    check(english["type"] == "publication/materialized",
          "native reconciliation materialized source-locale text under its declared review policy")
    status, rendered, headers = public.request("GET", publications["en"]["path"])
    check(status == 200 and headers.get("content-type", "").startswith("text/html"),
          "anonymous public readback serves the actual manifest artifact as HTML")
    markup = MediaMarkup()
    markup.feed(rendered.decode())
    check(len(markup.images) == 1 and markup.images[0].get("src") == ART
          and markup.images[0].get("alt") == "Sutured Signal", "approved source text displays its selected artwork")
    check([link.get("href") for link in markup.links] == [SONG] and not markup.active,
          "selected music is a canonical listen destination without automatic remote players or media")
    check(b"unapproved.svg" not in rendered and b"Ordinary &amp; &lt;unsafe&gt;" in rendered
          and b"&lt;script&gt;" in rendered and b"![Code example]" in rendered,
          "unsigned metadata, unsafe URLs, HTML and fenced examples never become active media")
    _, asset, _ = public.request("GET", ART)
    check(sha256(asset) == receipt["asset-sha256"], "displayed artwork readback matches the supplied real asset digest")
    manifest_status, manifest, _ = public.request("GET", "/published/manifest.edn")
    check(manifest_status == 200, "publication committed its inspectable native EDN manifest")
    spanish = cms.json("POST", "/api/publications/reconcile", {"publicationId": publications["es"]["id"]})
    check(spanish["type"] == "publication/blocked" and "translation-review-required" in spanish["blockers"],
          "Spanish cannot reuse English media without translation and exact review evidence")
    check(public.request("GET", publications["es"]["path"])[0] == 404
          and public.request("GET", "/published/manifest.edn")[1] == manifest,
          "blocked Spanish reconciliation leaves the English committed publication untouched")
    value.update(parents=[document["revision"]], metadata={"blocks": [{"type": "image", "src": "/graphics/tampered.svg"},
                                                                     {"type": "track", "src": "javascript:alert(1)"}]})
    edited = cms.json("PATCH", document_path, value)
    check(edited["revision"] != document["revision"] and edited["content"] == CONTENT,
          "metadata tamper creates a retained CMS revision without changing authenticated source text")
    repeated = cms.json("POST", "/api/publications/reconcile", {"publicationId": publications["en"]["id"]})
    check(repeated["type"] == "publication/noop" and public.request("GET", publications["en"]["path"])[1] == rendered,
          "metadata-only revision cannot change the publication bytes or create an unapproved image")
    check(len(cms.json("GET", document_path + "/history")["revisions"]) == 2,
          "native history retains the original and metadata revision without rewriting either")
    result = {"publication-path": publications["en"]["path"], "public-url": receipt["public-origin"] + publications["en"]["path"],
              "public-html-sha256": sha256(rendered), "manifest-sha256": sha256(manifest),
              "asset-sha256": sha256(asset), "song-destination": SONG, "actual-local-tracks": 0,
              "proof": proof}
    (run_root / "verification.json").write_text(json.dumps(result, indent=2) + "\n")
    print("WARN fixture identity and process-local empty evidence stores do not verify password login, Mongo persistence or provider output.")
    print("WARN music listening destination is selected public metadata; this run does not prove current remote playback or ingest audio bytes.")
    print("WARN supplied artwork is separately staged; CMS owns the source reference and publication, not transferred media byte custody.")
    print("WARN translated media authenticity is separately covered by publication-runtime-test; a real Spanish provider/review journey remains required.")
    print("WARN optional prebuilt frontend is a read-only tour input; its source/image qualification is separate from this backend fixture proof.")
    print("PASS native CMS approved-media verification completed; shell teardown removes the whole owned runtime.")


if __name__ == "__main__":
    try:
        if len(sys.argv) == 2 and sys.argv[1] == "--source-proof":
            print(json.dumps(source_proof(), indent=2))
        elif len(sys.argv) == 3 and sys.argv[1] == "--proof":
            print(json.dumps(source_proof(sys.argv[2]), indent=2))
        elif len(sys.argv) == 3:
            verify(sys.argv[1], sys.argv[2])
        else:
            raise ValueError("Usage: verify-approved-cms-media.py --proof compiled | run-root proof.json")
    except (AssertionError, OSError, ValueError, subprocess.SubprocessError) as error:
        print("FAIL " + str(error), file=sys.stderr)
        sys.exit(1)
