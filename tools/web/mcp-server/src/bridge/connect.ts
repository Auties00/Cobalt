import type { SnapshotPlatform } from "../types/snapshot.js";
import type { PlatformBridge, ConnectionOptions } from "../types/bridge.js";
import { CdpBridge } from "./cdp-bridge.js";
import { DEFAULT_DESKTOP_CDP_PORT } from "../live/manager-constants.js";
import { createLogger } from "../utils/logger.js";

const log = createLogger("bridge");

export async function connectToPlatform(
  platform: SnapshotPlatform,
  options: ConnectionOptions = {}
): Promise<PlatformBridge> {
  log.info(`connecting to platform: ${platform}`);
  switch (platform) {
    case "web":
      return CdpBridge.connect(platform, {
        launch: {
          headless: options.headless ?? false,
          slowMo: options.slowMo,
          locale: options.locale,
        },
      });

    case "desktop_windows":
      return CdpBridge.connect(platform, {
        cdpUrl: `http://localhost:${options.cdpPort ?? DEFAULT_DESKTOP_CDP_PORT}`,
        autoLaunchDesktop: true,
      });

    default:
      throw new Error(
        `connectToPlatform does not support platform="${platform}". ` +
          `Only web and desktop_windows expose a live JS runtime; ` +
          `desktop_macos and ios are analyzed statically via Ghidra.`
      );
  }
}

export { CdpBridge } from "./cdp-bridge.js";
export type { PlatformBridge, ConnectionOptions, CapturedResponse, ResponseListener, LoadedResources } from "../types/bridge.js";
