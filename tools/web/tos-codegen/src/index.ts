import { parseArgs } from "node:util";
import { extractTosNotices } from "./whatsapp/tos-extractor.js";
import { writeTosNoticeJava } from "./generator/java-writer.js";

const { values: args } = parseArgs({
    options: {
        "output-dir": { type: "string", default: "../../../modules/model/src/main/java" },
        "package":    { type: "string", default: "com.github.auties00.cobalt.model.tos" },
    },
});

const outputDir = args["output-dir"]!;
const pkg = args["package"]!;

async function main(): Promise<void> {
    console.log("=".repeat(50));
    console.log("WhatsApp TOS Notice Extractor");
    console.log("=".repeat(50));
    console.log(`Output dir : ${outputDir}`);
    console.log(`Package    : ${pkg}`);
    console.log("");

    const { notices, unmatchedProps } = await extractTosNotices();
    console.log(`\nFound ${notices.length} TOS notices`);
    for (const notice of notices) {
        const parts: string[] = [];
        if (notice.defaultId !== null) parts.push(`id=${notice.defaultId}`);
        if (notice.webProp !== null) parts.push(`web=${notice.webProp}`);
        if (notice.smbProp !== null) parts.push(`smb=${notice.smbProp}`);
        if (notice.multiValued) parts.push("list");
        console.log(`  ${notice.name.padEnd(28)} ${parts.join(", ")}`);
    }

    if (unmatchedProps.length > 0) {
        console.log(
            `\n[RECONCILE] ${unmatchedProps.length} notice-id-shaped AB-prop(s) referenced by no extracted notice ` +
            `(investigate - a new TOS notice may need wiring):`,
        );
        for (const prop of unmatchedProps) console.log(`  - ${prop}`);
    }

    console.log("\nGenerating TosNotice.java...");
    const count = await writeTosNoticeJava(notices, outputDir, pkg);
    console.log(`Wrote TosNotice.java with ${count} constants`);

    console.log("\n" + "=".repeat(50));
    console.log(`Done! Generated ${count} TOS notice constants.`);
    console.log("=".repeat(50));
}

main().catch((err: unknown) => {
    const message = err instanceof Error ? err.stack ?? err.message : String(err);
    console.error(`[FATAL] ${message}`);
    process.exit(1);
});
