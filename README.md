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

### Magic circles

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

The main release blocker for 0.1.0 is completing and recording the in-game regression pass on both loaders, then fixing anything it exposes.

## License

This project is distributed under **GNU GPL v3.0**. See `LICENSE`.

The license choice reflects the direct integration with Envelope's GPLv3 code/API and is a project licensing decision, not legal advice.
