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

A native Envelope wax seal can be selected from the built-in A–Z, 0–9 and emblem symbols.

### Magic seals

A sealed letter or package reward can enable **Magic seal / Sceau magique**.

When magic mode is disabled, `grants_quest` behaves normally and progresses the destination quest when the mail is delivered.

When magic mode is enabled, `grants_quest` is deliberately delayed. The player must hold right click long enough to break Envelope's physical seal. Only then does the addon validate the one-time action and progress the target quest.

```text
Quest reward created
  -> sealed Envelope mail delivered
  -> player holds right click
  -> Envelope breaks the wax seal
  -> addon validates recipient + unique action ID
  -> grants_quest is applied
```

Magic actions are bound to the intended player's UUID and carry a unique persisted action ID. Duplicating/copying the physical item therefore cannot replay the same quest progression. Arbitrary command execution is not part of the system.

Example:

```json
{
  "type": "questlog_envelope:letter",
  "title": "Expedition invitation",
  "text": "Break the seal if you accept the expedition.",
  "seal": "envelope:heart",
  "magic_seal": true,
  "grants_quest": "example:expedition",
  "auto_claim": true
}
```

The target quest can use the existing `mail_received` prerequisite for that quest ID. With `magic_seal: true`, the matching marker is applied only when the seal is actually broken instead of at delivery time.

### Send quest packages

Add **Envelope Package / Colis Envelope** (`questlog_envelope:package`) as a reward.

The visual editor uses Envelope's native six-slot package layout plus the player's current inventory. Clicking inventory stacks copies them into reward slots without modifying the real inventory. Additional package pages create additional physical packages.

The adjacent **Seal / Sceau** button selects a native Envelope seal and also exposes the magic-seal toggle. When multiple physical packages are generated, only the first package carries the one-time `grants_quest` action.

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
- magic seals that delay `grants_quest` until the recipient breaks the seal;
- persistent anti-replay protection for magic-seal actions;
- native Envelope letter formatting including bold/italic/underline/colors;
- visual package editor with inventory copying and multiple six-slot pages;
- express package delivery to registered mailboxes;
- service-pigeon fallback for players without mailboxes;
- pigeon-position item spawning for direct delivery;
- safe sender fallback and non-blocking Questlog reward collection;
- short localized type labels and English/French translations.

Next steps are primarily testing/polish and any later service-reply or integration-test work.
