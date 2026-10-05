![QuestLog × Envelope — quests delivered by mail](https://media.forgecdn.net/attachments/description/null/description_883335d8-a9df-4b5e-bf0b-a41ddd9135b7.png)

# QuestLog Envelope Addon [QLXE]

Turn **Questlog quests** into **Envelope letters and packages**. Deliver invitations, send supplies, and let players accept a contract with a magical signature.

**Minecraft 1.21.1 · Fabric & NeoForge · Java 21**

## What can you do?

![Quest letters, package rewards, magic circles and player signatures](FEATURE_IMAGE_URL)

- **Letters and packages:** deliver quest invitations, rewards and supplies through Envelope, with rich text, wax seals and multi-page packages.
- **Magic circles:** hold to unlock a quest or run a command, with configurable position, size, colors and enchanted shine.
- **Player signatures:** sign once with a name or `@s`, an optional frame and command, feather animation and writing sound.
- **Mail objectives:** track received quest mail or sent deliveries, with recipient, text and item filters.
- **Design letters in-game:** format text, place circles and signatures anywhere on the paper, then save the quest in one click.
- **Add animated text:** optional **Ember's Text API 3.x** support, with an editor preview.

Mail transport, mailboxes, pigeons and wax seals come from **Envelope**; quest progression comes from **Questlog**.

## Letter reward example

For example, this **letter reward** adds a framed signature and announces the signer:

```json
{
  "type": "questlog_envelope:letter",
  "title": "Guild contract",
  "text": "Sign below to accept the assignment.",
  "signature": "@s",
  "signature_frame": true,
  "signature_magic_color": "#AA55FF",
  "signature_command": "/say Signed by @s"
}
```

With Ember installed, try `<wave>Welcome to the guild!</wave>` in the letter body.

## Installation

Install **one** addon jar matching your loader, alongside **Questlog 3.3.2**, **Envelope 0.7.5**, and their dependencies for Minecraft 1.21.1. Add Ember's Text API 3.x if you want its text effects.

Licensed under **GPL-3.0**.
