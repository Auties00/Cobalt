# Cobalt Validation Orchestrator

You are the lead validation orchestrator for the Cobalt project.
Your job is to prove that Cobalt implements every WhatsApp Web / Desktop / Mobile feature **correctly**, both at the source level (behavioral parity with the WA Web JS source) and at the observable level (the Nodes, WAM events and HTTP that Cobalt produces for a given input must match what the real WhatsApp runtime produces for the same input).

The user invokes this command as: `/validate`.

## Preconditions

- Work from the Cobalt repository root.
- Preserve existing validation outputs under `validation/` across runs.
- NEVER run `mvn` yourself. The user uses their IDE for compile diagnostics. Agents never run `mvn compile` either — scratch-file compilation is the one exception the agent is allowed, and only via the IDE-agnostic mechanism documented in `validate-module`.

### Clean Working Tree (required)

`/validate` requires a clean working tree. Run `git status --porcelain`; if it
prints anything, STOP and tell the user to commit (or otherwise clear) their
changes first. This is a hard precondition, not a soft warning, and there is no
dirty-tree fallback.

The clean-tree rule is what makes incremental mode exact. A run starts from a
known commit and ends (Phase 4.5) by committing the fixes its agents applied, so
the next run can diff those two commits with neither false positives nor missed
changes. A dirty tree at either boundary would smear that delta.

### Verify MCP Server

Before doing anything else, verify the whatsapp MCP server is reachable:

1. Call any lightweight MCP tool such as `mcp__whatsapp__get_active_snapshot`.
2. If the server is NOT running, ask the user to start it. This is a hard precondition, not a soft warning.

### Determine Run Mode (incremental vs full)

Decide FULL or INCREMENTAL once, here, and record it as the first line of
`scope.md`.

Run INCREMENTAL when ALL of these hold:
- `validation/run-state.json` exists and its `schemaVersion` equals the current
  schema (1).
- Every `snapshots.<platform>.snapshotId` it records still appears in
  `list_snapshots(<platform>)`, so `get_revision_diff` can resolve its `from`.
- Its `cobaltCommit` is reachable: `git cat-file -e <cobaltCommit>^{commit}`
  succeeds.

Otherwise run FULL. The user may also pass `--full` to force FULL even when a
valid anchor exists; `--full` is the ONLY argument `/validate` accepts.

- FULL runs Phase 1 (whole-universe discovery, Steps 1.1-1.4) and seeds the
  anchor at Phase 4.5 so the next run can be incremental.
- INCREMENTAL runs Phase 1-INC (changed-set discovery), then Phases 2-4 scoped
  to the changed-set.

If you fall back to FULL because an anchor was present but unusable (snapshot
pruned, commit unreachable, schema bumped), say so in one line in `scope.md`.

### Verify Registered Emulators

Live captures are produced by pairing the web runtime against a real WhatsApp account running on an Android emulator. Validating the full API surface requires **both** account types, because a non-trivial subset of flows is business-gated (catalog, business-profile MEX, label ops, advertising) and would silently skip without a business peer.

Call `mcp__whatsapp__web_live_emulator_list` and confirm the result contains at least one emulator with `registrationState: "registered"` for each of `accountType: "personal"` and `accountType: "business"`. Record their names — agents will reference them when driving live flows.

If either is missing, notify the user which account type is missing and ask whether to provision it now. **Do not start provisioning without explicit confirmation** — registration burns a TextVerified rental, may require manual intervention on the confirmation-notice / 2FA / device-migration screens, and takes several minutes. Once the user confirms, run the chain:

1. `mcp__whatsapp__web_live_emulator_create` — creates the AVD
2. `mcp__whatsapp__web_live_emulator_start` — boots it
3. `mcp__whatsapp__web_live_emulator_install_apk` — installs the `personal` or `business` APK
4. `mcp__whatsapp__web_live_emulator_register_whatsapp` — drives the on-device registration flow

Poll `web_live_emulator_list` until both account types are `registered`, then continue.

### Verify Live Runtime

Call `mcp__whatsapp__web_live_status`. If `running=false`, call `mcp__whatsapp__web_live_start_session` and wait until `authState` reaches `logged_in`. Live captures require this session. **Do NOT stop the session between agents** — it's reused for the entire run. Only stop it at the very end, from Phase 4.

If login is needed, drive it yourself via the `web_live_login_with_phone_number` flow, not via an agent. Agents assume the session is already up.

### Regenerate Auto-Generatable Catalogs

Two Cobalt catalogs are derived from the live WA Web bundle and checked in as plain Java. Regenerate both before validation begins so per-module agents validate against the freshest source-of-truth, not a stale snapshot:

1. **WAM events and enums** → `com.github.auties00.cobalt.wam` (`wam/event/`, `wam/type/`):
   ```bash
   cd tools/web/wam-codegen && npm start
   ```
2. **AB props** → `com.github.auties00.cobalt.props`:
   ```bash
   cd tools/web/ab-props-codegen && npm start
   ```

Both tools are Playwright-based and may take a few minutes each. Run sequentially. If either fails, stop and surface the error to the user — do NOT continue against a stale catalog.

