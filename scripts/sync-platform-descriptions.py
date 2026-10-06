"""Adapt the README to platform Markdown and update the Modrinth project body."""
import hashlib
import json
import os
from pathlib import Path
import re
import urllib.parse
import urllib.request


def description(readme: str, feature_url: str) -> str:
    if not feature_url.startswith("https://cdn.modrinth.com/"):
        raise ValueError("Expected a public Modrinth CDN image URL")
    feature_pattern = r"(!\[Quest letters, package rewards, magic circles and player signatures\])\([^)]+\)"
    body, replacements = re.subn(feature_pattern, lambda match: f"{match.group(1)}({feature_url})", readme)
    if replacements != 1:
        raise ValueError("Expected one feature overview image in README")
    body = re.sub(r"^\[Download v[^\n]+\n", "", body, flags=re.M)
    body = re.sub(r"^\[Full setup & JSON examples\][^\n]+\n", "", body, flags=re.M)
    body = body.replace("Licensed under [GPL-3.0](LICENSE).", "Licensed under **GPL-3.0**.")
    links = re.findall(r"!?\[[^\]]*\]\(([^)]+)\)", body)
    if any(not link.startswith("https://") for link in links):
        raise ValueError("Platform description contains a relative or non-HTTPS link")
    if "FEATURE_IMAGE_URL" in body:
        raise ValueError("Unresolved feature image URL")
    return re.sub(r"\n{3,}", "\n\n", body).strip() + "\n"


def main():
    project_id = os.environ["MODRINTH_PROJECT_ID"]
    token = os.environ["MODRINTH_TOKEN"]
    output = Path("platform-descriptions")
    output.mkdir(exist_ok=True)

    def api(path, method="GET", data=None, content_type=None):
        headers = {"Authorization": token, "User-Agent": "exco9/QuestLog-X-Envelope-Addon/description-sync"}
        if content_type:
            headers["Content-Type"] = content_type
        request = urllib.request.Request("https://api.modrinth.com/v2/" + path,
                                         headers=headers, data=data, method=method)
        with urllib.request.urlopen(request, timeout=60) as response:
            payload = response.read()
        return json.loads(payload) if payload else None

    project = api(f"project/{project_id}")
    if project["id"] != project_id or project["slug"] != "qlxe":
        raise ValueError("Project does not match QLXE")
    (output / "modrinth-before.md").write_text(project["body"], encoding="utf-8")
    image = Path(".github/images/features.png").read_bytes()
    image_hash = hashlib.sha256(image).digest()

    def matching_image(gallery):
        for entry in gallery:
            url = entry["url"]
            if not url.startswith("https://cdn.modrinth.com/"):
                continue
            with urllib.request.urlopen(url, timeout=30) as response:
                if hashlib.sha256(response.read()).digest() == image_hash:
                    return url
        return None

    feature_url = matching_image(project.get("gallery", []))
    if not feature_url:
        query = urllib.parse.urlencode({"ext": "png", "featured": "false", "title": "Features",
                                        "description": "Quest letters, package rewards, magic circles and player signatures"})
        api(f"project/{project_id}/gallery?{query}", "POST", image, "image/png")
        project = api(f"project/{project_id}")
        feature_url = matching_image(project.get("gallery", []))
    if not feature_url:
        raise ValueError("Could not verify uploaded feature image")

    body = description(Path("README.md").read_text(encoding="utf-8"), feature_url)
    for platform in ("modrinth", "curseforge"):
        (output / f"{platform}.md").write_text(body, encoding="utf-8")
    (output / "feature-url.txt").write_text(feature_url + "\n", encoding="utf-8")
    api(f"project/{project_id}", "PATCH", json.dumps({"body": body}).encode(), "application/json")
    updated = api(f"project/{project_id}")
    if updated["body"] != body:
        raise ValueError("Modrinth description verification failed")
    print("Modrinth description updated and verified. CurseForge Markdown is ready.")
    print(f"Public feature image: {feature_url}")


if __name__ == "__main__":
    main()
