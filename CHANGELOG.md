# Changelog

All notable changes to QuestLog × Envelope Addon are documented here.

The project follows Semantic Versioning for published releases where practical.

## [Unreleased]

### Added

- Fabric and NeoForge multiloader support for Minecraft 1.21.1 / Java 21.
- Questlog `mail_received` and `mail_sent` objectives/prerequisites.
- Recipient, mail-kind, letter-text, package-item, and package-quantity filters for sent-mail objectives.
- Offline persistence and replay for quest mail markers.
- Envelope letter and package Questlog rewards.
- Native Envelope wax-seal selection and rich letter formatting.
- Configurable interactive magic circles with quest and server-command actions.
- Dedicated Fabric and NeoForge client-to-server activation networking.
- Server-side recipient validation, persistent action IDs, and anti-replay protection.
- Multi-page visual package editor.
- Mailbox, no-mailbox, and non-Overworld delivery fallbacks.
- English and French localization.
- Automated cross-loader build artifact smoke checks and a manual regression matrix.
- CI artifact packaging for Fabric and NeoForge builds.
- Tag-driven GitHub release packaging.

### Changed

- Documentation now reflects the implemented integration rather than the original MVP plan.
- Build tooling is standardized on the Gradle version used by the green CI build.

### Still to validate before 0.1.0

- Complete the in-game regression matrix in `TESTING.md` on both Fabric and NeoForge.
- Resolve any loader-specific runtime regressions discovered during that pass.
- Replace `0.1.0-SNAPSHOT` with the intended release version before tagging.