Once regenerated, the WAM and AB-props **definitions** (the catalog files themselves: `cobalt.wam.event/*Event.java`, `cobalt.wam.type/*.java`, `cobalt.props/ABProp.java`) are **guaranteed correct by codegen** and are NOT re-validated by Phase 3 per-module agents — the WA modules that produced them are SKIPPED (see SKIPPED criteria 10–11 below).

**Their *usages* are NOT guaranteed correct.** When a per-module agent validates a handwritten module that emits a WAM event (`wamService.commit(SomeEvent...)`) or reads an AB prop (`ABProp.X.value()`), the agent must still verify that:
- the right event class / prop key is referenced,
- it's invoked at the equivalent code path WA Web uses,
- the field values populated on the event match WA Web,
- the prop's default and read-path semantics match.

Codegen only proves the catalog *shapes* are right. Whether Cobalt *uses* them correctly is part of normal per-module validation for the consumer.

SMAX (`cobalt.stanza.smax`), MEX (`cobalt.stanza.mex`), legacy IQ (`cobalt.stanza.iq`), and USync (`cobalt.stanza.usync`) are NOT auto-generatable — they are handwritten and go through normal Phase 3 validation.

## Output Layout

```
validation/
  scope.md                 # Phase 1 scope report (for the user to review)
  manifest.json            # Whole-universe module-to-owner mapping
  plan.md                  # Topologically ordered agent list
  run-state.json           # Incremental-mode anchor: per-platform snapshots + fixes commit + per-module verdicts (git-ignored)
  reports/<Module>.md      # Per-module reports — outcome is VALIDATED or SKIPPED
  captures/<Module>/       # Live captures + Cobalt captures + diffs (persist across runs)
    live.json              # Captured real WA Web stanzas / WAM / HTTP
    cobalt.json            # Captured Cobalt output from the scratch file
    session.json           # Seed data (keys, device) pulled from the live session
    input.json             # Input used for both sides
    diff.md                # Rendered diff + verdict
  feature-tree.md          # Phase 3.11.1 feature taxonomy
  features/<F>/<S>/        # Phase 3.11 per-feature artefacts
    scenario.json          #   inputs + preconditions + expected observables
    live.json              #   live-WA-Web capture
    cobalt.json            #   Cobalt capture
    diff.md                #   diff + verdict
    debug.md               #   root-cause + fix (only if FUNCTIONAL_FIXED)
  feature-summary.md       # Phase 3.11.7 index of (sub-)feature verdicts
  flow-report.md           # Phase 3.12 cross-cutting issues
  phantom-report.md        # Phase 3.13 dead-code sweep
  report.md                # Final synthesis
```

`validation/captures/` MUST persist across runs. Agents read existing captures and reuse them; they only re-drive the live runtime when a capture is missing or the user has explicitly invalidated it.

The entire `validation/` tree is git-ignored; it is local run data (including the `run-state.json` anchor), not committed artefacts.

---

## Phase 1: Whole-Universe Discovery (You Do This Inline)

This phase runs in FULL mode. In INCREMENTAL mode, skip Step 1.1's enumeration
and run Phase 1-INC (below) instead. Phase 1-INC still reuses Step 1.2 (owner
mapping) and Step 1.3 (dependency graph), because the reverse-dependency closure
needs the full graph even when only a handful of modules changed.

### Step 1.1: Enumerate the WA Module Universe

Call `mcp__whatsapp__list_modules` with no `platform` argument. The tool returns every module from every loaded catalog (web + desktop_windows + desktop_macos + ios) in one shot. Each entry carries `{name, platform, sourceBytes, exports[], dependencies[], sourcePath}`.

Do NOT read `manifest.json` files from disk — the MCP tool is the source of truth and stays in sync with the active snapshot. Do NOT use `search_modules` for enumeration — it's fuzzy and capped.

The result is a de-duplicated set of `{platform, name, dependencies, exports, sourceBytes}` tuples. Expect ~10,000 modules. This is the raw universe — every module gets a manifest entry. **No upfront filtering.** Each per-module agent decides for itself in Phase 3 whether to validate or skip, using the skip criteria embedded in its prompt.

### Step 1.2: Map WA Modules to Cobalt Owners

For each WA module, find its Cobalt owner(s):

1. `grep -rn '@WhatsAppWebModule(moduleName = "<name>"' modules/lib/src/main/java/` to find exact annotations. This is the authoritative mapping.
2. If no match, `grep -rn '@WhatsAppWebExport(moduleName = "<name>"' modules/lib/src/main/java/` — some modules are split: the class isn't annotated but individual methods are.
3. If still no match, the module is UNCLAIMED. Keep it in the plan as a candidate for `MISSING_IN_COBALT` — the per-module agent will confirm or mark SKIPPED if Cobalt has no counterpart by design.

For each Cobalt file, the reverse mapping (Cobalt → WA modules) is already expressed by its annotations. Reverse-index it: for every Java file under `modules/lib/src/main/java/`, collect the `@WhatsAppWebModule`/`@WhatsAppWebExport` annotations it carries.

### Step 1.3: Build the Dependency Graph

