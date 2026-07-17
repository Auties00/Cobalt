import { parseArgs } from "node:util";
import { extractABProps } from "./whatsapp/ab-props-extractor.js";
import { writeABPropJava } from "./generator/java-writer.js";

const { values: args } = parseArgs({
    options: {
        "output-dir": { type: "string", default: "../../../modules/model/src/main/java" },
        "package":    { type: "string", default: "com.github.auties00.cobalt.model.props" },
    },
});

const outputDir = args["output-dir"]!;
const pkg = args["package"]!;

async function main(): Promise<void> {
    console.log("=".repeat(50));
    console.log("WhatsApp AB Props Extractor");
    console.log("=".repeat(50));
    console.log(`Output dir : ${outputDir}`);
    console.log(`Package    : ${pkg}`);
    console.log("");

    const props = await extractABProps();
    console.log(`\nFound ${props.length} AB props`);

    const typeCounts = { bool: 0, int: 0, float: 0, string: 0 };
    for (const prop of props) {
        typeCounts[prop.type]++;
    }
    console.log(`  bool: ${typeCounts.bool}, int: ${typeCounts.int}, float: ${typeCounts.float}, string: ${typeCounts.string}`);

    console.log("\nGenerating ABProp.java...");
    const count = await writeABPropJava(props, outputDir, pkg);
    console.log(`Wrote ABProp.java with ${count} constants`);

    console.log("\n" + "=".repeat(50));
    console.log(`Done! Generated ${count} AB prop constants.`);
    console.log("=".repeat(50));
}

main().catch((err: unknown) => {
    const message = err instanceof Error ? err.stack ?? err.message : String(err);
    console.error(`[FATAL] ${message}`);
    process.exit(1);
});
