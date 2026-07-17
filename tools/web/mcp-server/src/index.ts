import "dotenv/config";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { bootstrapCatalogFromEnv, loadCatalogById } from "./services/bootstrap.js";
import { SnapshotCatalog } from "./services/catalog.js";
import { getLiveWebSessionRegistry } from "./live/registry.js";
import { getEmulatorRegistry } from "./live/emulator/registry.js";
import { registerCatalogTools } from "./mcp/tools/catalog-tools.js";
import { registerSnapshotTools } from "./mcp/tools/snapshot-tools.js";
import { registerLiveTools } from "./mcp/tools/live-tools.js";
import type { SnapshotPlatform } from "./types/snapshot.js";
import { DEFAULT_PLATFORM, resolveAutoPlatforms } from "./storage/snapshot-utils.js";
import { log } from "./utils/logger.js";
import { createServer } from "node:http";

const serverLog = log.child("server");

const catalogs = new Map<SnapshotPlatform, SnapshotCatalog>();
const registry = getLiveWebSessionRegistry();
const emulatorRegistry = getEmulatorRegistry();
let bootstrapDone = false;

function requireReady(): void {
    if (!bootstrapDone) {
        throw new Error("Server is still initializing, please retry in a moment.");
    }
}

function requireCatalog(platform?: SnapshotPlatform): SnapshotCatalog {
    requireReady();
    const p = platform ?? DEFAULT_PLATFORM;
    const catalog = catalogs.get(p);
    if (!catalog) {
        const available = [...catalogs.keys()];
        if (available.length === 0) {
            serverLog.error("requireCatalog: no catalogs initialized");
            throw new Error("No catalogs initialized.");
        }
        serverLog.warn(`requireCatalog: platform '${p}' not loaded, available: ${available.join(", ")}`);
        throw new Error(
            `No catalog loaded for platform '${p}'. Available: ${available.join(", ")}`
        );
    }
    return catalog;
}

function activeSnapshotRevision(): string | null {
    const catalog = catalogs.get(DEFAULT_PLATFORM);
    return catalog?.revision ?? catalogs.values().next().value?.revision ?? null;
}

async function switchCatalog(platform: SnapshotPlatform, snapshotId: string): Promise<SnapshotCatalog> {
    serverLog.info(`switching catalog: platform=${platform} snapshotId=${snapshotId}`);
    const catalog = await loadCatalogById(platform, snapshotId);
    catalogs.set(platform, catalog);
    serverLog.info(`catalog switched: platform=${platform} revision=${catalog.revision} modules=${catalog.getAllModules().length}`);
    return catalog;
}

function createConfiguredServer(): McpServer {
    const server = new McpServer({
        name: "web-mcp-server-new",
        version: "3.0.0",
    });
    registerCatalogTools(server, {
        requireCatalog,
        switchCatalog,
        listLoadedCatalogs: () => [...catalogs.values()],
    });
    registerSnapshotTools(server, { requireCatalog, requireReady });
    registerLiveTools(server, {
        registry,
        emulatorRegistry,
        activeSnapshotRevision,
        requireReady,
        requireSession: (id: string) => registry.requireSession(id),
        requireEmulator: (name: string) => emulatorRegistry.requireEmulator(name),
    });
    return server;
}

function logStartupBanner(): void {
    const rawPlatform = process.env.WEB_MCP_PLATFORM ?? "auto";
    const isAuto = rawPlatform === "auto" || rawPlatform === "";
    serverLog.info("starting web-mcp-server-new v3.0.0");
    serverLog.info(`log level: ${process.env.WEB_MCP_LOG_LEVEL ?? "info (default)"}`);
    serverLog.info(`platform: ${rawPlatform}${isAuto ? ` (resolved: [${resolveAutoPlatforms().join(", ")}])` : ""}`);
    serverLog.info(`mode: ${process.env.WEB_MCP_MODE ?? "live (default)"}`);
}

async function bootstrapCatalogs(): Promise<void> {
    const bootstrapped = await serverLog.time("bootstrap catalogs", () =>
        bootstrapCatalogFromEnv()
    );
    for (const [platform, catalog] of bootstrapped) {
        catalogs.set(platform, catalog);
        serverLog.info(
            `${platform}: snapshot=${catalog.snapshotId} revision=${catalog.revision} modules=${catalog.getAllModules().length}`
        );
    }
    bootstrapDone = true;
}

