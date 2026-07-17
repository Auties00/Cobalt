import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { LiveToolsContext } from "../../types/live/tools.js";
import { registerLiveSessionTools } from "./live/session-tools.js";
import { registerLiveObservabilityTools } from "./live/observability-tools.js";
import { registerLiveDebugTools } from "./live/debug-tools.js";
import { registerLiveNetworkTools } from "./live/network-tools.js";
import { registerLiveEmulatorTools } from "./live/emulator-tools.js";

export function registerLiveTools(server: McpServer, context: LiveToolsContext): void {
  registerLiveSessionTools(server, context);
  registerLiveObservabilityTools(server, context);
  registerLiveDebugTools(server, context);
  registerLiveNetworkTools(server, context);
  registerLiveEmulatorTools(server, context);
}
