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

The editor keeps the real IDs (`questlog_envelope:letter`, `questlog_envelope:package`, `questlog_envelope:mail_received`) internally, but displays short localized names so they fit Questlog's narrow type picker.

### Make a quest unlock when its mail arrives

1. Open/create the destination quest in Questlog's editor.
2. Open **Prerequisites**.
3. Add **Mail Received / Courrier reçu** (`questlog_envelope:mail_received`).
4. Leave the letter/package quest marker empty to use the current quest ID automatically, or select another Quest ID.
5. Leave the required amount at `1` for a normal quest delivery.

### Send a quest letter from another quest

1. Open the source quest in Questlog's editor.
2. Open **Rewards**.
3. Add **Envelope Letter / Lettre Envelope** (`questlog_envelope:letter`).
4. Use **Quest granted by letter** to select the destination Quest ID. This field uses Questlog's native Quest-ID autocomplete.
5. Optionally set the reward **Name** and **Icon** as usual in Questlog.
6. Press **Envelope Letter Options...**.
7. The body is edited on Envelope's own letter-and-quill paper using Envelope's native `TextBox`, so wrapping and formatting controls match a normal in-game letter.
8. Select text to use Envelope's native formatting toolbar (bold, italic, underline and colors). Envelope keyboard formatting shortcuts also remain available.
9. Select a global text-size preset: **Small / Normal / Large**. Normal letters remain completely native; small/large quest letters use an addon view that keeps Envelope's paper and formatting while scaling the text layout.
10. Optionally select a native Envelope wax-seal symbol. The editor renders the real Envelope seal preview.

The `seal` option uses Envelope's own seal registry. Built-in choices include A–Z, 0–9 and emblems such as heart, book, swords, creeper, emerald, villager and more. Quest rewards currently use Envelope's native red-wax seal material.

### Send a quest package

1. Add **Envelope Package / Colis Envelope** (`questlog_envelope:package`) to a quest's rewards.
2. Optionally select **Quest granted by package**.
3. Press **Options...** to edit contents, sender and title.
4. The editor shows Envelope's native six-slot package layout plus the player's current inventory.
5. Select a package slot, then left-click an inventory item to copy its complete stack into the reward. Right-click an inventory item copies one item. The real inventory is never modified.
6. Right-click a configured package slot to clear it.
7. Use **Add package** to create another physical six-slot package and the arrow buttons to move between packages.
8. Use the adjacent **Seal / Sceau** button to choose a native Envelope seal for every physical package in that reward.

Each editor page corresponds to one real Envelope package. The saved `packages` format preserves all six slot positions, including empty slots. Old quest definitions using a flat `items` array remain supported and are migrated to explicit package pages when saved through the new editor.

The Envelope-specific editors work on Questlog's temporary reward entry, so Questlog's normal **Cancel** behavior remains available.

## Delivery behavior

Letters keep Envelope's normal service-delivery timing when the player has a default mailbox.

Quest reward **packages use express mailbox delivery**: when a default mailbox exists, the addon creates a real Envelope service pigeon near the recipient side and starts it directly in the mailbox-approach phase. The pigeon is still visible and performs the physical drop-off, but the long simulated trip from the postal service/hub is skipped.

When the player has **no mailbox linked to their PlayerAddress**, the addon switches only its own reward mail to a direct-delivery fallback: in the Overworld, an Envelope service pigeon appears near the player and approaches the position captured for the final delivery. The delivered item is spawned at that delivery point with a short downward motion instead of using Minecraft's `player.drop(...)`, so a moving player no longer looks like they personally threw the letter/package. Outside the Overworld, where Envelope's `MailService` does not operate, the reward is dropped directly and safely.

A configured sender service is resolved safely. If its resource ID is syntactically valid but no longer registered, the addon logs a warning and falls back to Envelope's normal mail-service address. Envelope routing errors also no longer leave Questlog's **Collect Reward** action permanently unclaimed.

## Quest unlocked by receiving mail

A quest can wait for a marked Envelope letter or package before Questlog considers it triggered:

```json
{
  "title": "A mysterious request",
  "prerequisites": [
    {
      "type": "questlog_envelope:mail_received",
      "quest": "example:mysterious_request"
    }
  ],
  "objectives": [
    {
      "type": "questlog:item_obtain",
      "item": "minecraft:amethyst_shard",
      "required_amount": 8
    }
  ]
}
```