Using the `dependencies` arrays from Step 1.1, construct a directed graph over the universe. Edges go `module -> dep`. For every Cobalt file, also read its `import com.github.auties00.cobalt.*` statements and translate into WA-module edges via the mapping from Step 1.2 — this catches cases where Cobalt's internal dep graph diverges from WA Web's and the divergence is itself the thing under test.

Compute strongly connected components (SCCs). Topologically sort. Leaves first, top-level consumers last.

### Step 1.4: Write `validation/scope.md` and Wait for User Confirmation

Present:
- Total WA modules found per platform (counts).
- Total Cobalt files claimed vs unclaimed (counts).
- Orphan Cobalt files (claim a WA module that does not exist in the snapshot) — list them for user review.
- The first and last 20 entries of the topological order.

Note in scope.md that there is **no upfront skip list**. Each per-module agent in Phase 3 reads its module's source and decides one of two outcomes:
- `VALIDATED` — Cobalt has a counterpart; per-module deep validation runs.
- `SKIPPED` — module is irrelevant to a headless Java client (UI / browser-only / vendored / generated / locale data / platform-specific shell). Agent records the reason in one line.

Ask the user to confirm or to amend the skip criteria in the agent prompt. Do NOT proceed to Phase 2 until they say go.

### Phase 1-INC: Incremental Discovery (INCREMENTAL mode only)

Replaces Step 1.1. Computes the changed-set: every module whose validation could
have been invalidated since the recorded anchor. Everything outside the
changed-set inherits its prior verdict from `run-state.json`.

You MAY use `.claude/scripts/diff-scope.mjs` for the closure and topo math in
steps 6 and 8: pass it `{ "graph": { "<module>": ["<dep>", ...] }, "seed":
["<module>", ...] }` (stdin or a file path arg) and it returns
`{ "closure": [...], "topoOrder": [...], "cyclesDetected": bool }`.

1. **Build the graph.** Run `list_modules` (no platform) and perform Steps 1.2
   and 1.3 exactly as in FULL mode. This yields the dependency graph and the
   Cobalt owner reverse-index. No agents are spawned for the universe.

2. **WA-source delta.** For each platform p in {web, desktop_windows,
   desktop_macos, ios}, read `active = get_active_snapshot(p)`. If
   `active.snapshotId == run-state.snapshots[p].snapshotId` the WA delta for p is
   empty; otherwise call `get_revision_diff(fromSnapshotId =
   run-state.snapshots[p].snapshotId, toSnapshotId = active.snapshotId, platform
   = p)` and collect `addedModules` + `removedModules` + the `module` of each
   `changedModules` entry. Keep each entry's `sourceChanged` flag for step 7.

3. **Cobalt-code delta.** The tree is clean (precondition) and the prior run's
   fixes were committed at its Phase 4.5, so the anchor commit IS the
   last-validated Cobalt state. Compute both:
   - `git diff --name-status <run-state.cobaltCommit> -- modules/lib/src/main/java`
     (modified, added, renamed, deleted tracked owner files), and
   - `git ls-files --others --exclude-standard -- modules/lib/src/main/java`
     (new untracked owner files that `git diff` does not show).
   Map each path to its WA module(s) via the Step 1.2 reverse-index. For `D` and
   `R` entries, also include the module the old path used to claim. This delta is
   exact: no false positives, nothing missed.

4. **Carry-forward.** Add every `run-state.verdicts` entry with `open == true`
   (a prior MISMATCH, MISSING_IN_COBALT, LIVE_MISMATCH, or FUNCTIONAL_BLOCKED the
   last run could not close), plus any module in the current universe that is
   absent from `run-state.verdicts` (never validated).

5. **seed-set** = (WA delta) + (Cobalt delta) + (carry-forward).

6. **changed-set** = the reverse-dependency closure of seed-set over the Step 1.3
   graph: starting from seed-set, repeatedly add any module that depends
   (directly or transitively) on a module already in the set. This catches a
   consumer whose own source did not change but whose dependency's export shape
   did, the same invalidation Phase 4.2 handles in FULL mode.

7. **Invalidate stale captures.** For each module in the seed-set, under
   `validation/captures/<Module>/` delete `cobalt.json` (Cobalt code may have
   moved) and, when its WA `sourceChanged` flag is set, `live.json` (runtime
   behaviour moved). Leave `input.json` and `session.json`. Modules pulled in
   only by closure with no WA or Cobalt change of their own keep their captures.

8. **Topologically sort** the changed-set subgraph (leaves first), exactly as
   Step 1.3 sorts the universe.

9. **Write scope.md and gate.** Present: run mode INCREMENTAL; per-platform
   `from -> to` snapshot ids and revisions; the anchor `cobaltCommit`; the sizes
   of the WA delta, Cobalt delta, carry-forward, and the final changed-set after
   closure; and the topological order of the changed-set. Apply the same
   no-upfront-skip-list note as Step 1.4, then ask the user to confirm. Do NOT
   proceed to Phase 2 until they say go.

   If the changed-set is empty (no snapshot moved, no owner file changed, no open
   verdict), write scope.md stating the validation is up to date against the
   anchor and STOP. There is nothing to validate, and run-state.json is already
   current, so do not rewrite it.

---

## Phase 2: Manifest Building

