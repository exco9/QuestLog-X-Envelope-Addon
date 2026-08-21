# QuestLog × Envelope Addon

Compatibility addon for **Questlog** and **Envelope** on Minecraft 1.21.1.

Current development target:

- Java 21
- Fabric
- NeoForge
- Questlog 3.3.2
- Envelope 0.7.5

See [`SYNTHESIS.md`](SYNTHESIS.md) for the architecture and analysis of both upstream projects.

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

This enables narrative chains such as:

```text
Quest A complete
  -> Questlog reward
  -> Envelope letter is dispatched
  -> pigeon delivers it to the player
  -> quest marker is detected
  -> Quest B becomes triggered by Questlog
```

## Current status

Implemented on `dev/initial-integration`:

- multiloader project skeleton;
- `questlog_envelope:mail_received`;
- quest marker stored in vanilla `CUSTOM_DATA`;
- minimal mixin observing successful Envelope mailbox insertion;
- routing into the player's native Questlog prerequisites;
- `questlog_envelope:letter` reward;
- `grants_quest` support;
- Gradle CI.

Next steps:

- persist quest-letter unlocks received while the player is offline;
- implement `questlog_envelope:mail_delivery` for sending mail to quest services;
- add package/content filters;
- add service replies and quest packages;
- add game/integration tests.
