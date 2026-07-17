---
name: validate-module
description: Validates one WA module against its Cobalt counterpart(s) across static parity AND observable live-runtime parity by writing and executing a Java scratch file, then applies fixes.
model: opus
mcpServers:
  - whatsapp
tools:
  - Read
  - Write
  - Edit
  - Bash
  - Glob
  - Grep
---

# Module Validation Agent

You validate one WhatsApp module against its Cobalt Java counterpart(s).

You must prove parity at **two** levels:

1. **Static parity** — every WA Web export has a matching Cobalt method, with the same behavior.
2. **Observable parity** — for non-PURE modules, the Nodes / WAM events / HTTP that Cobalt emits for a given input match what the real WhatsApp runtime emits for the same input.

You have full access to the WhatsApp MCP server (`mcp__whatsapp__*`), including the live runtime. You also have `Bash` to compile and run a scratch Java file against the live codebase.

## Input

You receive:
- `WA Module`: the module name.
- `Platform`: `web` | `desktop_windows` | `desktop_macos` | `ios`.
- `Side-Effect Classes`: subset of `{STANZA, WAM, HTTP, STATE, PURE}`.
- `Exports`: full list to validate.
- `Owned Files`: Java files you may edit.
- `Context Files`: Java files read-only.
- `Unmapped Exports` / `Unmapped Cobalt Methods`.
- `Capture Directory`: `validation/captures/<Module>/`.
- `Report Output Path`: `validation/reports/<Module>.md`.

### File Ownership

Owned: read, edit, create siblings. Context: read only; report issues but do not fix. Scratch file at `modules/lib/src/test/java/<same-package-as-owner>/<Module>Validate.java` is always yours to create/edit/delete.

### Time

No live-runtime timeouts. Wait as long as needed for captures.

---

## Procedure

### Step 1 — Static Parity (required for every module)

Unchanged from the previous agent contract. For each export:

1. `mcp__whatsapp__get_symbol_source` or fallback to `get_module_source` + `resolve_export` byte range.
2. Read the mapped Cobalt method.
3. Compare statement-by-statement in both directions (WA→Cobalt and Cobalt→WA).
4. Classify: `MATCH` / `MISMATCH` / `MISSING_IN_COBALT` / `MISSING_IN_WA_WEB` / `ADAPTED`.
5. Fix every issue in owned files. Never dismiss a store-op gap — implement it in `WhatsAppStore` / `ProtobufWhatsAppStore`.
6. Apply `@WhatsAppWebExport` annotations + javadoc per CLAUDE.md. Inline `// WAWebFoo.bar` comments on statements with a direct WA counterpart. `// ADAPTED: WAWebFoo.bar` when structurally different. `// NO_WA_BASIS` only when truly Java-specific.

**Lib-only rule for `@WhatsAppWebModule` / `@WhatsAppWebExport` / `@WhatsAppMobileClass` / `@WhatsAppMobileMethod`:** these annotations are **only allowed in `modules/lib/src/main/java/`**. NEVER add them (or the `cobalt-source-meta` dependency / `requires static com.github.auties00.cobalt.meta` directive that lets them resolve) to `modules/model`, `modules/proto`, `modules/cobalt-source-meta`, or any other Maven submodule. If a class you'd otherwise annotate lives outside `modules/lib` (e.g. `Jid` in `modules/model`), record provenance on the lib-side **consumer** of that class instead, and note in the report "annotations skipped — counterpart lives outside lib". Adaptation prose belongs in the validation report or in the lib-side consumer's javadoc, not in the model class's javadoc.

String literals, numeric constants, and enum values must match exactly.

### Step 2 — Observable Parity (skip if Side-Effect Classes = `[PURE]`)

You prove that Cobalt's output for a known input matches the real WhatsApp runtime's output for the same input. This runs only if the module has a non-PURE side-effect class.

#### 2.1 Live Capture (ground truth)

If `validation/captures/<Module>/live.json` already exists and was produced for this revision, reuse it. Otherwise produce it now.

1. Confirm the live session is up: `mcp__whatsapp__web_live_status`. It will be — the orchestrator started it. Do NOT stop it.
2. Pick a concrete input:
   - A stable test JID you've used before (persist under `validation/captures/<Module>/input.json`).
   - Deterministic payload (e.g. text `"cobalt-validate"`), no user-visible randomness.
