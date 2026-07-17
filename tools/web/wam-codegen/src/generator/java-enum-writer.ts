import { mkdir, readdir, unlink, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { packageToPath } from "./naming.js";
import type { WamEnumDef } from "../parser/types.js";

function generateEnum(enumDef: WamEnumDef, pkg: string): string {
    const lines: string[] = [];

    lines.push(`package ${pkg}.type;`);
    lines.push("");
    lines.push("import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;");
    lines.push("");
    lines.push(`import ${pkg}.annotation.WamEnum;`);
    lines.push(`import ${pkg}.annotation.WamEnumConstant;`);
    lines.push("");
    lines.push(`@WhatsAppWebModule(moduleName = "${enumDef.moduleName}")`);
    lines.push("@WamEnum");
    lines.push(`public enum ${enumDef.javaName} {`);

    const last = enumDef.constants.length - 1;
    enumDef.constants.forEach((c, i) => {
        const suffix = i < last ? "," : "";
        lines.push(`    @WamEnumConstant(${c.value}) ${c.name}${suffix}`);
    });

    lines.push("}");
    lines.push("");

    return lines.join("\n");
}

/**
 * Writes all WAM enum definitions as Java source files, pruning
 * any pre-existing {@code *.java} that is no longer in the fresh
 * definition set.
 *
 * <p>Pruning is unconditional: every run treats {@code enums} as
 * the authoritative snapshot, so an enum retired by WhatsApp Web
 * between runs is deleted rather than left as orphaned source.
 *
 * @returns the number of files written
 */
export async function writeEnums(
    enums: readonly WamEnumDef[],
    outputDir: string,
    pkg: string,
): Promise<number> {
    const dir = join(outputDir, packageToPath(pkg), "type");
    await mkdir(dir, { recursive: true });

    const expected = new Set(enums.map((e) => `${e.javaName}.java`));
    await pruneStaleJavaFiles(dir, expected, "enum");

    let written = 0;
    for (const enumDef of enums) {
        const filePath = join(dir, `${enumDef.javaName}.java`);
        await writeFile(filePath, generateEnum(enumDef, pkg));
        written++;
    }

    return written;
}

/**
 * Deletes every {@code .java} file in {@code dir} that is not in
 * {@code expected}, leaving non-Java entries alone.
 */
async function pruneStaleJavaFiles(
    dir: string,
    expected: ReadonlySet<string>,
    label: string,
): Promise<void> {
    let entries: string[];
    try {
        entries = await readdir(dir);
    } catch {
        return;
    }
    for (const entry of entries) {
        if (!entry.endsWith(".java")) continue;
        if (expected.has(entry)) continue;
        const filePath = join(dir, entry);
        await unlink(filePath);
        console.log(`  pruned stale ${label}: ${entry}`);
    }
}
