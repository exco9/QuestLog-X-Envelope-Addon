# QuestLog × Envelope Addon

Compatibility addon for **Questlog** and **Envelope** on Minecraft 1.21.1.

Current development target:

- Java 21
- Fabric
- NeoForge
- Questlog 3.3.2
- Envelope 0.7.5

See [`SYNTHESIS.md`](SYNTHESIS.md) for the architecture and analysis of both upstream projects.

## Questlog editor integration

The compatibility types are registered through Questlog's public objective/reward registries, so they are available directly from Questlog's in-game quest editor.

The editor keeps the real IDs internally but displays short localized names so they fit Questlog's narrow type picker.

### Receive quest mail

Use **Mail Received / Courrier reçu** (`questlog_envelope:mail_received`) as an objective or prerequisite when a quest should react to marked mail delivered by the addon.

The optional `quest` field selects the expected quest marker. When omitted, the objective uses its own parent quest ID.

### Send matching mail

Use **Mail Sent / Courrier envoyé** (`questlog_envelope:mail_sent`) as either an objective or prerequisite.

The event is recorded only when a player actually dispatches mail from an Envelope mailbox with a pigeon. Automated reward deliveries from this addon do not count as player-sent mail.

Questlog's normal target field configures the recipient address and `required_amount` configures how many matching mail pieces are required. **Mail filters... / Filtres du courrier...** adds optional filters:

- mail type: any, letter or package;
- text that a letter must contain;
- item that a package must contain;
- minimum quantity of that item.

All configured filters use AND semantics. Empty filters match anything.

Example:

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

Or a letter-content objective:

```json
{
  "type": "questlog_envelope:mail_sent",
  "recipient": "Some Address",
  "mail_kind": "letter",
  "text_contains": "I accept the contract"
}
```

### Send a quest letter

Add **Envelope Letter / Lettre Envelope** (`questlog_envelope:letter`) as a reward and use Questlog's **Quest granted by letter** field when the mail should progress another quest.

**Envelope Letter Options...** reuses Envelope's own letter paper and `TextBox`. Select text to reveal Envelope's native formatting toolbar for bold, italic, underline, strikethrough and colors. The small reminder under the letter is kept specifically to make this behavior discoverable.

A native Envelope wax seal can be selected from the built-in A–Z, 0–9 and emblem symbols. Wax seals are normal Envelope seals and have no special QuestLog behavior.

### Magic circles

A letter reward can enable **Magic circle / Cercle magique** independently of its Envelope wax seal.

The circle is drawn directly on the writable paper area and previewed in the letter reward editor. The center handle moves it and the upper-left handle resizes it. Hovering either handle identifies its purpose, the live size is shown while resizing, and holding **Shift** snaps movement/size to a small grid. The ↺ button resets position and size to the default bottom-right layout.

The editor also configures:

- activation color (`#RRGGBB`);
- hold duration, from **0.5 to 10 seconds** (3 seconds by default);
- an optional server-side command;
- an optional `grants_quest` Questlog action.

`grants_quest` and `magic_circle_command` can be used together. The server stores those executable actions independently of the physical letter and executes them once after validating the recipient and unique action ID.

When the player opens the delivered letter, holding the left mouse button on the circle progressively fills the artwork with the configured color. Releasing early or moving the cursor away cancels the hold. Completion uses a dedicated client-to-server payload rather than a visible/internal player command. Successful activation plays a short chime and visual pulse, then the circle remains permanently displayed in its configured color.

Each generated circle is bound to the intended player's UUID and receives a unique persisted action ID. The physical letter carries only its action ID and visual/interaction metadata. The server validates that ID against the letter the player is actually holding. A duplicated copy therefore cannot replay the action after the first copy consumes the ID; an old duplicate is simply recognized as already used.

The stored action representation is typed (`quest`, `command`, with room for additional action types later), so future behavior can be added without redesigning the editor interaction or anti-replay layer. Letter metadata and SavedData are versioned, and legacy first-generation action records remain readable.