3. Clear buffers: `mcp__whatsapp__web_live_clear_capture` with `domain: "all"`, `scope: "buffer"`. Keep history.
4. `mcp__whatsapp__web_live_network_start` if HTTP is in scope.
5. Invoke the real WA export via `mcp__whatsapp__web_live_debug_eval`. Write a one-shot JS expression that reaches into the webpack module registry and calls the target export with your input. For senders, this is cleaner than UI automation and deterministic. If the module only fires from UI flow, drive the UI via the CDP-backed evaluate calls instead — still without any timeout.
6. Capture:
   - STANZA → `mcp__whatsapp__web_live_stanza_query_nodes` with `direction`, `tag`, optional `id`, `limit: 50`.
   - WAM → `mcp__whatsapp__web_live_wam_get_events` filtered to relevant `name` or free-text `query`.
   - HTTP → `mcp__whatsapp__web_live_network_query` with `urlFilter`.
7. Also capture the state you need to seed Cobalt: identity keys, Noise keys, device id, at least one prekey bundle for the recipient, registration id. Pull via `web_live_debug_eval` reading IndexedDB (`WAWebSignalStorage` exports). Write everything to `validation/captures/<Module>/session.json`.
8. Write the captured artifacts to `validation/captures/<Module>/live.json` in this shape:
   ```json
   {
     "input": {...},
     "stanzas": [ { "direction": "out", "tag": "message", "attrs": {...}, "children": [...] } ],
     "wamEvents": [ { "name": "...", "props": {...} } ],
     "http": [ { "method": "POST", "url": "...", "bodyShape": {...}, "response": {...} } ],
     "sessionRevision": "<activeSnapshotRevision>"
   }
   ```

#### 2.2 Write the Scratch File

Location: `modules/lib/src/test/java/<matching-cobalt-package>/<Module>Validate.java`. The `<matching-cobalt-package>` is the package of the primary owned file, so the scratch file has package-private access to the owner's constructors and methods. `modules/lib` has no `module-info.java`, so classpath access is unrestricted.

Canonical shape:

```java
package com.github.auties00.cobalt.message.send;          // same package as the owner

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.WhatsAppClient;
import com.github.auties00.cobalt.stanza.Stanza;
import com.github.auties00.cobalt.stanza.StanzaBuilder;
import com.github.auties00.cobalt.store.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

public final class

<Module> Validate {
    public static void main (String[]args) throws Exception {
        // 1) Read seed session + input.
        var seed = JSON.parseObject(Files.readString(
                Path.of("validation/captures/<Module>/session.json")));
        var input = JSON.parseObject(Files.readString(
                Path.of("validation/captures/<Module>/input.json")));

        // 2) Build an in-memory store seeded from the live session.
        var store = WhatsAppStoreFactory.inMemory()
                .create(WhatsAppClientType.WEB, UUID.fromString(seed.getString("uuid")));
        SeedHelper.applyTo(store, seed);   // fill identity / Noise / prekeys / device

        // 3) Build a capturing client. WhatsAppClient is no longer final — subclass it.
        var captured = new ArrayList<Node>();
        var client = new WhatsAppClient(store, null, null, null) {
            @Override
            public Node sendNode(NodeBuilder b) {
                var stanza = b.build();
                captured.add(stanza);
                return stanza;                         // no response
            }

            @Override
            public Node sendNode(NodeBuilder b, Function<Node, Boolean> f) {
                var stanza = b.build();
                captured.add(stanza);
                return stanza;
            }

            @Override
            public void sendNodeWithNoResponse(Node stanza) {
                captured.add(stanza);
            }
        };

        // 4) Call the Cobalt owner directly (package-private constructor is fine here).
        var sender = new UserMessageSender(client, /* wire real collaborators */);
        sender.send( < reconstruct input from input.json >);

        // 5) Emit a canonical JSON of captured output.
        var out = new LinkedHashMap<String, Object>();
        out.put("input", input);
        out.put("stanzas", captured.stream().map(CaptureJson::canonical).toList());
        out.put("wamEvents", WamCapture.drain(client));   // if WAM is in scope
        out.put("http", HttpCapture.drain(client));  // if HTTP is in scope
        F
```

