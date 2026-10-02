# QuestLog × Envelope Addon

Compatibility addon between **Questlog** and **Envelope** for Minecraft **1.21.1**.

Current target:

- Java 21
- Fabric + NeoForge
- Questlog 3.3.2
- Envelope 0.7.5
- Gradle 9.5.0

The addon keeps Questlog as the quest system and Envelope as the physical mail system. Quest progression is driven through Questlog's native objective/reward model, while visible mail deliveries use Envelope's mail, addressing, mailbox, and pigeon behavior.

See [`SYNTHESIS.md`](SYNTHESIS.md) for the technical architecture and [`TESTING.md`](TESTING.md) for the release regression matrix.

## Installation

Install the jar matching your loader together with the matching Minecraft 1.21.1 versions of Questlog and Envelope and their normal dependencies.

### Ember's Text API (optional)

Install [Ember's Text API](https://github.com/TysonTheEmber/EmbersTextAPI) 3.x for
Minecraft 1.21.1 and your loader to enable its text effects in received letters.
The addon uses Ember's native Minecraft Component/Font hooks; no API code is bundled
and the mod still works when Ember is absent, including on dedicated servers.

Use **Ember preview** above the paper to see wrapped, animated text while authoring.
Turn it off to edit the raw markup. Preview clips to the writable paper and leaves
signatures, circles, saving and the stored letter source intact. For example:
`<rainbow>Greetings, adventurer!</rainbow>` or `<wave>Welcome to the academy.</wave>`.
Effects and custom fonts follow Ember's own client configuration and resource packs.
Received letters retain Envelope's native wrapping and hover/click behavior.

Do not install both addon jars at the same time:

- Fabric: use the Fabric addon jar and Fabric builds of Questlog/Envelope.
- NeoForge: use the NeoForge addon jar and NeoForge builds of Questlog/Envelope.

CI build artifacts contain both loader-specific jars. Tagged releases are configured to publish the verified distributable jars as GitHub release assets.

## Questlog editor integration

The compatibility types are registered through Questlog's objective/reward registries and are available directly from the in-game quest editor. Internal IDs remain stable while the editor displays short localized labels.

### Receive quest mail

Use **Mail Received / Courrier reçu** (`questlog_envelope:mail_received`) as an objective or prerequisite when a quest should react to marked mail delivered by the addon.

The optional `quest` field selects the expected quest marker. When omitted, the objective uses its own parent quest ID. Delivered markers are persisted so a letter received while the target player is offline can still progress the quest after their Questlog state loads.

### Send matching mail

Use **Mail Sent / Courrier envoyé** (`questlog_envelope:mail_sent`) as an objective or prerequisite.

The event is recorded only when a player actually dispatches mail from an Envelope mailbox with a pigeon. Addon-generated service/reward mail is excluded.

Questlog's normal target field configures the recipient and `required_amount` configures how many matching pieces are required. **Mail filters... / Filtres du courrier...** adds optional filters for:

- mail type: any, letter, or package;
- text a letter must contain;
- item a package must contain;
- minimum quantity of that item.

All configured filters use AND semantics. Empty filters match anything.

```json
{
  "type": "questlog_envelope:mail_sent",
  "recipient": "envelope:some_service",
  "mail_kind": "package",
  "item": "minecraft:diamond",
  "item_count": 3,
  "required_amount": 1
}
```

Letter-content example:

```json
{
  "type": "questlog_envelope:mail_sent",
  "recipient": "Some Address",
  "mail_kind": "letter",
  "text_contains": "I accept the contract"
}
```

## Envelope letter rewards

Add **Envelope Letter / Lettre Envelope** (`questlog_envelope:letter`) as a Questlog reward. The editor supports sender, title, rich text, auto-claim, Envelope wax seals, optional `grants_quest`, and the addon magic-circle mechanic.

The letter editor reuses Envelope's writable-paper presentation and text formatting behavior. Selecting text exposes formatting for bold, italic, underline, strikethrough, and colors.

A native Envelope wax seal can be selected from built-in letters, numbers, and emblem symbols. Wax seals use normal Envelope behavior and are independent of quest mechanics.

### Signatures

Use **Signature** beneath the magic-circle settings to place a signature, then click or drag it
on the letter. Its contextual controls appear alongside the ordinary sender, title
and reward settings; clicking elsewhere on the paper restores the circle/seal controls.
Set the ink color (`#RRGGBB`), size (8–28), and optional compact rectangular frame.
Drag the gold bottom-right handle vertically to resize; hold Shift while moving to
snap to a four-pixel grid. Reset restores placement and size; Remove deletes the signature.

Use **Player name (@s)** or type `@s` as a standalone token in the signature text.
The editor previews your name; delivery resolves the token once to the rewarded
player's profile name. Giving that letter to another reader does not change its signature.
Literal names remain supported, and `@s` inside words/email addresses is left alone.

Before signing, the received letter shows a faint signature guide (35% opacity).
Its printed frame remains fully opaque, like a contract's signing box.
After server confirmation, ink and frame smoothly turn to **Magic ink** (default
enchantment-style purple `#AA55FF`). A short sparkle burst celebrates signing;
a diagonal glint continues on the signed ink and frame, including after reopening.
The contextual inspector lets you change the normal and magic ink colors separately.
Hold the letter and click its signature: opaque ink appears from left to right over
1.1 seconds, following an animated vanilla feather with the cartography-table drawing sound.
After the trace completes, a dedicated serverbound request marks the actual held letter
as signed. The server verifies the item and signature against its own data and rejects
repeat signing. The signed flag persists in item components across closing, inventory
moves, copies and world/server restarts. Legacy letters without the flag begin unsigned.

Repeated clicks during writing or while awaiting confirmation are ignored. Closing before
the trace finishes cancels signing and stops the sound. A completed signature is fully
opaque and cannot play the writing animation again. Circle actions remain independent.
Magic-circle controls keep priority where decorations overlap.

**Command after signing** is optional. Like circle commands, it executes on the server
as the original recipient with permission level 4; `@s` selects the signer. The command
stays in server SavedData, while the item and signing packet only carry its action UUID.
Signing consumes that UUID before executing, so copied unsigned letters and world
restarts cannot replay it. A command-bearing letter can initially be signed only by
its intended recipient. Legacy/decorative signatures with no command still work.

**Save Quest** validates the letter, commits its reward through Questlog's native editor,
and saves the whole quest to the server in one click. This includes other pending quest
changes. Cancel discards the letter edits and returns to the quest editor.

Signatures coexist with wax seals and magic circles. Long names fit the writable paper,
and older unsigned letters remain supported.

```json
{
  "type": "questlog_envelope:letter",
  "title": "Invitation",
  "text": "Your presence is requested at the academy.",
  "signature": "@s",
  "signature_color": "#7B593D",
  "signature_size": 12,
  "signature_x": 40,
  "signature_y": 110,
  "signature_frame": true,
  "signature_magic_color": "#AA55FF",
  "signature_command": "/say Signed by @s"
}
```

The bundled **BitScript** pixel handwriting font (devurandom / usr_share;
TrueType conversion by William.Thompsonj) is released under **CC0**:
[original bitmap](https://opengameart.org/content/bitscript-a-low-res-handwriting-font),
[TrueType font](https://opengameart.org/content/bitscript-true-type-font).
Credits/license information ships at `assets/questlog_envelope/font/BitScript-LICENSE.txt`.
A bitmap atlas copies the original CC0 Grafx2 pixel glyphs directly, avoiding
broken strokes from rasterizing the TrueType conversion. Normal-size glyphs
use whole physical pixels at the current GUI scale. Minecraft's default font supplies
missing glyphs. Very long names still fit the paper. Resource packs can replace
`assets/questlog_envelope/font/signature.json` and `textures/font/signature.png`.
Regenerate the bundled atlas with `java scripts/GenerateSignatureFont.java`.

### Magic circles

Activated circles fade to their **Magic ink** color (`magic_circle_magic_color`,
default `#AA55FF`), then retain a tinted diagonal glint. A radial sparkle burst
plays once on a fresh server-confirmed activation, never when reopening a used letter.
The initial fill color and final magical color can be configured separately.

A letter reward can enable an interactive **Magic circle / Cercle magique** independently of its wax seal.

The editor previews the circle directly on the writable paper. The center handle moves it, the upper-left handle resizes it, Shift enables snapping, and the reset button restores the default bottom-right layout.

The editor configures:

- activation color (`#RRGGBB`);
- hold duration from 0.5 to 10 seconds;
- an optional server-side command;
- an optional `grants_quest` action.

Quest and command actions can be combined. Executable actions are stored server-side, bound to the intended player's UUID, and keyed by a unique persistent action ID. The physical letter carries only the action ID plus visual/interaction metadata.

When the player opens the delivered letter and holds the circle for the configured duration, a dedicated client-to-server payload requests activation. The server validates the recipient, the currently held letter, and the action ID before executing anything. Successful activation persists, plays feedback, and consumes the action ID so duplicated copies cannot replay it.

```json
{
  "type": "questlog_envelope:letter",
  "title": "Expedition invitation",
  "text": "Activate the circle if you accept the expedition.",
  "magic_circle": true,
  "magic_circle_color": "#55AAFF",
  "magic_circle_command": "/say Circle activated by @s",
  "magic_circle_hold_seconds": 3.0,
  "magic_circle_x": 103,
  "magic_circle_y": 105,
  "magic_circle_size": 36,
  "grants_quest": "example:expedition",
  "auto_claim": true
}
```

The circle artwork is a replaceable Minecraft resource at `assets/questlog_envelope/textures/gui/magic_circle.png`. Fabric and NeoForge invalidate the generated tint-mask cache on client resource reload.

## Envelope package rewards

Add **Envelope Package / Colis Envelope** (`questlog_envelope:package`) as a reward.

The visual editor uses Envelope's six-slot package layout plus the player's current inventory. Clicking an inventory stack copies it into reward configuration without modifying the real inventory. Additional pages create additional physical packages, and packages can use normal Envelope wax seals.

```json
{
  "type": "questlog_envelope:package",
  "sender": "envelope:mail_service",
  "title": "Expedition supplies",
  "seal": "envelope:book",
  "packages": [
    [
      { "id": "minecraft:bread", "count": 16 },
      null,
      { "id": "minecraft:iron_ingot", "count": 8 },
      null,
      null,
      null
    ]
  ],
  "grants_quest": "example:next_assignment",
  "auto_claim": true
}
```

Legacy package definitions using `"items": [...]` remain supported.

## Delivery behavior

Letters use Envelope's normal service-delivery timing when the player has a default mailbox.

Quest reward packages use express mailbox delivery: an Envelope service pigeon performs the final recipient-side approach without the long simulated trip from the postal hub.

When the player has no linked mailbox in the Overworld, a service-pigeon/direct-delivery fallback completes the reward near the player. Outside the Overworld, where Envelope's `MailService` does not operate, reward mail uses the safe direct-drop fallback.

Invalid or unavailable configured sender services fall back to Envelope's normal mail-service address. Routing failures are handled so Questlog's reward does not remain permanently unclaimed.

## Building

A Gradle wrapper is committed/generated for reproducible local builds. With Java 21 installed:

```bash
./gradlew build --stacktrace
bash scripts/verify-build.sh
```

The smoke-check script validates loader metadata and shared resources and collects distributable jars into `dist/`.

GitHub Actions builds pull requests and pushes to `main` and `dev/**`, then retains `dist/*.jar` as a workflow artifact.

## Testing

The automated build is only the first release gate. Minecraft integration behavior should also be exercised in-game on **both Fabric and NeoForge**.

Follow [`TESTING.md`](TESTING.md), which covers startup, editor registration, online/offline quest mail, sent-mail filters, letters, packages, seals, formatting, magic-circle validation/anti-replay, persistence, mailbox fallbacks, dimensions, resource reload, and localization.

## Releases

Before creating a release:

1. complete the regression matrix on both loaders;
2. update [`CHANGELOG.md`](CHANGELOG.md);
3. set `mod_version` in `gradle.properties` to the release version without `-SNAPSHOT`;
4. push the release commit and confirm CI is green;
5. create/push a matching tag such as `v0.1.0`.

The tag workflow refuses to publish if the tag version does not exactly match `mod_version`. A valid `v*` tag builds, smoke-checks, and attaches the Fabric and NeoForge jars to a GitHub release.

## Current implementation

Implemented:

- multiloader Fabric + NeoForge project;
- `mail_received` with offline persistence/replay;
- `mail_sent` with recipient/type/text/item/quantity filters;
- exclusion of addon-generated mail from player-send progression;
- Envelope letter and package rewards;
- native wax seals and letter rich-text editing;
- interactive magic circles with configurable visuals/timing;
- quest and server-command magic-circle actions;
- dedicated Fabric + NeoForge C2S activation networking;
- recipient binding, persistent action IDs, migration, and anti-replay protection;
- persistent activated-circle state and resource-pack reload support;
- multi-page package editor;
- mailbox, no-mailbox, and non-Overworld delivery paths;
- safe sender fallback and non-blocking reward collection;
- English and French localization;
- automated build/package smoke checks and release workflow.

Release 0.1.0 includes magical signatures, one-shot signing commands and optional
Ember text previews. Automated tests and distribution checks are recorded during
release preparation. The full in-game regression matrix in `TESTING.md`, including
Ember-installed rendering, remains a manual validation step on both loaders.

## License

This project is distributed under **GNU GPL v3.0**. See `LICENSE`.

The license choice reflects the direct integration with Envelope's GPLv3 code/API and is a project licensing decision, not legal advice.