```text
Quest reward created
  -> Envelope letter delivered
  -> player opens the letter
  -> player holds the magic circle for its configured duration
  -> dedicated C2S payload requests activation
  -> server validates recipient + held letter + unique action ID
  -> configured quest/command actions execute once
  -> circle remains on the letter in its configured activated color
```

Example:

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

A normal Envelope wax seal may still be added to the same letter with `"seal": "envelope:heart"`; sealing/unsealing and magic-circle activation are two separate mechanics.

The renderer first looks for the replaceable resource `assets/questlog_envelope/textures/gui/magic_circle.png`. Until a final raw artwork is supplied, the current artwork is kept as a text-resource fallback. This keeps the rendering code independent from a hard-coded Java Base64 blob and prepares the circle for normal resource-pack overrides.

### Send quest packages

Add **Envelope Package / Colis Envelope** (`questlog_envelope:package`) as a reward.

The visual editor uses Envelope's native six-slot package layout plus the player's current inventory. Clicking inventory stacks copies them into reward slots without modifying the real inventory. Additional package pages create additional physical packages.

The adjacent **Seal / Sceau** button selects a native Envelope wax seal. Packages do not use the letter magic-circle mechanic; `grants_quest` on a package follows the normal delivery marker behavior.

## Delivery behavior

Letters use Envelope's normal service-delivery timing when the player has a default mailbox.

Quest reward packages use express mailbox delivery: a real Envelope service pigeon starts near the recipient side and performs the final mailbox approach without the long simulated trip from the postal service/hub.

When the player has no linked mailbox, an Envelope service pigeon approaches the player in the Overworld. The delivered item is spawned at the delivery point with a short downward motion rather than via `player.drop(...)`, so a moving player no longer looks like they personally threw the mail.

Outside the Overworld, where Envelope's `MailService` does not operate, reward mail uses the safe direct-drop fallback.

Configured sender services fall back to Envelope's normal mail-service address when invalid or unavailable. Envelope routing errors never leave Questlog's **Collect Reward** permanently unclaimed.

## JSON examples

### Regular sealed letter

```json
{
  "type": "questlog_envelope:letter",
  "sender": "envelope:mail_service",
  "title": "New assignment",
  "text": "A new task is waiting for you.",
  "seal": "envelope:swords",
  "grants_quest": "example:mysterious_request",
  "auto_claim": true
}
```

### Packages

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

## Current status

Implemented on `dev/initial-integration`:

- multiloader Fabric + NeoForge project;
- `questlog_envelope:mail_received` objective/prerequisite;
- `questlog_envelope:mail_sent` objective/prerequisite with recipient/type/text/item filters;
- mailbox-send observation that excludes addon-generated service mail;
- offline persistence/replay for delivered quest markers;
- Envelope letter and package rewards;
- real Envelope red-wax seals with built-in symbol preview/selection;
- independent letter magic circles with visual move/resize controls, reset and Shift snapping;
- configurable magic-circle color, hold duration and command action;
- dedicated Fabric + NeoForge C2S activation payload;
- typed/versioned server-side magic-circle actions with legacy-record migration;
- persistent recipient binding and anti-replay protection for magic-circle actions;
- persistent activated-circle color plus short activation pulse/chime feedback;
- resource-backed magic-circle artwork loading prepared for normal PNG/resource-pack replacement;
- native Envelope letter formatting including bold/italic/underline/colors;
- visual package editor with inventory copying and multiple six-slot pages;
- express package delivery to registered mailboxes;
- service-pigeon fallback for players without mailboxes;
- pigeon-position item spawning for direct delivery;
- safe sender fallback and non-blocking Questlog reward collection;
- short localized type labels and English/French translations.

The remaining work is mainly in-game regression testing on both loaders and replacing/adding final custom visual/audio assets where desired.