async function mainStdio(): Promise<void> {
    logStartupBanner();

    const server = createConfiguredServer();
    const transport = new StdioServerTransport();
    await server.connect(transport);
    serverLog.info("MCP server connected (stdio), bootstrapping catalogs...");

    await bootstrapCatalogs();
    serverLog.info("MCP server ready (stdio)");
}

async function mainHttp(): Promise<void> {
    logStartupBanner();
    const port = parseInt(process.env.WEB_MCP_HTTP_PORT ?? "8787", 10);

    serverLog.info(`HTTP mode: bootstrapping catalogs before accepting connections...`);
    await bootstrapCatalogs();
    serverLog.info("catalogs ready, starting HTTP server...");

    const httpServer = createServer(async (req, res) => {
        const url = new URL(req.url ?? "/", `http://localhost:${port}`);

        res.setHeader("Access-Control-Allow-Origin", "*");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, Mcp-Session-Id");
        res.setHeader("Access-Control-Expose-Headers", "Mcp-Session-Id");

        if (req.method === "OPTIONS") {
            res.writeHead(204);
            res.end();
            return;
        }

        if (url.pathname === "/health") {
            res.writeHead(200, { "Content-Type": "application/json" });
            res.end(JSON.stringify({ ready: bootstrapDone, version: "3.0.0" }));
            return;
        }

        if (url.pathname === "/mcp") {
            if (req.method === "POST") {
                try {
                    const server = createConfiguredServer();
                    const transport = new StreamableHTTPServerTransport({ sessionIdGenerator: undefined });
                    res.on("close", () => {
                        transport.close().catch(() => {});
                        server.close().catch(() => {});
                    });
                    await server.connect(transport);
                    await transport.handleRequest(req, res);
                } catch (err) {
                    serverLog.error(`HTTP handler error: ${err}`);
                    if (!res.headersSent) {
                        res.writeHead(500, { "Content-Type": "application/json" });
                        res.end(JSON.stringify({ error: "Internal server error" }));
                    }
                }
                return;
            }

            res.writeHead(405, { "Content-Type": "application/json" });
            res.end(JSON.stringify({ error: "Method not allowed (stateless mode)" }));
            return;
        }

        res.writeHead(404, { "Content-Type": "application/json" });
        res.end(JSON.stringify({ error: "Not found" }));
    });

    // A single Ghidra-backed decompile (get_native_module_wat format=ghidra) can
    // run for minutes while it imports and auto-analyzes a multi-MB WASM module.
    // Node's default request/socket timeouts (requestTimeout 300000, headersTimeout
    // 60000) would sever the connection mid-analysis, so disable them; the MCP
    // client still bounds the call with its own MCP_TOOL_TIMEOUT.
    httpServer.requestTimeout = 0;
    httpServer.headersTimeout = 0;
    httpServer.timeout = 0;

    httpServer.listen(port, () => {
        serverLog.info(`MCP HTTP server listening on http://localhost:${port}/mcp`);
        serverLog.info(`Health check: http://localhost:${port}/health`);
    });
}

async function shutdown(signal: string): Promise<void> {
    serverLog.info(`received ${signal}, stopping all live sessions + emulators...`);
    try {
        await registry.stopAllSessions();
    } catch (err) {
        serverLog.error(`shutdown sessions error: ${err instanceof Error ? err.message : String(err)}`);
    }
    try {
        await emulatorRegistry.stopAll();
    } catch (err) {
        serverLog.error(`shutdown emulators error: ${err instanceof Error ? err.message : String(err)}`);
    }
    process.exit(0);
}

process.on("SIGINT", () => void shutdown("SIGINT"));
process.on("SIGTERM", () => void shutdown("SIGTERM"));

const serverMode = process.env.MCP_TRANSPORT ?? "http";
const httpMode = serverMode.toLowerCase() === "http";
if (httpMode) {
    await mainHttp();
} else {
    await mainStdio();
}
