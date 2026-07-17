import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { LiveToolsContext } from "../../../types/live/tools.js";
import { createLogger } from "../../../utils/logger.js";
import { getAndroidSdk } from "../../../live/emulator/android-sdk.js";
import {
  TextVerifiedProvider,
  getSmsVerificationProvider,
} from "../../../live/verification/textverified.js";

const log = createLogger("tools:live:emulator");

const emulatorNameSchema = z
  .string()
  .min(1)
  .regex(/^[A-Za-z0-9_-]{1,40}$/, "Use 1-40 chars of [A-Za-z0-9_-].")
  .describe("Caller-supplied emulator name (e.g. 'primary', 'counterparty', 'business').");

const apkVariantSchema = z
  .enum(["personal", "business"])
  .describe("Which WhatsApp APK variant to install (com.whatsapp vs com.whatsapp.w4b).");

export function registerLiveEmulatorTools(
  server: McpServer,
  context: LiveToolsContext
): void {
  const { requireReady, emulatorRegistry, requireEmulator } = context;

  server.tool(
    "web_live_emulator_list",
    "Read-only. Lists every registered emulator with its AVD, run state, registered WhatsApp account, and adb serial (when running). Call first to discover available emulators.",
    {},
    async () => {
      requireReady();
      try {
        const records = emulatorRegistry.listRecords();
        return {
          content: [{ type: "text" as const, text: JSON.stringify({ count: records.length, emulators: records }, null, 2) }],
        };
      } catch (error) {
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_emulator_list_system_images",
    "Read-only. Lists Android system images known to the local SDK. Installed images are flagged; use them as the 'systemImage' arg to web_live_emulator_create.",
    {},
    async () => {
      requireReady();
      try {
        const sdk = getAndroidSdk();
        const images = await sdk.listSystemImages();
        return {
          content: [{ type: "text" as const, text: JSON.stringify({ count: images.length, images }, null, 2) }],
        };
      } catch (error) {
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_emulator_create",
    "Creates a new AVD-backed emulator entry. The AVD is provisioned on disk but not booted. Choose the APK variant now (switching later wipes the account).",
    {
      name: emulatorNameSchema,
      deviceProfile: z.string().optional().describe("avdmanager device id (default 'pixel_7_pro')."),
      systemImage: z.string().optional().describe("system-images id (default android-33 google_apis_playstore x86_64)."),
      apkVariant: apkVariantSchema.optional().describe("Defaults to 'personal'."),
    },
    async ({
      name,
      deviceProfile,
      systemImage,
      apkVariant,
    }: {
      name: string;
      deviceProfile?: string;
      systemImage?: string;
      apkVariant?: "personal" | "business";
    }) => {
      requireReady();
      log.info(`web_live_emulator_create: name=${name} variant=${apkVariant ?? "personal"}`);
      try {
        const record = await emulatorRegistry.createEmulator({
          name,
          deviceProfile,
          systemImage,
          apkVariant,
        });
        return {
          content: [{ type: "text" as const, text: JSON.stringify(record, null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_emulator_create: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_emulator_start",
    "Boots a registered emulator. Headless by default; set headless:false to show the emulator window for debugging the registration flow.",
    {
      name: emulatorNameSchema,
      headless: z.boolean().optional().default(true),
      bootTimeoutMs: z.number().optional().describe("Reserved; current implementation waits indefinitely for boot."),
    },
    async ({ name, headless, bootTimeoutMs }: { name: string; headless: boolean; bootTimeoutMs?: number }) => {
      requireReady();
      log.info(`web_live_emulator_start: name=${name} headless=${headless}`);
      try {
        const record = await emulatorRegistry.startEmulator(name, { headless, bootTimeoutMs });
        return {
          content: [{ type: "text" as const, text: JSON.stringify(record, null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_emulator_start: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_emulator_stop",
    "Stops a running emulator. The AVD and any registered WhatsApp account are preserved.",
    { name: emulatorNameSchema },
    async ({ name }: { name: string }) => {
      requireReady();
      log.info(`web_live_emulator_stop: name=${name}`);
      try {
        await emulatorRegistry.stopEmulator(name);
        return {
          content: [{ type: "text" as const, text: JSON.stringify({ stopped: name }, null, 2) }],
        };
      } catch (error) {
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_emulator_delete",
    "DESTRUCTIVE. Stops the emulator, deletes the AVD on disk, and removes the persistent record. The registered WhatsApp account is lost.",
    {
      name: emulatorNameSchema,
      confirm: z.literal(true).describe("Must be true; deleting a registered emulator costs a future verification."),
    },
    async ({ name }: { name: string; confirm: true }) => {
      requireReady();
      log.info(`web_live_emulator_delete: name=${name}`);
      try {
        await emulatorRegistry.deleteEmulator(name);
        return {
          content: [{ type: "text" as const, text: JSON.stringify({ deleted: name }, null, 2) }],
        };
      } catch (error) {
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_emulator_info",
    "Read-only. Returns the full record for one emulator.",
    { name: emulatorNameSchema },
    async ({ name }: { name: string }) => {
      requireReady();
      try {
        const emulator = requireEmulator(name);
        return {
          content: [{ type: "text" as const, text: JSON.stringify(emulator.getRecord(), null, 2) }],
        };
      } catch (error) {
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_emulator_install_apk",
    "Installs (or reinstalls) the WhatsApp APK on a running emulator. Switching variant invalidates the current account.",
    {
      name: emulatorNameSchema,
      variant: apkVariantSchema.optional().describe("Defaults to the emulator's recorded variant."),
    },
    async ({ name, variant }: { name: string; variant?: "personal" | "business" }) => {
      requireReady();
      log.info(`web_live_emulator_install_apk: name=${name} variant=${variant ?? "(current)"}`);
      try {
        const emulator = requireEmulator(name);
        await emulator.installApk(variant);
        return {
          content: [{ type: "text" as const, text: JSON.stringify(emulator.getRecord(), null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_emulator_install_apk: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_emulator_register_whatsapp",
    "Registers WhatsApp on a booted emulator. If 'phone' is omitted, a number is rented from the configured SMS verification provider (TEXTVERIFIED_API_KEY required). Costs real money per run; refuses to re-register a 'registered' emulator unless force:true.",
    {
      name: emulatorNameSchema,
      phone: z.string().optional().describe("Optional E.164 phone number. When omitted, one is rented from the SMS provider."),
      countryCode: z.string().optional().describe("Optional ISO country hint for number selection."),
      force: z.boolean().optional().describe("Required when re-registering an already-registered emulator."),
    },
    async ({ name, phone, countryCode, force }: { name: string; phone?: string; countryCode?: string; force?: boolean }) => {
      requireReady();
      log.info(`web_live_emulator_register_whatsapp: name=${name} phoneProvided=${!!phone}`);
      try {
        const emulator = requireEmulator(name);
        const result = await emulator.registerWhatsApp({ name, phone, countryCode, force });
        return {
          content: [{ type: "text" as const, text: JSON.stringify({ result, emulator: emulator.getRecord() }, null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_emulator_register_whatsapp: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );

  server.tool(
    "web_live_emulator_enter_pairing_code",
    "Types a pairing code on a running emulator's linked-devices screen to link a browser session to this emulator's WhatsApp account. Pass the code shown by the browser session.",
    {
      name: emulatorNameSchema,
      code: z.string().describe("Pairing code shown by the browser session (whitespace and dashes are stripped)."),
      packageName: z.string().optional().describe("Override package name; defaults to the emulator's variant."),
      stepTimeoutMs: z.number().optional().describe("Per-step UI timeout."),
    },
    async ({
      name,
      code,
      packageName,
      stepTimeoutMs,
    }: {
      name: string;
      code: string;
      packageName?: string;
      stepTimeoutMs?: number;
    }) => {
      requireReady();
      log.info(`web_live_emulator_enter_pairing_code: name=${name}`);
      try {
        const emulator = requireEmulator(name);
        const result = await emulator.enterPairingCode({ name, code, packageName, stepTimeoutMs });
        return {
          content: [{ type: "text" as const, text: JSON.stringify(result, null, 2) }],
        };
      } catch (error) {
        log.error(`web_live_emulator_enter_pairing_code: ${error instanceof Error ? error.message : String(error)}`);
        return {
          content: [{ type: "text" as const, text: error instanceof Error ? error.message : String(error) }],
          isError: true,
        };
      }
    }
  );
}
