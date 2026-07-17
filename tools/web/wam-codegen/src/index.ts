import { parseArgs } from "node:util";
import { extractWamDefinitions } from "./whatsapp/wam-extractor.js";
import { writeEnums } from "./generator/java-enum-writer.js";
import { writeEvents } from "./generator/java-event-writer.js";

const { values: args } = parseArgs({
    options: {
        "output-dir": { type: "string", default: "../../../modules/lib/src/main/java" },
        "package":    { type: "string", default: "com.github.auties00.cobalt.wam" },
    },
});

const outputDir = args["output-dir"]!;
const pkg = args["package"]!;

async function main(): Promise<void> {
    console.log("=".repeat(50));
    console.log("WhatsApp WAM Definition Extractor");
    console.log("=".repeat(50));
    console.log(`Output dir : ${outputDir}`);
    console.log(`Package    : ${pkg}`);
    console.log("");

    const { enums, events } = await extractWamDefinitions();

    console.log(`\nFound ${enums.length} WAM enums`);
    for (const e of enums) {
        console.log(`  ${e.javaName}: ${e.constants.length} constants`);
    }

    console.log(`\nFound ${events.length} WAM events`);
    for (const ev of events) {
        const extras: string[] = [];
        if (ev.channel !== "regular") extras.push(`channel=${ev.channel}`);
        if (ev.weights.release !== 1) extras.push(`weight=${ev.weights.release}`);
        if (ev.privateStatsId >= 0) extras.push(`psId=${ev.privateStatsId}`);
        const suffix = extras.length > 0 ? ` (${extras.join(", ")})` : "";
        console.log(`  ${ev.javaClassName}: ${ev.fields.length} fields, id=${ev.eventId}${suffix}`);
    }

    console.log("\nGenerating Java enum files...");
    const enumCount = await writeEnums(enums, outputDir, pkg);
    console.log(`Wrote ${enumCount} enum files`);

    console.log("\nGenerating Java event files...");
    const eventCount = await writeEvents(events, outputDir, pkg);
    console.log(`Wrote ${eventCount} event files`);

    console.log("\n" + "=".repeat(50));
    console.log(`Done! Generated ${enumCount} enums and ${eventCount} events.`);
    console.log("=".repeat(50));
}

main().catch((err: unknown) => {
    const message = err instanceof Error ? err.stack ?? err.message : String(err);
    console.error(`[FATAL] ${message}`);
    process.exit(1);
});