In INCREMENTAL mode, "every kept WA module" below means every module in the
changed-set, not the whole universe. Build fresh manifest entries for those and
merge them over the prior `validation/manifest.json`, so untouched modules keep
their existing entries and `validationOrderIndex`. In FULL mode, build the entire
manifest as usual.

### Step 2.1: For every kept WA module, enumerate exports

1. `mcp__whatsapp__get_exports` → list of `{exportName, symbolName, bindingKind}`.
2. For each export, record the Cobalt method mapped to it (via `@WhatsAppWebExport`) or mark as `unmapped`.

### Step 2.2: Classify the module by observable side effect

This governs whether the agent runs a live-capture pass. For each module, determine its side-effect class by inspecting its exports' names and (if ambiguous) a quick peek at `mcp__whatsapp__get_symbol_source`:

- **STANZA**: produces or consumes `<message>`, `<iq>`, `<presence>`, `<receipt>`, `<ack>`, `<notification>`, `<chatstate>` stanzas.
- **WAM**: emits WAM telemetry (name contains `Wam`, or exports include constructors like `WamXxx`).
- **HTTP**: issues HTTP (`fetch`, `xhr`, MEX GraphQL, media upload/download to `mmg.whatsapp.net`).
- **STATE**: mutates persisted state (store collections, IndexedDB writes) with no network effect of its own.
- **PURE**: pure functions (crypto primitives, encoders, validators, string formatters) — no side effect.

A module may have multiple classes (a sender typically produces STANZA + WAM). Record the set.

PURE modules skip the live-capture pass entirely — static parity is sufficient.

### Step 2.3: Write `validation/manifest.json`

```json
{
  "timestamp": "<ISO>",
  "totalModules": N,
  "modules": [
    {
      "waModule": "WAWebSendMessageJob",
      "platform": "web",
      "cobaltFiles": ["modules/lib/src/main/java/.../UserMessageSender.java"],
      "sideEffects": ["STANZA", "WAM"],
      "exports": [
        { "exportName": "sendMessage", "cobaltMethod": "UserMessageSender.java#send", "status": "pending" }
      ],
      "unmappedCobaltMethods": [],
      "validationOrderIndex": 127
    }
  ]
}
```

### Step 2.4: Write `validation/plan.md`

One line per module in validation order, plus coverage counts and the layer breakdown (how many leaves, how many intermediate, how many consumers).

---

## Phase 3: Strict Sequential Validation

**Rule — no parallelism.** Spawn exactly one `validate-module` agent. Wait for it to finish. Spawn the next. No `run_in_background`, no worktrees. Every agent operates on the live codebase and sees every fix from every previous agent.

In INCREMENTAL mode, "each module" below means each module in the changed-set,
walked in the changed-set's own topological order. Modules outside the
changed-set are never spawned; their prior `validation/reports/<Module>.md` and
verdicts stand unchanged. In FULL mode, walk the whole universe.

For each module in topological order:

1. Open the manifest entry.
2. Spawn `validate-module` with the prompt below.
3. Wait for completion.
4. Read `validation/reports/<Module>.md`.
5. If the report declares MISMATCH or MISSING and the agent applied fixes, record the fact but do not re-compile (user runs the IDE). Move on.
6. If the agent declares LIVE_MISMATCH that it could not fix in this pass (e.g. needs a change in a not-yet-validated consumer), record it for Phase 3.12 (cross-cutting flow).
7. Continue until the last module.

### Prompt Template (exact fields the agent receives)

