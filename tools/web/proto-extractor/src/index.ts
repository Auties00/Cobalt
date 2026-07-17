import { parseArgs } from "node:util";
import { fetchWhatsAppWeb } from "./fetch/index.js";
import { parseProtoModules } from "./parse-js/index.js";
import { parseProtobufCFromWasm } from "./parse-wasm/index.js";
import { writeProtoFile } from "./emit/index.js";
import { countTopLevel, mergeProtos } from "./merge.js";
import type { ParsedProtos } from "./types.js";

const DEFAULT_OUTPUT = "./whatsapp.proto";
const DEFAULT_PACKAGE = "com.github.auties00.whatsapp.model.unsupported";

interface CliOptions {
    readonly outputPath: string;
    readonly pkg: string;
}

function parseCli(): CliOptions {
    const { values } = parseArgs({
        options: {
            "output": { type: "string", default: DEFAULT_OUTPUT },
            "package": { type: "string", default: DEFAULT_PACKAGE },
        },
    });
    return { outputPath: values["output"]!, pkg: values["package"]! };
}

function logHeader({ outputPath, pkg }: CliOptions): void {
    const bar = "=".repeat(50);
    console.log(bar);
    console.log("WhatsApp Proto Extractor");
    console.log(bar);
    console.log(`Output file : ${outputPath}`);
    console.log(`Package     : ${pkg}`);
    console.log("");
}

async function main(): Promise<void> {
    const options = parseCli();
    logHeader(options);

    const { version, chunks, wasmBinaries } = await fetchWhatsAppWeb();
    const jsBytes = chunks.reduce((acc, c) => acc + c.content.length, 0);
    const wasmBytes = wasmBinaries.reduce((acc, b) => acc + b.data.length, 0);
    console.log(`\nJS content   : ${(jsBytes / 1_000_000).toFixed(1)} MB across ${chunks.length} chunks`);
    console.log(`Wasm content : ${(wasmBytes / 1_000_000).toFixed(1)} MB across ${wasmBinaries.length} modules`);

    console.log("\nParsing JS proto modules...");
    const jsParsed = parseProtoModules(chunks);
    console.log(`Found ${jsParsed.moduleOrder.length} JS proto modules, ${countTopLevel(jsParsed)} top-level entities`);

    const wasmParts: ParsedProtos[] = [];
    for (const wasm of wasmBinaries) {
        const filename = wasm.url.split("/").pop()?.split("?")[0] ?? wasm.url;
        const label = `wasm:${filename.replace(/\.wasm$/, "")}`;
        const parsed = parseProtobufCFromWasm(wasm.data, label);
        const totalEntities = Object.values(parsed.modulesInfo)
            .flatMap((m) => Object.values(m.identifiers)).length;
        if (totalEntities > 0) {
            console.log(`  ${filename}: ${totalEntities} entities (${countTopLevel(parsed)} top-level)`);
            wasmParts.push(parsed);
        }
    }
    console.log(`Found ${wasmParts.length} wasm modules carrying protobuf-c descriptors`);

    const merged = mergeProtos([jsParsed, ...wasmParts]);

    console.log("\nWriting proto file...");
    await writeProtoFile(merged, version, options.pkg, options.outputPath);

    const bar = "=".repeat(50);
    console.log(`\n${bar}`);
    console.log(`Done! Wrote ${options.outputPath} (WhatsApp Web ${version}).`);
    console.log(bar);
}

main().catch((err: unknown) => {
    const message = err instanceof Error ? err.stack ?? err.message : String(err);
    console.error(`[FATAL] ${message}`);
    process.exit(1);
});