If `quest` is omitted from `mail_received`, the parent Questlog quest ID is used as the expected mail marker.

## Send a sealed quest letter as a reward

```json
{
  "type": "questlog_envelope:letter",
  "sender": "envelope:mail_service",
  "title": "New assignment",
  "text": "A new task is waiting for you.",
  "font_size": "large",
  "seal": "envelope:heart",
  "grants_quest": "example:mysterious_request",
  "auto_claim": true
}
```

`font_size` accepts `small`, `normal` or `large` (`normal` is the default and may be omitted). `seal` accepts a registered Envelope seal-symbol ID, for example `envelope:heart`, `envelope:swords`, `envelope:letter/a` or `envelope:number/7`.

## Send sealed quest packages as a reward

The visual editor writes explicit six-slot pages. `null` means an empty slot:

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
    ],
    [
      { "id": "minecraft:diamond", "count": 2 },
      null,
      null,
      null,
      null,
      null
    ]
  ],
  "grants_quest": "example:next_assignment",
  "auto_claim": true
}
```

Legacy definitions with `"items": [...]` are still accepted. For deliveries that contain `grants_quest`, the quest marker is placed only on the first physical package so one reward does not advance `mail_received` multiple times.

Quest-mail markers are persisted in Overworld `SavedData`. If marked mail reaches a mailbox while the player is offline, the marker remains pending and is applied after Questlog finishes loading/deserializing that player's quests on the next login.

This enables narrative chains such as:

```text
Quest A complete
  -> Questlog reward
  -> sealed Envelope letter/package is dispatched
  -> pigeon delivers it to the mailbox, or directly to a player without one
  -> quest marker is detected/persisted
  -> Quest B becomes triggered by Questlog
```

## Planned: interactive / magical seals

This is intentionally **design-only for now**.

Envelope's sealed letters already use a hold-right-click interaction to break/unseal the wax. The preferred design is therefore to make that existing gesture the confirmation action rather than adding a competing second gesture:

```text
Player receives sealed quest mail
  -> holds right click to break the special seal
  -> visual charge / particles / sound during the hold
  -> seal breaks
  -> server validates a one-time seal action
  -> configured Questlog action executes
```

Example future data shape:

```json
{
  "seal": "envelope:heart",
  "magic_seal": {
    "action": "accept_quest",
    "quest": "example:expedition",
    "confirmation_text": "Validate your participation in this expedition"
  }
}
```

Initial action types should stay narrow and server-validated, for example `accept_quest`, `grant_quest` or a dedicated Questlog flag/progression action. Arbitrary command execution should **not** be enabled by default. A unique action ID should be persisted per player so copying or duplicating the physical letter cannot trigger the same progression repeatedly.

The magic metadata should live in the addon's custom mail data while the visual seal remains Envelope's native `SEAL` component. That keeps normal seals fully compatible and lets interactive seals be added later without changing existing reward JSON or replacing Envelope's rendering.

## Current status

Implemented on `dev/initial-integration`:

- multiloader project skeleton;
- `questlog_envelope:mail_received`;
- quest marker stored in vanilla `CUSTOM_DATA`;
- mailbox delivery observation and offline quest-marker persistence;
- replay of pending unlocks after Questlog player data is loaded;
- `questlog_envelope:letter` reward;
- `questlog_envelope:package` with explicit six-slot physical package pages and legacy `items` compatibility;
- native Envelope red-wax seals for letter and package rewards, with symbol preview/selection;
- native Envelope text formatting, including bold/italic/underline/colors;
- small/normal/large global quest-letter text sizes;
- express service-pigeon delivery for quest packages sent to a registered mailbox;
- configurable sender service with safe fallback to Envelope's mail service;
- direct service-pigeon delivery when a player has no default mailbox;
- pigeon-position item spawning for direct deliveries instead of `player.drop(...)`;
- `grants_quest` support for letters and packages;
- Questlog in-game editor integration for compatibility objectives/rewards;
- native Envelope-style letter editor with formatting preview;
- visual package editor with inventory stack copying and multiple package pages;
- short localized type labels in Questlog's type picker;
- English/French translations;
- Gradle CI definition.

Next steps:

- prototype interactive/magical seal actions after the normal seal UX is validated;
- implement `questlog_envelope:mail_delivery` for sending mail to quest services;
- add package/content filters for objectives;
- add service replies;
- add game/integration tests;
- investigate why GitHub Actions checks are not currently appearing on the private repository.
