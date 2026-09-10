# Testing QuestLog × Envelope Addon

The addon targets Minecraft 1.21.1 on both Fabric and NeoForge. A release candidate is ready only after the automated build/smoke checks pass and the in-game matrix below has been exercised on both loaders.

## Automated checks

Run:

```bash
./gradlew build --stacktrace
bash scripts/verify-build.sh
```

`verify-build.sh` collects the distributable Fabric and NeoForge jars into `dist/` and verifies that each jar contains the expected loader metadata plus the shared mixin, language, and magic-circle resources.

GitHub Actions runs the same checks for pull requests and pushes to `main` or `dev/**`, and retains the resulting jars as a workflow artifact.

## In-game regression matrix

Repeat this matrix once on Fabric and once on NeoForge.

| Area | Scenario | Expected result |
| --- | --- | --- |
| Startup | Launch client with Questlog, Envelope, and the addon | Game reaches title screen with no addon/mixin errors |
| World load | Open a fresh world and an existing world | World loads; addon SavedData initializes without errors |
| Quest editor | Open Questlog's editor | Mail Received, Mail Sent, Envelope Letter, and Envelope Package integrations are available with localized labels |
| Mail received | Deliver a marked quest letter while recipient is online | Matching objective/prerequisite progresses once |
| Mail received offline | Deliver marked quest mail while recipient is offline, then reconnect | Pending unlock replays once after Questlog state loads |
| Mail sent | Send matching mail from an Envelope mailbox using a pigeon | Matching Mail Sent objective increments once |
| Generated mail exclusion | Receive/send addon-generated service reward mail | It does not incorrectly count as player-sent mail |
| Recipient filter | Send mail to matching and non-matching recipients | Only the configured recipient progresses the objective |
| Letter text filter | Send letters with and without configured text | Only matching letter content progresses the objective |
| Package filter | Send packages below/at the configured item quantity | Only qualifying packages progress the objective |
| Letter reward | Claim a regular letter reward | Envelope delivers a readable letter and Questlog marks the reward claimed |
| Wax seal | Configure a built-in Envelope seal | Delivered letter uses the chosen seal and normal Envelope seal behavior |
| Rich text | Use bold/italic/underline/strikethrough/color formatting | Formatting survives save, delivery, and viewing |
| Quest grant | Deliver a letter/package with `grants_quest` | Target quest progresses/triggers exactly once |
| Magic circle cancel | Begin activation, then release/move away early | Activation cancels and no server action executes |
| Magic circle success | Hold for configured duration | Visual completes, chime/pulse occurs, configured action executes once |
| Magic circle dual action | Configure both quest and command actions | Both execute once after successful validation |
| Anti-replay | Duplicate/reuse a letter after activation | Consumed action ID cannot execute again |
| Recipient binding | Give a bound magic-circle letter to another player | Other player cannot execute the server-side action |
| Persistence | Restart server/world after activation and retry duplicate | Used action remains consumed after restart |
| Package pages | Configure multiple six-slot package pages | Correct physical packages and item contents are delivered |
| Mailbox delivery | Reward a player with a linked mailbox | Native/express Envelope delivery reaches mailbox as intended |
| No mailbox | Reward a player without a linked mailbox in Overworld | Service pigeon/direct delivery fallback completes without losing reward |
| Non-Overworld | Claim reward outside Overworld | Safe direct-drop fallback completes and reward does not remain stuck |
| Invalid sender | Configure an invalid/unavailable sender service | Normal Envelope mail-service sender fallback is used |
| Resource reload | Replace/reload `magic_circle.png` via resource pack | Tint-mask cache refreshes without game restart |
| Localization | Switch between English and French | Addon editor labels/help text render without missing translation keys |

## Release gate

Before tagging a release:

1. CI is green on the release commit.
2. The full matrix above has no release-blocking failures on Fabric or NeoForge.
3. `CHANGELOG.md` contains the release notes and `gradle.properties` no longer uses an unintended `-SNAPSHOT` version.
4. Verify the two jars in `dist/` install independently with the correct loader-specific dependencies.

Record any known non-blocking issue in the release notes rather than relying on memory.