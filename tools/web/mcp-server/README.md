# Cobalt MCP

A graph-first MCP server for reverse engineering WhatsApp across all platforms.
Extracts, indexes, and exposes JavaScript module bundles and native WebAssembly modules as a structured knowledge graph — then lets you reverse-engineer the WASM (call graph, C++ vtables, decompilation), attach a live debugger to JS or WASM, inspect protocol traffic, and manipulate runtime state, all through MCP tools your AI agent can call.

## Platforms

| Platform                       | Extraction method                            | Live session      |
|--------------------------------|----------------------------------------------|-------------------|
| **WhatsApp Web**               | Playwright (Chromium)                        | CDP               |
| **WhatsApp Desktop (Windows)** | CDP                                          | CDP               |
| **WhatsApp Desktop (macOS)**   | Ghidra decompilation of Mac Catalyst Mach-O  | NOT YET SUPPORTED |
| **WhatsApp iOS**               | Ghidra decompilation of decrypted IPA/Mach-O | NOT YET SUPPORTED |
| **WhatsApp Android**           | NOT YET SUPPORTED                            | NOT YET SUPPORTED |

Since late 2024 the macOS desktop app is a Mac Catalyst port of the iOS binary (not an Electron web wrapper). Point `WEB_MCP_MACOS_BINARY` at `/Applications/WhatsApp.app` (or its Mach-O executable) to extract a `desktop_macos` snapshot via the same Ghidra pipeline used for iOS.

## Features

### Static analysis — Module knowledge graph

Every extracted JavaScript module is parsed with Babel into a rich AST index:

- **Fuzzy search** across module names, exports, dependencies, symbols, and string literals (powered by Orama with English stemming)
- **Dependency graph traversal** — BFS forward (what does this module use?) and reverse (what uses this module?)
- **Export resolution** — trace any export back to its implementing symbol with exact byte ranges
- **Cross-module call edges** — see which functions in module A call exports from module B
- **Symbol references** — find every usage of a function/class/variable across the entire bundle
- **Code search** — regex or literal search across all module sources, or fast indexed literal search
- **Switch dispatch detection** — automatically identifies polymorphic routing patterns
- **Persistent annotations** — label and annotate modules with notes that survive across sessions

### Static analysis — WASM

Native WebAssembly modules get a full reverse-engineering pipeline, built on an in-process operator decoder (no external dependencies for the static core):

- **Structural analysis** — imports, exports, function signatures, memory/table/global declarations, section sizes
- **Element + data segments** — the table-slot to function-index map, and data-segment descriptors including passive segments placed via `memory.init` (Emscripten pthread/shared-memory builds)
- **Data strings + symbol recovery** — extracts C string constants and maps each back to the functions that load its address (the path to names in a stripped binary)
- **Call graph** — direct `call`/`ref.func` edges plus `call_indirect` sites resolved to type-compatible candidate callees
- **C++ vtable recovery** — walks Itanium RTTI (`_ZTS`/`_ZTI`/`_ZTV`) to map a typeinfo name to its vtable and each virtual slot to a function index
- **Constant search** — find every function whose body contains a specific `i32.const` value (e.g. a magic number)
- **WAT disassembly** — full module or per-function WebAssembly Text output
- **C pseudocode** — per-function decompilation via Ghidra headless + the ghidra-wasm-plugin (optional; see Installation)
- **Binary access + patching** — base64 binary slices with byte-range support, plus length-preserving byte patches (no-op a function, overwrite bytes at a linear address or file offset, clear the shared-memory bit)
- **Cross-references** — identifies which JS loader modules reference each WASM module

Where the abstraction matches the JS graph, these are surfaced through the same tools: `find_references`, `search_code`, and `trace_dependencies` each accept a WASM target, alongside the dedicated `get_native_module_*` and `patch_native_module` tools.

### Revision snapshots

Every extraction produces a versioned snapshot tied to the client revision:

- **Snapshot history** — keep multiple snapshots per platform, switch between them at runtime
- **Diff engine** — compare any two snapshots to see added/removed/changed modules with symbol-level deltas and optional source excerpts
- **Hot reload** — switch the active catalog to a different snapshot without restarting the server

### Live sessions

Attach to a running WhatsApp instance for real-time inspection and control:

- **Session lifecycle** — start, stop, health-check, validate snapshot-to-runtime revision match
- **Phone login automation** — submit phone number, extract pairing code, wait for login completion
- **ADB integration** — list Android devices, automate pairing code entry on-device via UI automation

### Protocol observability

Inspect and inject protocol-level traffic in real time:

