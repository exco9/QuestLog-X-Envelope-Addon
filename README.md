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

### Make a quest unlock when its mail arrives

1. Open/create the destination quest in Questlog's editor.
2. Open **Prerequisites**.
3. Add the **Mail Received** type (`questlog_envelope:mail_received`).
4. Leave **Letter/package quest marker** empty to use the current quest ID automatically, or select another Quest ID.
5. Leave the required amount at `1` for a normal quest delivery.

### Send a quest letter from another quest

1. Open the source quest in Questlog's editor.
2. Open **Rewards**.
3. Add **Letter** (`questlog_envelope:letter`).
4. Use **Quest granted by letter** to select the destination Quest ID. This field uses Questlog's native Quest-ID autocomplete.
5. Optionally set the reward **Name** and **Icon** as usual in Questlog.
6. Press **Envelope Letter Options...** to configure:
   - sender service (blank uses Envelope's mail service);
   - physical letter title;
   - physical letter body;
   - whether the reward is auto-claimed.
7. Press **Done**, then save the quest normally in Questlog.

### Send a quest package

1. Add **Package** (`questlog_envelope:package`) to a quest's rewards.
2. Optionally select **Quest granted by package**.
3. Press **Envelope Package Options...**.
4. Configure the sender, package title, auto-claim setting and package contents.
5. Enter one item per line using `item [count]`, for example:

```text
minecraft:bread 16
minecraft:iron_ingot 8
minecraft:diamond
```

A full ItemStack JSON object can also be supplied on a line when custom components are needed. Envelope packages hold six slots; when the configured contents require more room, the addon uses Envelope's native package splitting and sends as many packages as required.

The Envelope-specific editors work on Questlog's temporary reward entry, so Questlog's normal **Cancel** behavior remains available.

## Delivery behavior

When the player has a default Envelope mailbox, letters and packages use Envelope's normal service-delivery flow.

When the player has **no mailbox linked to their PlayerAddress**, the addon switches only its own reward mail to a direct-delivery fallback: an Envelope service pigeon appears near the player, approaches them and drops the delivered letter/package at their position instead of returning it as `recipient not found`.

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

## Send a quest letter as a reward

```json
{
  "type": "questlog_envelope:letter",
  "sender": "envelope:mail_service",
  "title": "New assignment",
  "text": "A new task is waiting for you.",
  "grants_quest": "example:mysterious_request",
  "auto_claim": true
}
```

## Send a quest package as a reward

```json
{
  "type": "questlog_envelope:package",
  "sender": "envelope:mail_service",
  "title": "Expedition supplies",
  "items": [
    { "item": "minecraft:bread", "count": 16 },
    { "item": "minecraft:iron_ingot", "count": 8 }
  ],
  "grants_quest": "example:next_assignment",
  "auto_claim": true
}
```

For deliveries that contain `grants_quest`, the quest marker is stored on the Envelope mail stack and consumed by the `mail_received` prerequisite when the delivery succeeds. If a package reward is split into multiple Envelope packages, the marker is placed only on the first package so one reward does not advance the prerequisite multiple times.

Quest-mail markers are persisted in world `SavedData`. If marked mail reaches a mailbox while the player is offline, the marker remains pending and is applied after Questlog finishes loading/deserializing that player's quests on the next login.

This enables narrative chains such as:

```text
Quest A complete
  -> Questlog reward
  -> Envelope letter/package is dispatched
  -> pigeon delivers it to the mailbox, or directly to a player without one
  -> quest marker is detected/persisted
  -> Quest B becomes triggered by Questlog
```

## Current status

Implemented on `dev/initial-integration`:

- multiloader project skeleton;
- `questlog_envelope:mail_received`;
- quest marker stored in vanilla `CUSTOM_DATA`;
- mailbox delivery observation and offline quest-marker persistence;
- replay of pending unlocks after Questlog player data is loaded;
- `questlog_envelope:letter` reward;
- `questlog_envelope:package` reward with native Envelope package splitting;
- configurable sender service with safe fallback to Envelope's mail service;
- direct service-pigeon delivery when a player has no default mailbox;
- `grants_quest` support for letters and packages;
- Questlog in-game editor integration for compatibility objectives/rewards;
- dedicated Envelope letter/package option screens with English/French translations;
- Gradle CI definition.

Next steps:

- implement `questlog_envelope:mail_delivery` for sending mail to quest services;
- add package/content filters for objectives;
- add service replies;
- add game/integration tests;
- investigate why GitHub Actions checks are not currently appearing on the private repository.