```
Validate WA module `{waModule}` against Cobalt.

## WA Module
`{waModule}` (platform: {platform})

## Side-Effect Classes
{STANZA|WAM|HTTP|STATE|PURE, comma-separated}

## Exports to Validate
{each export with its mapped Cobalt method or `unmapped`}

## Owned Files (may edit)
{paths}

## Context Files (read-only)
{paths}

## Unmapped Cobalt Methods (candidates for MISSING_IN_WA_WEB)
{list}

## Capture Directory
validation/captures/{waModule}/

## Report Output Path
validation/reports/{waModule}.md

## Decide the module's outcome BEFORE validating

Read the module source. Pick exactly one of two outcomes and write it as the
first line of the report:

### A. SKIPPED — Cobalt is a headless Java client; this module has no counterpart

Mark SKIPPED if any of these apply:

1. **UI rendering** — exports a JSX element / React component, name contains
   `Component`, `Jsx`, `Icon`, `Emoji`, `Modal`, `Dialog`, `Drawer`, `Tooltip`,
   `Popover`, `Carousel`, `Animation`, `Theme`, `Stylesheet`, ends with `View` /
   `Viewer` / `Renderer` / `Plugin`.
2. **Browser-only runtime** — touches `window` / `document` / `navigator` /
   `localStorage`, IndexedDB schemas, web/service workers, Comlink/portal
   bridges, Lexical editor.
3. **Vendored third-party** — name starts with `WAWeb-` (hyphen), or matches
   Lottie / easel.js / similar bundled libraries.
4. **Generated artifact** — `.graphql` files, `*_facebookRelayOperation` stubs,
   `WAProto*.pb` (Cobalt regenerates these from `tools/web/proto-extractor/`).
5. **Lazy-load entry** — name ends with `Loadable`; module body is a thin
   `requireDeferred` wrapper.
6. **Locale data** — `WAWebLocalesEmojiSuggestion*`, `WAWebCountriesLocale*`,
   `WAWebLexicon*`, `WAWebFbt*`, `WAWebEphemeralL10N*`. Per-language data tables
   with no logic.
7. **Platform-specific desktop shell** — `WAWebWindowsHybridBridge*`,
   `WAWebElectron*`, `WAWebMacOs*`. Cobalt is headless.
8. **UI state** — routers, navigation, theme, keyboard input, file-saver,
   clipboard, wallpaper, transitions, NUX onboarding, app rating.
9. **Frontend infra** — `WAWebUse*` React hooks, `WAWebGet*` Backbone-getters,
   `WAWebDom*` DOM helpers, `WAWebBroker*` browser brokers, `WAWebTP*` /
   `WAWebTP3P*` third-party-bridge wrappers, `WAWebCanonical*` browser-recovery,
   `WAWebStorage*` browser-storage manager, `WAWebApi*` / `WAWebCmd*`
   frontend-bridge, `*Mutator` Backbone mutators, `*Loadable` lazy entries.
10. **WAM (codegen-guaranteed)** — `WAWebWam*` / `*WamEvent` / `WAWebWamEnum*`.
    The counterparts under `cobalt.wam.event/` and `cobalt.wam.type/` are
    regenerated by `web-wam-codegen` in the prerequisites step and are
    guaranteed correct by codegen — do not re-validate. Reason line:
    "codegen-guaranteed (web-wam-codegen)".
11. **AB-props (codegen-guaranteed)** — `WAWebAbProps*` / `WAWebFeatureGate*` /
    `WAWebCapability*`. The counterparts under `cobalt.props/` are regenerated
    by `web-ab-props-codegen` in the prerequisites step and are guaranteed
    correct by codegen — do not re-validate. Reason line:
    "codegen-guaranteed (web-ab-props-codegen)".

If you decide SKIPPED, write a one-line reason and stop. Do NOT validate exports.

### B. VALIDATED — Cobalt has a handwritten counterpart; do the full pass

This is the default outcome for every module that isn't SKIPPED. Examples of
catalog families that go through normal per-module validation here:

- `WASmax*` — handwritten Java under `cobalt.stanza.smax/`.
- `WAWebMex*` — handwritten Java under `cobalt.stanza.mex/`.
- `WADeprecatedSendIq` and consumers — handwritten Java under `cobalt.stanza.iq/`.
- `WAWebUsync*` — handwritten Java under `cobalt.stanza.usync/`.

For every export, produce one of these statuses:
- MATCH (DIRECT) — Cobalt's logic is byte-equivalent to WA Web's.
- MATCH (ADAPTED) — Cobalt intentionally diverges per CLAUDE.md (sealed
  exception hierarchy, flat store, virtual-thread blocking, nullable-Boolean
  coalescing, per-mutation handler interface, dropped frontend bridge).
- MISMATCH — fix the Cobalt owner; re-verify; record the fix.
- MISSING_IN_COBALT — implement the missing piece in Cobalt.
- LIVE_MATCH / LIVE_MISMATCH — observable parity verdict (see below).
- SKIPPED_PURE — pure module; static parity is sufficient.

For every export whose side-effect class is non-PURE:
  1. Ensure a live capture exists under validation/captures/{waModule}/live.json
     — if not, drive the live runtime to produce one and save it. NO TIMEOUTS on live calls.
  2. Write a scratch validation file under
     modules/lib/src/test/java/<matching-cobalt-package>/{waModule}Validate.java
     that invokes the Cobalt owner with the same input and captures what Cobalt emits
     (Nodes via an overridden WhatsAppClient.sendNode, WAM via the wamService, HTTP via …).
  3. Compile and run the scratch file. Diff captured-Cobalt against captured-live.
  4. Any non-zero structural diff → fix the Cobalt owner, re-run the scratch file, repeat.
  5. Write validation/captures/{waModule}/diff.md with the final diff (should be empty on success).

For PURE modules, static parity alone is sufficient. Skip steps 1-5.

## Looks-skippable but is NOT — DO NOT skip these

- `WAWebHandle*` — almost always stanza handlers (`<message>`, `<receipt>`,
  `<notification>`, `<presence>`, `<failure>`, `<stream:error>`). Validate.
- `WAWebChatDialogState`, `WAWebMessageButton*`, `WAWebInteractive*Header`,
  `WAWebList*Action` — protocol-level despite UI-flavoured tokens.
- `WAWebWidFactory`, `WAWebJid*`, `WAWebWid*`, `WAParsableWapNode`,
  `WAParsableXmlNode` — JID/WID/WAP protocol primitives.
- Modules with very few exports (1–3) but >2KB source — usually behavior
  modules, not pure helpers.
- `WAWebSyncd*` infrastructure (CollectionHandler, AntiTampering, Bootstrap,
  ResponseParser) — sync framework, not auto-gen.
- `WAWebHistory*` — history sync orchestration; validate.
- `WAWebLid*`, `*LidMigration*`, `*LidPnMapping*` — LID/PN protocol logic.
- `WAWebE2EProto*` — handwritten proto serialization; validate.
- `WAWebUsync*` — handwritten in Cobalt under `stanza/usync/`. Validate the
  per-module Java parity directly. (USync is NOT auto-generated despite being
  a typed-stanza family — its 11 protocols are stable enough to hand-write.)

## General rules

Exhaustive static parity is always required for VALIDATED modules: every export
must have a verdict.  Leaves must be complete against WA Web, not just against
current consumer needs. Fix all issues in owned files. Report issues in context
files without editing. Write the report.
```

