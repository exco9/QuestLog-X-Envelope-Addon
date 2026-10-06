"""Validate a released loader JAR and extract only its version's changelog."""
import json
import os
from pathlib import Path
import re
import sys
import tomllib
import zipfile


def prepare(source: Path, artifacts: Path, tag: str, loader: str):
    if not re.fullmatch(r"v\d+\.\d+\.\d+", tag):
        raise ValueError("Expected a release tag such as v0.1.2")
    if loader not in {"fabric", "neoforge"}:
        raise ValueError("Unsupported loader")
    properties = dict(re.findall(r"^([a-z_]+)=(.+)$", (source / "gradle.properties").read_text(), re.M))
    version = properties["mod_version"].strip()
    minecraft = properties["minecraft_version"].strip()
    if tag != f"v{version}":
        raise ValueError("Tag does not match the released source version")
    matches = list(artifacts.glob("*.jar"))
    expected = f"questlog-envelope-addon-{loader}-{minecraft}-{version}.jar"
    if len(matches) != 1 or matches[0].name != expected:
        raise ValueError(f"Expected exactly one released artifact: {expected}")
    artifact = matches[0]
    with zipfile.ZipFile(artifact) as jar:
        if loader == "fabric":
            metadata = json.loads(jar.read("fabric.mod.json"))
            mod_id, jar_version = metadata["id"], metadata["version"]
        else:
            metadata = tomllib.loads(jar.read("META-INF/neoforge.mods.toml").decode())
            mod_id, jar_version = metadata["mods"][0]["modId"], metadata["mods"][0]["version"]
        if mod_id != "questlog_envelope" or jar_version != version:
            raise ValueError("Artifact metadata does not match the released source")
        json.loads(jar.read("questlog_envelope.mixins.json"))
    changelog = (source / "CHANGELOG.md").read_text(encoding="utf-8")
    section = re.search(rf"^## \[{re.escape(version)}\][^\n]*\n(.*?)(?=^## \[|\Z)", changelog, re.M | re.S)
    if not section or not section.group(1).strip():
        raise ValueError("Missing or empty changelog section")
    return artifact, version, minecraft, section.group(1).strip()


if __name__ == "__main__":
    loader = os.environ["LOADER"]
    artifact, version, minecraft, changelog = prepare(
        Path(sys.argv[1]), Path(sys.argv[2]), os.environ["RELEASE_TAG"], loader)
    Path("platform-changelog.md").write_text(changelog + "\n", encoding="utf-8")
    outputs = f"file={artifact.as_posix()}\nversion={version}\nminecraft={minecraft}\n"
    if os.environ.get("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
            output.write(outputs)
    print(f"Validated {artifact.name}; publishing metadata is for Minecraft {minecraft} / {loader}.")
