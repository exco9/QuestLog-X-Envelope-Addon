# Changelog

All notable changes to QuestLog × Envelope Addon are documented here.

The project follows Semantic Versioning for published releases where practical.

## [Unreleased]

### Fixed

- Apply each `mail_received` delivery to all matching prerequisites and regular objectives, including nested objectives and multiple quests. Keep queued deliveries when Questlog prerequisite gating prevents any progress.
- Use lowercase paths for the bundled BitScript source atlas and license so Minecraft no longer rejects those resource entries.

### Tests

- Add nine regression tests using Questlog's actual quest and objective classes: prerequisite and objective traversal, multiple deliveries, nested groups, locked quests, pending-event consumption and save/reload isolation.

## [0.1.0] - 2026-10-03

### Added

- Optional Ember's Text API 3.x compatibility through Minecraft's native styled Component rendering, plus a bounded live preview that preserves editable markup.

- One-time signature-writing animation on received letters: progressive ink, moving vanilla feather and cartography-table drawing sound.
- Animation timing, signing persistence and network-codec tests and signature hover help.

- Optional cursive letter signatures with ink color, size, draggable placement, grid snapping and a rectangular frame.
- Contextual signature controls, delivered-item metadata and bundled CC0 BitScript pixel handwriting font.
- Automated signature JSON/NBT round-trip, compatibility and invalid-input tests; font-resource packaging checks.

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

- Unsigned signature text renders at reduced opacity, while its printed frame stays opaque; completed signatures persist server-side and cannot be signed again.
- Signing and circle activation fade to configurable magic ink (enchantment-style purple by default), with tinted persistent diagonal glint and a one-time sparkle burst after server confirmation.
- Optional signature commands use an independent server-side one-shot ledger, owner checks and action UUID matching to prevent repeats, copied-letter replay and signing the wrong command-bearing letter.
- Signature control now sits beneath circle settings; command fields use the full inspector width.
- Fixed bitmap character mapping: only blue Grafx2 markers are separators, so the dollar-sign ink no longer shifts every following character.
- Feather motion uses continuous positions, gentle acceleration and varied strokes instead of mechanical pixel steps.
- Font atlas now preserves the original CC0 BitScript pixel glyphs instead of rasterizing thin TrueType outlines into broken strokes.
- BitScript now uses a bitmap atlas without antialiasing and normal text scales align to physical pixels.

- Signatures are placed and selected directly on paper, with compact one-line frames and recipient-name `@s` templates.
- Save Quest from the letter editor commits the reward and saves the complete quest through Questlog in one click.

- Documentation now reflects the implemented integration rather than the original MVP plan.
- Build tooling is standardized on the Gradle version used by the green CI build.

### Still to validate before 0.1.0

- Complete the in-game regression matrix in `TESTING.md` on both Fabric and NeoForge.
- Resolve any loader-specific runtime regressions discovered during that pass.
- Replace `0.1.0-SNAPSHOT` with the intended release version before tagging.