---

## Phase 3.11: Feature-Level Functional Validation

Phase 3 proves **structural** parity (shapes, schemas, per-module exports). It
does **not** prove **functional** parity — that an end-to-end user flow
produces the same observable outcome in Cobalt as in WhatsApp Web. A poll can
pass per-module validation, every WAM event class can match, and yet the "vote
on poll" flow can still be broken because Cobalt's poll-update receiver
forgets to refresh the chat list.

This phase closes that gap.

In INCREMENTAL mode, run this phase only for features whose module group
intersects the changed-set; every other (sub-)feature inherits its prior verdict
from `feature-summary.md`, and existing `live.json` captures with unchanged
scenario inputs are reused. In FULL mode, run the whole feature tree.

### Step 3.11.1: Group VALIDATED modules into a feature taxonomy

Spawn one `validate-feature-grouper` agent. It reads every Phase 3 report whose
outcome is `VALIDATED` and groups the modules into a feature tree: `Feature →
Sub-feature → Modules`. Example:

```
Messaging
├── Send text message       (UserMessageSender, ChatFanoutStanza, ...)
├── Edit message            (MessageEditAction, ProcessEditProtocolMsgs, ...)
├── Delete for me           (DeleteMessageForMeHandler, ...)
├── Delete for everyone     (RevokeMsgAction, ...)
├── React to message        (ReactionSender, ReactionHandler, ...)
└── Forward message         (ForwardMessage, ...)

Newsletter
├── Create newsletter
├── Send newsletter message (NewsletterMessageSender, ...)
├── Update newsletter
└── Follow / unfollow

Groups
├── Create group
├── Add participants
├── Promote / demote admin
└── ...

Status
├── Post status
└── Reply to status

Polls
├── Send poll
└── Vote on poll
```

Output: `validation/feature-tree.md`. The user reviews and either confirms or
edits the grouping before Step 3.11.2 runs.

### Step 3.11.2: Define behavioral scenarios per (sub-)feature

For each leaf in the feature tree, the agent writes a scenario file under
`validation/features/<Feature>/<Sub>/scenario.json` that describes:

- **Inputs**: the user-level operation (e.g., "send text 'hello' to chat XYZ").
- **Preconditions**: required session state (logged in, peer registered, etc.).
- **Expected observables**: the set of stanzas, WAM events, HTTP calls, and
  store mutations that constitute "this feature working."

These scenarios are derived by reading WA Web's source for the feature path,
not invented from spec. They reflect what WA Web *actually* does today.

### Step 3.11.3: Drive the scenario in WhatsApp Web (live)

Use the live emulator and `mcp__whatsapp__web_live_*` tools. Capture every
outbound stanza, WAM commit, and HTTP request the live runtime produces. Save
to `validation/features/<Feature>/<Sub>/live.json`.

If a capture already exists from a prior run and the scenario inputs are
unchanged, **reuse it**. Captures persist across runs.

### Step 3.11.4: Drive the same scenario in Cobalt

Spawn a scratch driver under
`modules/lib/src/test/java/feature/<Feature><Sub>FeatureValidate.java` that
exercises Cobalt at the same granularity (same inputs, same preconditions).
Override `WhatsAppClient.sendNode` to capture stanzas, override `wamService` to
capture WAM events, intercept `HttpClient` to capture HTTP. Save to
`validation/features/<Feature>/<Sub>/cobalt.json`.

### Step 3.11.5: Diff and verdict

Diff `live.json` against `cobalt.json`. Three outcomes per (sub-)feature:

- `FUNCTIONAL_PARITY` — captures match (modulo CLAUDE.md-documented adaptations).
- `FUNCTIONAL_DIVERGENCE` — captures differ; root cause not yet identified.
- `FUNCTIONAL_FIXED` — captures differed, agent traced the divergence to a
  Cobalt code path, applied a fix, and re-ran to confirm parity.

### Step 3.11.6: Debug Cobalt against WhatsApp Web

For every `FUNCTIONAL_DIVERGENCE`, spawn one debug agent per feature. Its
contract:

1. Open the diff. Identify the first observable that diverged (missing stanza,
   wrong attribute, missing WAM event, missing store mutation).
2. Read the WA Web source path that produced the missing/divergent observable —
   the agent must locate the actual JS code that emits it, not guess.
3. Trace through Cobalt's equivalent code path. The agent reads Cobalt's owner
   classes for every module Phase 3 grouped into this feature.
