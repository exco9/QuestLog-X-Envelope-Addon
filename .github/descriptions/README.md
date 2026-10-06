# Project descriptions

**Short description for both platforms:**

Connect Questlog and Envelope with quest mail, package rewards, magic circles and player signatures.

- [CurseForge](curseforge.md): paste the file's contents into the description editor in Markdown mode. CurseForge provides both [Markdown and visual editors](https://support.curseforge.com/support/solutions/articles/9000199552-overview-of-the-project-submission-page).
- [Modrinth](modrinth.md): paste the file's contents into the project description, which uses [GitHub Flavored Markdown](https://support.modrinth.com/en/articles/8801962-advanced-markdown-formatting).

Both descriptions now mirror the README, including the banner, feature overview, showcase GIF and JSON example. All images use public HTTPS URLs; no image relies on access to this private repository.

The **Sync project descriptions** GitHub workflow uploads the current feature image to the Modrinth gallery, updates the Modrinth project body and produces ready-to-paste CurseForge Markdown. It reuses an identical gallery image on later runs. The workflow retains a backup of the previous Modrinth description.

CurseForge's author upload token does not expose description editing through the documented upload API. Apply [curseforge.md](curseforge.md) through the connected author dashboard's Markdown editor.

The descriptions omit private GitHub download and guide links that visitors cannot access. The feature image has descriptive alternative text for accessibility.