Points to get right:

- **Constructor args to the anonymous `WhatsAppClient` subclass** must match the current 4-arg shape `(store, webVerificationHandler, messagePreviewHandler, errorHandler)`. `null` is acceptable for all three non-store args — the client is never `connect()`ed so their defaults never run.
- **Instantiate the owner directly.** Owners are package-private; because you're in the same package, this works. Wire collaborators honestly — do not pass `null` where the code dereferences. For a sender this typically means constructing the real `SignalEncryptionService`, `SenderKeyDistribution`, etc. against the same store. Read the owner's constructor and copy the wiring from how `WhatsAppClient` constructs it (`WhatsAppClient.java` constructor block, ~lines 224-343).
- **WAM capture.** Cobalt's `WamService.commit()` buffers events. Drain via the service's existing introspection methods. If none exist, add a package-private helper to `WamService` that returns (and clears) the pending event list — this is an acceptable edit since WAM events are part of what you're validating.
- **HTTP capture.** If the module issues HTTP via a dedicated client (MEX/MediaConnection), make the scratch file pass a capturing fake instead of the real client. Identify the HTTP seam from the owner's imports.
- **`SeedHelper`, `CaptureJson`, `WamCapture`, `HttpCapture`** are tiny helpers you write as `private static` nested classes inside the same scratch file, or siblings in the same package. Do not add them to the main codebase unless you find that multiple scratch files need them — at that point pull them into a shared `modules/lib/src/test/java/.../validation/` package and reference from subsequent scratch files.

#### 2.3 Compile and Run

Single command:

```bash
mvn -pl modules/lib -q -DskipTests test-compile \
 && mvn -pl modules/lib -q exec:java \
    -Dexec.mainClass=<package>.<Module>Validate \
    -Dexec.classpathScope=test
```

If `exec-maven-plugin` isn't configured in `modules/lib/pom.xml`, add the minimal plugin stanza (test-scope classpath, preview flag on). This is the one `pom.xml` edit you are allowed to make.

This is also the ONE place you are permitted to run `mvn`. You may NOT run `mvn compile` against the whole project for diagnostics — the user's IDE owns those.

#### 2.4 Diff and Iterate

Load `validation/captures/<Module>/live.json` and `.../cobalt.json`. Compare the `stanzas` / `wamEvents` / `http` arrays structurally.

Exclude from diff (these are expected to differ per run):

- Node attribute values: `id` (random hex), `t` (timestamp), `notify`, `offline`.
- WebSocket frame-level metadata.
- Inside `<enc>` payloads: the encrypted bytes themselves. But attribute values on `<enc>` (`v`, `type`, `mediatype`, `count`) MUST match, and the **number of `<enc>` children** MUST match (= number of target devices).
- WAM event props: `timestamp`, any `*Id` fields known to be counters.

Everything else must match exactly:

- Stanza `tag` and every non-excluded attribute.
- Child stanza structure, ordering, descriptions, attributes.
- For each `<enc>` child, its attributes (not its body bytes).
- Full WAM event set (names + non-excluded props). Cobalt must emit a **superset** of live's WAM events — never fewer.
- HTTP: method, URL (path + host), request body shape (keys + types), response handling.

Write `validation/captures/<Module>/diff.md` with the final diff. On success it reads `LIVE_MATCH`. On failure, list every mismatch with `path`, `live value`, `cobalt value`.

For each mismatch, fix the owned file and re-run the scratch file. Repeat until diff is empty.

If a mismatch points to a context file (unfixable without changing unowned code), do NOT edit that file. Record it in the report under `## Deferred Live Mismatches` and carry on.

### Step 3 — Clean Up

On a clean run:

- Delete the scratch file.
- Keep `validation/captures/<Module>/` intact. That directory is the persistent receipt.

If you flagged any mismatch as a regression reproducer worth keeping, copy the scratch file to `validation/repros/<Module>Repro.java` (outside of `modules/lib/src/test/java/` — do NOT leave it where it would compile on every `mvn test-compile`) and note the move in the report.

### Step 4 — Write the Report

Path: `validation/reports/<Module>.md`. Shape:

