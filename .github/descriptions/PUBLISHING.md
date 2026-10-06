# Platform publishing

The `release` workflow publishes verified GitHub release JARs to Modrinth and CurseForge after building a new `vX.Y.Z` tag. Each platform receives separate Fabric and NeoForge versions for Minecraft 1.21.1, with loader-specific dependencies and the matching changelog section.

Configure these repository **Actions secrets**:

- `MODRINTH_TOKEN`: Modrinth token permitted to create versions on the project.
- `CURSEFORGE_TOKEN`: CurseForge author upload token.

Configure these repository **Actions variables**:

- `MODRINTH_PROJECT_ID`: the eight-character project ID for `https://modrinth.com/mod/qlxe`.
- `CURSEFORGE_PROJECT_ID`: the numeric project ID for `https://www.curseforge.com/minecraft/mc-mods/qlxe`.

Tokens must be separate secrets, not a combined value. Their values are never written to repository files.

To publish an existing GitHub release, run **Publish CurseForge and Modrinth** from the Actions tab and enter its tag. The default **dry_run** checks artifacts, metadata and changelog without uploading. Disable it only for the actual upload. Configuration is checked before live uploads begin.

If an upload partially succeeds, use GitHub's **Re-run failed jobs** to retry failed platform/loader jobs; do not rerun successful uploads. CurseForge may require moderation before an uploaded file becomes public. An existing tag and GitHub release are never rebuilt or replaced by this workflow.

Local validation:

```sh
python3 -m unittest discover -s scripts -p 'test_prepare_platform_release.py'
```