4. Identify the divergence point. Categories include:
   - missing call site in Cobalt (e.g., a WAM commit, a follow-up stanza)
   - wrong order of operations (Cobalt computes Y before X but WA Web does X first)
   - wrong condition (Cobalt's `if` predicate differs)
   - missing observer / listener registration
   - wrong store accessor (Cobalt reads from a different field)
5. Apply the fix in the Cobalt owner. Re-drive Cobalt and re-diff. If the diff
   shrinks but is non-empty, repeat from step 1.
6. When the diff is empty, mark the feature `FUNCTIONAL_FIXED` and record the
   root cause in `validation/features/<Feature>/<Sub>/debug.md`.

If the agent cannot resolve the divergence (e.g., needs a change in a
not-yet-validated dependency), it emits `FUNCTIONAL_BLOCKED` with the blocking
reason. The orchestrator records these for Phase 3.12 (cross-cutting flow) to
pick up.

### Step 3.11.7: Write the feature index

Output: `validation/feature-summary.md` — one row per (sub-)feature with its
verdict and (for FIXED) a one-line root-cause summary.

---

## Phase 3.12: Cross-Cutting Flow Validation

After every Phase 3 module has a verdict, spawn a single `validate-flow` agent
for cross-file issues (delegation misses, type mismatches across module
boundaries, per-call vs batched patterns). The agent reads every Phase 3
report's "Issues in Context Files" section plus any deferred LIVE_MISMATCH
carried forward.

Output: `validation/flow-report.md`.

This phase runs fully in both FULL and INCREMENTAL mode: it is a single agent and
cheap relative to Phase 3, and cross-file issues can surface from any pairing of
changed and inherited modules.

## Phase 3.13: Phantom Code Sweep

After cross-cutting flow validation, spawn `validate-phantom` to remove dead
code. Same contract as before. Output: `validation/phantom-report.md`.

This phase runs fully in both modes; dead-code detection is whole-codebase by
definition.

---

## Phase 4: Synthesis

### Step 4.1: Completeness Check

For every WA module in the universe, confirm its manifest entry has a Phase 3
outcome (one of `VALIDATED`, `SKIPPED`). For every VALIDATED export, confirm
its status is one of MATCH, MISMATCH (fixed), MISSING_IN_COBALT (implemented),
ADAPTED, LIVE_MATCH, LIVE_MISMATCH (fixed), or SKIPPED_PURE.

For every (sub-)feature in `feature-tree.md`, confirm a row in
`feature-summary.md` with a verdict in {`FUNCTIONAL_PARITY`,
`FUNCTIONAL_DIVERGENCE`, `FUNCTIONAL_FIXED`, `FUNCTIONAL_BLOCKED`}.

In INCREMENTAL mode, a module outside the changed-set takes the verdict inherited
from `run-state.json`. Confirm every universe module has either a fresh verdict
(changed-set) or an inherited one, with none missing.

### Step 4.2: Re-validation Pass

If any agent applied fixes, re-run Phase 3 from the first module that depended (directly or transitively) on a fixed leaf. This converges quickly because dependency order already got most issues in the first pass. In INCREMENTAL mode this re-validation stays within the changed-set: a fixed leaf there may force re-running its changed-set consumers, but inherited modules are not reopened.

### Step 4.3: Stop the Live Session

`mcp__whatsapp__web_live_stop_session`. Captures are preserved.

### Step 4.4: Write `validation/report.md`

```markdown
# Cobalt Validation Report

## Summary
- Modules in universe: N (web: X, desktop_windows: Y, desktop_macos: Z, ios: W)
- VALIDATED: N (per-module deep validation completed)
- SKIPPED:   N (irrelevant to a headless Java client, or covered by codegen — see breakdown)
- MATCH (DIRECT):     N
- MATCH (ADAPTED):    N
- MISMATCH (fixed):   N
- MISSING_IN_COBALT (implemented): N
- LIVE_MATCH:    N
- LIVE_MISMATCH (fixed): N
- SKIPPED_PURE:  N

## SKIPPED Breakdown
- UI / browser / vendored / generated / locale / platform-specific shell: N
- Codegen-guaranteed (WAM / AB-props, regenerated in prerequisites): N

## Feature-Level Functional Outcomes
| Feature | Sub-Feature | Modules | Verdict | Root cause (if FIXED) |
|---------|-------------|---------|---------|------------------------|
- FUNCTIONAL_PARITY:    N
- FUNCTIONAL_FIXED:     N (root causes summarised inline)
- FUNCTIONAL_BLOCKED:   N (carried into Phase 3.12 cross-cutting flow)

## Observable Parity
Per-side-effect-class outcome over the VALIDATED set: how many STANZA modules reached live parity, how many WAM, etc.

## Issues Fixed
Grouped by module, with category + one-sentence fix description.

## Remaining ADAPTED Items
Modules where Cobalt intentionally diverges, with reason.

## Per-Module Table (VALIDATED only)
| Module | SideEffects | Exports | MATCH | MISMATCH | MISSING_COBALT | LIVE_MATCH | LIVE_MISMATCH | ADAPTED |
|--------|-------------|---------|-------|----------|----------------|------------|---------------|---------|
```

### Step 4.5: Commit Fixes and Write `validation/run-state.json`

This finalizes the anchor for the next run and runs in BOTH modes.

1. **Commit the fixes.** Agents applied fixes during Phases 3, 3.11, 3.12, and
   3.13, so the tree is now dirty under `modules/`. Stage and commit them as a
   single commit (do NOT stage `validation/`, which is git-ignored):
   ```bash
   git add -- modules/
   git commit -m "validation fixes"
   ```
   If nothing changed, skip the commit; the existing HEAD is already the
   validated state. This end-of-run commit is a deliberate, command-scoped
   exception to the usual "commit only when asked" rule; it is what lets the next
   run diff exactly.

2. **Record the anchor.** Write `validation/run-state.json`:
   - `schemaVersion`: 1
   - `completedAt`: now, ISO 8601
   - `cobaltCommit`: `git rev-parse HEAD` (the commit from step 1, or the
     unchanged HEAD if nothing was committed)
   - `snapshots`: the `{snapshotId, revision}` from `get_active_snapshot(p)` for
     each platform p
   - `verdicts`: one entry per WA module, `{outcome, open, reason?}`. In FULL
     mode this covers the whole universe; in INCREMENTAL mode merge the fresh
     changed-set verdicts over the inherited ones. `open` is `true` for any
     module the run left unresolved (an unfixed MISMATCH, MISSING_IN_COBALT, or
     LIVE_MISMATCH, or a FUNCTIONAL_BLOCKED feature touching it).

```json
{
  "schemaVersion": 1,
  "completedAt": "<ISO>",
  "cobaltCommit": "<sha>",
  "snapshots": {
    "web":             { "snapshotId": "...", "revision": "..." },
    "desktop_windows": { "snapshotId": "...", "revision": "..." },
    "desktop_macos":   { "snapshotId": "...", "revision": "..." },
    "ios":             { "snapshotId": "...", "revision": "..." }
  },
  "verdicts": {
    "WAWebSendMessageJob": { "outcome": "VALIDATED", "open": false }
  }
}
```

---

## Rules

- **Auto mode, one optional flag.** With no argument the command auto-selects INCREMENTAL when a valid `validation/run-state.json` anchor exists and FULL otherwise (see "Determine Run Mode"). The ONLY accepted argument is `--full`, which forces a whole-universe pass. Do not accept feature-scoped or module-scoped arguments.
- **Clean tree in, committed fixes out.** The command aborts on a dirty working tree and ends (Phase 4.5) by committing the fixes its agents applied. That end-of-run commit is the one sanctioned exception to "commit only when asked" and is required for incremental diffing to stay exact.
- **Strict sequential.** One agent at a time, full stop. The 5-hour usage ceiling makes parallel waves impossible.
- **No mvn by the orchestrator.** The user's IDE owns compilation diagnostics. Agents may compile their own scratch file, nothing else.
- **No live-run timeouts.** Live MCP calls wait as long as they need. The orchestrator gates on agent completion, not on wall-clock.
- **Captures persist.** `validation/captures/<Module>/` survives across runs. Agents reuse existing captures.
- **Exhaustiveness.** Every kept WA module has a verdict. Every export has a status.
- **Live session is shared.** Start once in Phase 1, reuse for every agent, stop in Phase 4.3.
- **Skip transparency.** Every per-module agent records its outcome (`VALIDATED` / `SKIPPED`) on the first line of the report, with a one-line reason for SKIPPED. There is no upfront `skip-list.json`.
- **Topological order is mandatory.** Leaves first. Every leaf must be complete against WA Web, regardless of current consumer needs.
- **Every issue must be fixed, not only reported.**
- **No orchestrator-side shortcuts.** The orchestrator (you) MUST NOT auto-skip modules by writing reports directly, MUST NOT bulk-classify modules by name pattern without an agent, MUST NOT pre-filter the universe by guessing which modules are "obviously" UI / generated / locale / vendored / codegen-guaranteed — these decisions belong to the per-module `validate-module` agent. Every module in the topological plan goes through exactly one `validate-module` agent invocation, end of story. The agent's first action is to read the module source via the MCP and decide `VALIDATED` / `SKIPPED` with a reason — that decision is the agent's, not the orchestrator's. Even when the module name screams "obvious skip" (e.g. `*.pb`, `*.graphql`, `*Loadable`, `WAWebLocale*`, `WAWebFbt*`, `WAWebWam*`, `WAWebAbProps*`), still spawn the agent. The cost of a 30-second agent confirming the obvious is dramatically less than the cost of a misclassified module silently sliding through. Auto-skip heuristics are a footgun that lose information; do not introduce them under any pretext (throughput, context budget, "optimization"). If the orchestrator is tempted to write a SKIPPED report itself, stop — that is a guard violation; spawn the agent instead.

  The INCREMENTAL changed-set is NOT a violation of this rule and is NOT an auto-skip. It is a tool-computed delta (snapshot `get_revision_diff` plus `git diff` against the anchor commit plus reverse-dependency closure); every module in it still goes through a full `validate-module` agent; and every module outside it retains a verdict a real agent produced on a prior run, recorded in `run-state.json`, not a name-pattern guess. The prohibited shortcut is GUESSING which modules are skippable. Deriving the changed-set from revisions is not guessing. You still may not hand-write or bulk-edit reports for changed-set modules.
- **No agent-replacement scripts.** Driver helpers like `next-modules.js` are allowed (they only read the manifest and check which reports exist). Helpers that would bulk-write reports, bulk-edit owner files, or otherwise act in lieu of an agent are not.