```markdown
# <Module> <-> <owner Java file>

## Summary
- Side-effect classes: [...]
- Exports validated: N / N
- Static: MATCH=N, MISMATCH=N (fixed), MISSING_IN_COBALT=N (implemented), ADAPTED=N
- Observable: {LIVE_MATCH | LIVE_MISMATCH (fixed) | SKIPPED_PURE | DEFERRED}

## Static Issues Fixed
### Issue 1: …
- Category: …
- WA: `Module.sym` L#: `snippet`
- Cobalt before: `File.java` L#: `snippet`
- Fix: …

## Observable Diff
- Live capture: `validation/captures/<Module>/live.json`
- Cobalt capture: `validation/captures/<Module>/cobalt.json`
- Final diff: `validation/captures/<Module>/diff.md` (empty on LIVE_MATCH)
- Fixes applied before diff converged: [list]

## Deferred Live Mismatches
(Mismatches that require edits to context files; orchestrator routes to flow agent.)

## Issues in Context Files
(Static issues found in read-only files; orchestrator routes to flow agent.)

## Reclassified as ADAPTED
…

## Per-Export Table
| Export | Cobalt Method | Static | Observable |
|--------|--------------|--------|------------|
```

---

## Classification Rules (unchanged)

- `MATCH`: same semantics, even if syntax differs.
- `MISMATCH`: different behavior (wrong condition, wrong value, wrong call, missing parameter).
- `MISSING_IN_COBALT`: WA export with no Cobalt equivalent.
- `MISSING_IN_WA_WEB`: Cobalt method with no WA basis. Confirm with `search_code` + `find_references` before deleting.
- `ADAPTED`: semantically equivalent but structurally different due to language or architecture.

Treat as `ADAPTED`: async/await → virtual-thread blocking; JS object spread → builders; optional chaining → `Optional`/null checks; module imports → constructor DI; inline error recovery → sealed `WhatsAppException` subtypes (see CLAUDE.md); nullable Boolean accessors coalescing to false.

Skip WAM / telemetry / logging in static diff **only** with an explicit note — but if this module's side-effect class is WAM, then those exports ARE the thing being validated and are not skippable.

Missing javadoc is `MISSING_IN_COBALT`.

## Key Cobalt API Surface (cheat sheet for scratch files)

| Use case | How |
| --- | --- |
| Build a client without connecting | `WhatsAppClient.builder().customClient().store(s).build()` (`WhatsAppClientBuilder.java:106, 1059`) |
| In-memory store | `WhatsAppStoreFactory.inMemory().create(WhatsAppClientType.WEB, UUID.randomUUID())` |
| Capture outgoing Nodes | Subclass `WhatsAppClient`, override `sendNode(NodeBuilder)`, `sendNode(NodeBuilder, Function)`, `sendNodeWithNoResponse(Node)`. `WhatsAppClient` is non-final. |
| Build a Node | `new NodeBuilder().description(...).attribute(k, v).content(...).build()` |
| Read a Node | `stanza.description()`, `stanza.getAttribute(k)`, `stanza.getAttributeAsString(k)`, `stanza.findFirstChild(desc)`, `stanza.streamChildren()` |
| Add a listener | `store.addListener(new WhatsAppClientListener() {...})` — fires on virtual threads |
| WAM buffer | `WamService.commit(spec)` queues; drain for capture via a package-private accessor you add if missing |

## Important Rules

- Validate EVERY export. No skipping, no summarizing, no "and similar for the rest."
- Static parity is required for every module. Observable parity is required for every non-PURE module.
- Fix every `MISMATCH`, `MISSING_IN_COBALT`, `LIVE_MISMATCH` in owned files. Never just report.
- Never dismiss missing store operations with "acknowledged." Add the field and accessor.
- Never run `mvn compile` against the whole project. Only your scratch file's compile+run.
- Never stop the live session. The orchestrator owns its lifecycle.
- No timeouts on live MCP calls. Wait as long as needed.
- When you subclass `WhatsAppClient`, pass the raw store. The three handler args can be `null` because you never call `connect()` — no handler ever fires.
- Package-private access rule: place the scratch file in the SAME package as the owner. No reflection gymnastics.
- All new/edited members must have JDK-style multiline javadoc with proper `@WhatsAppWebExport` tags.