- **Stanza capture** — query inbound/outbound XML stanzas with filters on tag, id, from, to, attributes, or free-text
- **Stanza injection** — send custom stanza stanzas for targeted protocol experiments
- **WAM telemetry** — capture, query, and inject WAM (WhatsApp Analytics/Metrics) events
- **WAM schema discovery** — inspect event constructor definitions and field schemas before crafting events
- **Network capture** — record and query WebSocket frames and HTTP requests with direction/URL/content filters

### AB prop control

Full read/write access to the runtime's A/B testing flags:

- **Query** — list all flags, filter by name, diff against defaults
- **Schema discovery** — inspect flag definitions (name, code, type, defaults)
- **Mutate** — set individual flags, reset one or all to defaults

### JavaScript & WASM debugger

Full CDP debugger integration for both JavaScript and WebAssembly:

- **Script discovery** — list runtime scripts by URL pattern, including WASM modules (reported with their code offset)
- **Expression evaluation** — execute arbitrary JavaScript with promise support
- **Breakpoints** — set by URL+line or scriptId+line (JS), or by absolute module byte offset (WASM), with optional conditions
- **Stepping** — pause, resume, step over, step into, step out
- **Paused state inspection** — call stack, scope variables (including WASM locals, globals, and the operand stack), and pause reason
- **WASM linear memory** — read a slice of a paused WASM frame's memory as base64
- **Patched-binary serving** — serve a patched WASM binary in place of the original via CDP request interception

## Installation

### Prerequisites

```bash
cd tools/web/mcp-server
npm install
npx playwright install chromium
npm run build
```

