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

### Make a quest unlock when its letter arrives

1. Open/create the destination quest in Questlog's editor.
2. Open **Prerequisites**.
3. Add the **Mail Received** type (`questlog_envelope:mail_received`).
4. Leave **Letter quest marker** empty to use the current quest ID automatically, or select another Quest ID.
5. Leave the required amount at `1` for a normal quest letter.

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

The Envelope-specific editor works on Questlog's temporary reward entry, so Questlog's normal **Cancel** behavior remains available.

## Quest unlocked by receiving a letter

A quest can wait for a marked Envelope letter before Questlog considers it triggered:

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

The addon currently provides the first version of a postal reward:

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

When the reward is claimed, Envelope starts a real service delivery to the player's `PlayerAddress`. The `grants_quest` value is stored on the mail stack and is consumed by the `mail_received` prerequisite when the delivery reaches the player's mailbox.

Quest-letter markers are persisted in world `SavedData`. If the letter reaches the mailbox while the player is offline, the marker remains pending and is applied after Questlog finishes loading/deserializing that player's quests on the next login.

This enables narrative chains such as:

```text
Quest A complete
  -> Questlog reward
  -> Envelope letter is dispatched
  -> pigeon delivers it to the player
  -> quest marker is detected/persisted
  -> Quest B becomes triggered by Questlog
```

## Current status

Implemented on `dev/initial-integration`:

- multiloader project skeleton;
- `questlog_envelope:mail_received`;
- quest marker stored in vanilla `CUSTOM_DATA`;
- minimal mixin observing successful Envelope mailbox insertion;
- routing into the player's native Questlog prerequisites;
- persistence of unlocks received while the player is offline;
- replay of pending unlocks after Questlog player data is loaded;
- `questlog_envelope:letter` reward;
- `grants_quest` support;
- Questlog in-game editor integration for the compatibility objective/reward;
- dedicated Envelope letter options screen with English/French translations;
- Gradle CI definition.

Next steps:

- implement `questlog_envelope:mail_delivery` for sending mail to quest services;
- add package/content filters;
- add service replies and quest packages;
- add game/integration tests;
- investigate why GitHub Actions checks are not currently appearing on the private repository.
