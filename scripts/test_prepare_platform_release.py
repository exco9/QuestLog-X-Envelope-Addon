import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location("prepare_release", Path(__file__).with_name("prepare-platform-release.py"))
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseValidationTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.source = self.root / "source"
        self.artifacts = self.root / "artifacts"
        self.source.mkdir()
        self.artifacts.mkdir()
        (self.source / "gradle.properties").write_text("mod_version=0.1.2\nminecraft_version=1.21.1\n")
        (self.source / "CHANGELOG.md").write_text("## [0.1.2] - 2026-10-06\n\nNew cargo.\n\n## [0.1.1]\nOld notes.\n")

    def artifact(self, loader="fabric", version="0.1.2"):
        path = self.artifacts / f"questlog-envelope-addon-{loader}-1.21.1-0.1.2.jar"
        with zipfile.ZipFile(path, "w") as jar:
            jar.writestr("questlog_envelope.mixins.json", "{}")
            if loader == "fabric":
                jar.writestr("fabric.mod.json", json.dumps({"id": "questlog_envelope", "version": version}))
            else:
                jar.writestr("META-INF/neoforge.mods.toml", f'[[mods]]\nmodId="questlog_envelope"\nversion="{version}"\n')
        return path

    def test_fabric_release_extracts_only_current_notes(self):
        self.artifact()
        _, version, minecraft, notes = release.prepare(self.source, self.artifacts, "v0.1.2", "fabric")
        self.assertEqual((version, minecraft, notes), ("0.1.2", "1.21.1", "New cargo."))

    def test_neoforge_release_metadata(self):
        self.artifact("neoforge")
        release.prepare(self.source, self.artifacts, "v0.1.2", "neoforge")

    def test_wrong_tag_fails(self):
        self.artifact()
        with self.assertRaises(ValueError):
            release.prepare(self.source, self.artifacts, "v0.1.1", "fabric")

    def test_wrong_jar_version_fails(self):
        self.artifact(version="0.1.1")
        with self.assertRaises(ValueError):
            release.prepare(self.source, self.artifacts, "v0.1.2", "fabric")

    def test_multiple_artifacts_fail(self):
        self.artifact()
        self.artifact("neoforge")
        with self.assertRaises(ValueError):
            release.prepare(self.source, self.artifacts, "v0.1.2", "fabric")

    def test_missing_changelog_section_fails(self):
        self.artifact()
        (self.source / "CHANGELOG.md").write_text("## [0.1.1]\nOld notes.")
        with self.assertRaises(ValueError):
            release.prepare(self.source, self.artifacts, "v0.1.2", "fabric")


if __name__ == "__main__":
    unittest.main()
