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

### Smoke run — 2026-10-06

Test baseline: Minecraft 1.21.1, Java 21, Questlog 3.3.2, Envelope 0.7.5, Cloth Config 15.0.140; Fabric Loader 0.16.9 / Fabric API 0.107.0 and NeoForge 21.1.228. Ember and JEI were absent.

- Automated checks: 44 common tests passed; both loader builds and distributable-jar packaging checks passed. Nine tests specifically cover `mail_received` with native Questlog classes.
- Fabric: real client launch and world load succeeded. The tester reported no issues for the requested editor, letter, package, one-time signature/circle, reconnect/restart and mailbox/no-mailbox checklist. A small lag spike when five pigeons spawned together was reported; it has not been profiled or attributed to the addon.
- Fabric dedicated server: real startup, loading four smoke quests, `questlog reload`, `save-all flush`, graceful `stop` and restart of the saved world succeeded. Multiplayer gameplay remains pending.
- NeoForge: real client launch and world load succeeded. The tester reported the same gameplay checklist working normally, with the same lag spike at pigeon spawn. Dedicated startup, loading four smoke quests, `questlog reload`, `save-all flush`, graceful `stop` and restart of the saved world succeeded. Multiplayer gameplay remains pending.

This is a focused smoke pass, not a sign-off on every scenario below. Graphics variants, Ember-present coverage and the full release matrix remain separate checks.

The tester subsequently reported LAN and dedicated-server gameplay working. The prepared dedicated servers' connection logs did not show those sessions, so the dedicated gameplay report is recorded as tester feedback rather than independently confirmed coverage.

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
| Signature | Click Signature beneath circle settings, enter `Éléonore d’Arwen`, change ink and size, toggle frame | Preview uses cursive text and the delivered letter has the same appearance |
| Signature layout | Drag to all paper edges, resize via gold corner, move with Shift, reset | Signature stays on paper; snapping and reset work; long names fit |
| Signature coexistence | Deliver a framed signature with a wax seal and an actionable magic circle | All decorations remain present; circle activates normally |
| Signature persistence | Save, reopen editor, deliver, restart world, read letter | Signature text, color, size, compact frame and placement persist |
| Signature draft | Select signature/body and resize window/GUI scale before saving | Unsaved body, signature and magic-circle settings survive |
| Signature validation | Submit empty text, bad color or size outside 8–28; then cancel | Error displays and reward JSON stays unchanged |
| Signature disabled/legacy | Remove and save; read an older unsigned letter | Signature fields are removed; ordinary letters display as before |
| Player signature | Use `@s` or `Signed by @s`, reward two differently named players, pass a delivered letter to another reader | Each letter resolves its recipient's name once; quest template retains `@s`; later readers see the original name |
| Literal signature | Use `Arwen`, `@someone` or `mail@s.test` | Text is not substituted |
| One-click save | Change quest title/description and reward name/quest grant; edit letter and click Save Quest | Native quest save sends all current changes with the letter; no second Done or Save Quest needed |
| Save validation | Attempt Save Quest with invalid sender, circle settings or signature | Error displays, no quest save packet is sent; correcting inputs clears stale errors |
| Cancel | Edit/add/delete signature, then Cancel and reopen | Letter changes are discarded; other quest edits are preserved |
| Signature resources | Reload resources or replace signature font through a resource pack | Editor and received letters use the reloaded font |
| Signature writing | Hold a received letter and click its faint signature | Ink reveals left to right over 1.1 seconds; feather and drawing sound accompany it; signature becomes fully opaque |
| No repeat signing | Click repeatedly during writing, then after completion and reopening | No restart or stacked sounds; signed letter never starts writing again |
| Writing close | Close before the trace finishes, then reopen | Sound stops; signature remains faint and unsigned; no server commit was sent |
| Signed persistence | Sign, move/copy the letter, relog, restart world/server and reopen | Signed flag and full-opacity text persist; signing cannot repeat |
| Inventory race | Move/change held letter before the animation finishes | Different letter is not signed by a stale request |
| Offhand | Open and sign a letter from the offhand | Correct held letter gets the signed flag |
| Signing confirmation | Sign on a multiplayer server | Server inventory update confirms signing; temporary pending state prevents repeat clicks |
| Pixel font | View default signatures at GUI scales 1–4 and change size | Bitmap edges are sharp; normal-size atlas pixels align to screen pixels |
| Printed frame | View an unsigned framed signature, then start writing | Only signature text is faint; printed frame stays fully opaque throughout |
| Magical ink | Set Magic ink to purple, sign; repeat with blue on a different letter | After server confirmation, ink and frame fade to the chosen color; diagonal glint persists |
| Signing sparks | Sign once, wait, reopen the letter, restart the world | One sparkle burst on fresh signing; no repeated burst or feather when reopening; glint remains |
| Smooth feather | Sign short and long names at multiple frame rates | Feather travels continuously, eases at both ends and gently varies its tilt |
| Glyph integrity | Compare `Alex`, `test`, `Minecraft`, `gyp` at sizes 8, 12 and 28 | Pixel handwriting has continuous original strokes, no cropped ascenders/descenders |
| Exact glyphs | Type `azerty`, `0123456789`, `$`, `@s`; reload resources and reopen editor | Literal letters match input; no preceding-character substitution; `@s` resolves only as documented |
| Signature command | Configure `/say Signed by @s`, reward recipient, sign once | Server command executes once after signing and targets recipient; signing visual effects still play |
| Signature ownership | Give unsigned command-bearing letter to a different player | No command executes or action is consumed; recipient can still sign afterward |
| Signature duplicates | Copy unsigned command-bearing letter, sign original, sign copy, restart world | Command executes once total; copy becomes visually signed without replay |
| Signature item swap | Start signing, then exchange held letter for identically styled letter with a different signature command | Wrong command never executes; server checks action UUID as well as signature data |
| Command draft | Edit signature command, switch selection, resize, save/reopen; then Remove and save | Command draft persists; removing signature removes its command too |
| Circle magic | Activate a circle with different fill/magic colors, then reopen | Smooth fade, ink-colored diagonal shine, one radial particle burst; no replayed burst on reopening |
| Ember absent | Start each loader/client and a dedicated server without Ember | Normal letters work; no Ember preview button or missing-class errors |
| Ember present | Install Ember 3.x; type `<rainbow>Hello</rainbow>` and `<wave>Welcome</wave>`; toggle Ember preview | Preview animates and wraps within paper; disabling preview restores editable raw tags |
| Ember persistence | Preview tags with section-code formatting, resize GUI, save, deliver and reopen | Markup remains in stored source; received letter uses Ember effects and native Envelope interactions |
| Ember coexistence | Add signature, command and circle to an Ember-styled letter | All decorations, one-time actions and signing effects still work |
| Writing coexistence | Click a magic circle overlapping a signature | Circle controls retain priority and normal hold activation; no signature sound starts |
| Writing sound settings | Set master volume to zero, then restore it | Effect obeys normal Minecraft sound settings |
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