For iOS/macOS native analysis, install [Ghidra](https://ghidra-sre.org/) and optionally set `GHIDRA_INSTALL_DIR`.

**WASM C-pseudocode decompilation** (`get_native_module_wat` with `format=ghidra`) additionally needs the [ghidra-wasm-plugin](https://github.com/nneonneo/ghidra-wasm-plugin), built against your exact Ghidra version and installed into `<GHIDRA_INSTALL_DIR>/Ghidra/Extensions/`:

```bash
git clone https://github.com/nneonneo/ghidra-wasm-plugin
cd ghidra-wasm-plugin
# Ghidra 12 builds with JDK 21 (not newer); point JAVA_HOME at a JDK 21.
GHIDRA_INSTALL_DIR=/path/to/ghidra gradle buildExtension
# then unzip dist/*.zip into <GHIDRA_INSTALL_DIR>/Ghidra/Extensions/
```

The plugin is optional: every other WASM tool (and `format=wat`) works without it; only `format=ghidra` requires it. When Ghidra or the plugin is missing, `format=ghidra` returns a clear error rather than failing.

`format=ghidra` imports and auto-analyzes the entire WASM module before decompiling, which takes minutes for the large (multi-MB) voip/media modules. That cost is paid once per call, so `functionIndex` accepts an array of indices that are decompiled in a single batch; prefer one call with many indices over many single-index calls. The server removes its own HTTP request timeouts for these long runs, but the MCP client still enforces its own per-call timeout: raise it (Claude Code reads `MCP_TOOL_TIMEOUT`, in milliseconds, from the environment) before invoking `format=ghidra` on a large module, e.g. `MCP_TOOL_TIMEOUT=600000`.

### Claude Code

**CLI:**

```bash
claude mcp add --transport stdio \
  --env WEB_MCP_PLATFORM=auto \
  --env WEB_MCP_LOG_LEVEL=info \
  cobalt-mcp -- stanza tools/web/mcp-server/dist/index.js
```

Add `--scope user` for global (all projects) or `--scope project` to commit it in `.mcp.json`.

**JSON** — add to `.mcp.json` at the project root (or `~/.claude/.mcp.json` for global):

```json
{
  "mcpServers": {
    "cobalt-mcp": {
      "type": "stdio",
      "command": "stanza",
      "args": ["tools/web/mcp-server/dist/index.js"],
      "env": {
        "WEB_MCP_PLATFORM": "auto",
        "WEB_MCP_LOG_LEVEL": "info"
      }
    }
  }
}
```

### OpenAI Codex

**CLI:**

```bash
codex mcp add cobalt-mcp \
  --env WEB_MCP_PLATFORM=auto \
  --env WEB_MCP_LOG_LEVEL=info \
  -- stanza tools/web/mcp-server/dist/index.js
```

**JSON** — add to `.codex/config.toml` or `codex.json`:

```json
{
  "mcpServers": {
    "cobalt-mcp": {
      "type": "stdio",
      "command": "stanza",
      "args": ["tools/web/mcp-server/dist/index.js"],
      "env": {
        "WEB_MCP_PLATFORM": "auto",
        "WEB_MCP_LOG_LEVEL": "info"
      }
    }
  }
}
```

### Gemini CLI

**CLI:**

```bash
gemini mcp add \
  -e WEB_MCP_PLATFORM=auto \
  -e WEB_MCP_LOG_LEVEL=info \
  cobalt-mcp stanza tools/web/mcp-server/dist/index.js
```

Add `--scope user` for global configuration.

**JSON** — add to `~/.gemini/settings.json`:

```json
{
  "mcpServers": {
    "cobalt-mcp": {
      "command": "stanza",
      "args": ["tools/web/mcp-server/dist/index.js"],
      "env": {
        "WEB_MCP_PLATFORM": "auto",
        "WEB_MCP_LOG_LEVEL": "info"
      }
    }
  }
}
```

### OpenCode

**JSON** — add to `opencode.jsonc`:

```json
{
  "mcp": {
    "cobalt-mcp": {
      "type": "local",
      "command": ["stanza", "tools/web/mcp-server/dist/index.js"],
      "environment": {
        "WEB_MCP_PLATFORM": "auto",
        "WEB_MCP_LOG_LEVEL": "info"
      }
    }
  }
}
```

## Configuration

All configuration is via environment variables. Copy `.env.example` to `.env` and adjust:

| Variable                       | Description                                                                  | Default      |
|--------------------------------|------------------------------------------------------------------------------|--------------|
| `WEB_MCP_PLATFORM`             | Target platform(s): `auto`, `web`, `desktop_windows`, `desktop_macos`, `ios` | `auto`       |
| `WEB_MCP_MODE`                 | Startup mode: empty for live extraction, `cached` for offline                | empty (live) |
| `WEB_MCP_SNAPSHOT_ID`          | Load a specific snapshot by ID                                               | —            |
| `WEB_MCP_SOURCE_DIR`           | Ingest modules from a local directory of JS files                            | —            |
| `WEB_MCP_REVISION`             | Override revision string when ingesting from directory                       | —            |
| `WEB_MCP_IOS_BINARY`           | Path to decrypted IPA or Mach-O binary                                       | —            |
| `WEB_MCP_MACOS_BINARY`         | Path to WhatsApp.app bundle or its Mach-O executable                         | auto-detect  |
| `GHIDRA_INSTALL_DIR`           | Ghidra installation path (auto-detected if unset)                            | —            |
| `WEB_MCP_NATIVE_ANALYSIS_TIMEOUT` | Per-file Ghidra timeout in seconds (native Mach-O pipeline)               | `1800`       |
| `WEB_MCP_NATIVE_MAX_CPU`       | Max CPU cores for Ghidra (native Mach-O pipeline)                            | `4`          |
| `WEB_MCP_WEB_LOCALE`           | UI locale for WhatsApp Web                                                   | `en-US`      |
| `WEB_MCP_PHONE_NUMBER`         | Phone number for automated login (E.164)                                     | —            |
| `WEB_MCP_PHONE_COUNTRY_CODE`   | Country code override for phone login                                        | —            |
| `ADB_PATH`                     | Path to adb binary                                                           | `adb`        |
| `WEB_MCP_LOG_LEVEL`            | Log level: `debug`, `info`, `warn`, `error`, `silent`                        | `info`       |

## Tool reference

### Catalog tools (static analysis)

| Tool                  | Description                                                                            |
|-----------------------|----------------------------------------------------------------------------------------|
| `search_modules`      | Full-text fuzzy search over module names, exports, dependencies, symbols, and literals |
| `get_module_metadata` | Returns dependencies, exports, source hash, and byte size for a module                 |
| `get_exports`         | Returns export bindings with binding kind (identifier, member, computed, unresolved)   |
| `resolve_export`      | Resolves a specific export to its implementing symbol with byte range and source       |
| `get_module_source`   | Returns module source code with optional byte-range or line-range slicing              |
| `get_symbol_source`   | Returns the exact source code for a named function, class, or variable                 |
| `find_references`     | References to a symbol across JS modules, or (with `nativeModule`) functions referencing a WASM data string/address |
| `trace_dependencies`  | BFS over the JS dependency graph, or (with `nativeModule` + `funcIndex`) a WASM function call graph                  |
| `search_code`         | Regex/literal search over JS source or literals, or WASM data strings (`wasm_data`) / `i32.const` values (`wasm_const`) |
| `manage_annotations`  | Create, read, list, or remove persistent module annotations                            |

### Native module tools (WASM)

| Tool                         | Description                                                                             |
|------------------------------|-----------------------------------------------------------------------------------------|
| `list_native_modules`        | Lists all WASM modules with name, URL, hash, and size                                   |
| `get_native_module_metadata` | Structural analysis: signatures, imports, exports, memory/globals, element + data segments, cross-refs |
| `get_native_module_wat`      | WAT disassembly (full module or single function), or C pseudocode via Ghidra (`format=ghidra`)         |
| `get_native_module_binary`   | Base64-encoded binary slice with byte-range support                                     |
| `get_native_module_vtables`  | Recovers C++ vtables for an Itanium typeinfo name; maps each virtual slot to a function index          |
| `patch_native_module`        | Length-preserving byte patches (no-op a function, overwrite bytes, clear shared-memory bit); writes a patched `.wasm` |

### Snapshot tools

| Tool                  | Description                                                                          |
|-----------------------|--------------------------------------------------------------------------------------|
| `list_snapshots`      | Lists available snapshot IDs across platforms                                        |
| `get_active_snapshot` | Returns current snapshot metadata (ID, revision, module count)                       |
| `get_revision_diff`   | Compares two snapshots with module/export/symbol deltas and optional source excerpts |
| `switch_snapshot`     | Hot-reloads the catalog to a different snapshot                                      |

### Live session tools

| Tool                                  | Description                                                            |
|---------------------------------------|------------------------------------------------------------------------|
| `web_live_start_session`              | Starts a live session (web browser or Windows desktop via CDP)         |
| `web_live_stop_session`               | Stops the live session and clears capture state                        |
| `web_live_status`                     | Health check: session state, auth state, revision match                |
| `web_live_validate_snapshot_revision` | Confirms static snapshot matches live runtime revision                 |
| `web_live_login_with_phone_number`    | Submits phone number and extracts pairing code                         |
| `web_live_wait_for_login`             | Blocks until login completes                                           |
| `web_live_adb_list_devices`           | Lists connected Android devices via ADB                                |
| `web_live_adb_link_pairing_code`      | Automates pairing code entry on an Android device                      |

### Observability tools

| Tool                                 | Description                                                           |
|--------------------------------------|-----------------------------------------------------------------------|
| `web_live_stanza_query_nodes`        | Query captured stanza traffic with direction/tag/id/attr/text filters |
| `web_live_stanza_send_node`          | Send a custom stanza stanza into the live runtime                       |
| `web_live_wam_get_events`            | Query captured WAM telemetry events                                   |
| `web_live_wam_send_event`            | Send a custom WAM event with optional props                           |
| `web_live_wam_get_event_definitions` | Inspect WAM event constructor schemas                                 |
| `web_live_ab_props_query`            | List/filter/diff AB test flags                                        |
| `web_live_ab_props_get`              | Look up a single AB prop by name                                      |
| `web_live_ab_props_set`              | Set an AB prop value                                                  |
| `web_live_ab_props_reset`            | Reset one AB prop to default                                          |
| `web_live_ab_props_reset_all`        | Reset all AB props to defaults                                        |
| `web_live_ab_props_definitions`      | Inspect AB prop schemas (name, code, type, defaults)                  |
| `web_live_clear_capture`             | Clear stanza, WAM, network, or all capture buffers                    |

### Debug tools

| Tool                                   | Description                                           |
|----------------------------------------|-------------------------------------------------------|
| `web_live_debug_list_scripts`          | List runtime scripts (JS and WASM) by URL filter      |
| `web_live_debug_eval`                  | Evaluate JavaScript in the live runtime               |
| `web_live_debug_set_breakpoint_url`    | Set breakpoint by script URL + line/column            |
| `web_live_debug_set_breakpoint_script` | Set breakpoint by scriptId + line/column              |
| `web_live_debug_set_wasm_breakpoint`   | Set a breakpoint inside a WASM script at an absolute byte offset |
| `web_live_debug_read_wasm_memory`      | Read a slice of a paused WASM frame's linear memory as base64    |
| `web_live_debug_remove_breakpoint`     | Remove a breakpoint                                   |
| `web_live_debug_command`               | Debugger stepping: pause, resume, step over/into/out  |
| `web_live_debug_paused_state`          | Inspect call stack, scope variables (JS or WASM locals/globals/stack), and pause reason |
| `web_live_serve_wasm`                  | Serve a patched WASM binary in place of the original via CDP Fetch interception |

### Network tools

| Tool                      | Description                                                         |
|---------------------------|---------------------------------------------------------------------|
| `web_live_network_start`  | Start capturing WebSocket frames and HTTP requests                  |
| `web_live_network_stop`   | Stop network capture                                                |
| `web_live_network_query`  | Query captured frames/requests with type/direction/URL/text filters |
| `web_live_network_status` | Current capture state and frame/request counts                      |