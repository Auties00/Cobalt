import {mkdir, readdir, unlink, writeFile} from "node:fs/promises";
import {join} from "node:path";
import {packageToPath} from "./naming.js";
import type {WamEventDef, WamFieldDef} from "../parser/types.js";

// Java type mappings

function returnType(field: WamFieldDef): string {
    switch (field.wamType) {
        case "INTEGER":
            return "OptionalLong";
        case "FLOAT":
            return "OptionalDouble";
        case "ENUM":
            return `Optional<${field.enumJavaName!}>`;
        case "BOOLEAN":
            return "Optional<Boolean>";
        case "STRING":
            return "Optional<String>";
        case "TIMER":
            return "Optional<Instant>";
    }
}

// Import collection helpers

function collectEnumImports(fields: readonly WamFieldDef[]): string[] {
    const names = new Set<string>();
    for (const f of fields) {
        if (f.wamType === "ENUM" && f.enumJavaName) names.add(f.enumJavaName);
    }
    return [...names].sort();
}

function needsInstant(fields: readonly WamFieldDef[]): boolean {
    return fields.some((f) => f.wamType === "TIMER");
}

function collectOptionalImports(fields: readonly WamFieldDef[]): string[] {
    const imports = new Set<string>();
    for (const f of fields) {
        switch (f.wamType) {
            case "INTEGER":
                imports.add("java.util.OptionalLong");
                break;
            case "FLOAT":
                imports.add("java.util.OptionalDouble");
                break;
            default:
                imports.add("java.util.Optional");
                break;
        }
    }
    return [...imports].sort();
}

// Annotation builder

function buildAnnotation(event: WamEventDef): string {
    const attrs: string[] = [`id = ${event.eventId}`];

    if (event.channel !== "regular") {
        attrs.push(`channel = WamChannel.${event.channel.toUpperCase()}`);
    }
    if (event.weights.alpha !== 1) attrs.push(`alphaWeight = ${event.weights.alpha}`);
    if (event.weights.beta !== 1) attrs.push(`betaWeight = ${event.weights.beta}`);
    if (event.weights.release !== 1) attrs.push(`releaseWeight = ${event.weights.release}`);
    if (event.privateStatsId >= 0) attrs.push(`privateStatsId = ${event.privateStatsId}`);

    return `@WamEvent(${attrs.join(", ")})`;
}

// Full interface generation

function generateEvent(event: WamEventDef, pkg: string): string {
    const cls = event.javaClassName;
    const fields = event.fields;
    const out: string[] = [];

    const enumImports = collectEnumImports(fields);
    const optImports = collectOptionalImports(fields);
    const hasTimerFields = needsInstant(fields);
    const usesChannel = event.channel !== "regular";

    out.push(`package ${pkg}.event;`);
    out.push("");

    out.push("import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;");
    out.push("");

    out.push(`import ${pkg}.annotation.WamEvent;`);
    out.push(`import ${pkg}.model.WamEventSpec;`);
    out.push(`import ${pkg}.annotation.WamProperty;`);
    if (usesChannel) out.push(`import ${pkg}.model.WamChannel;`);
    out.push(`import ${pkg}.model.WamType;`);

    for (const name of enumImports) out.push(`import ${pkg}.type.${name};`);

    out.push("");

    if (hasTimerFields) out.push("import java.time.Instant;");

    for (const imp of optImports) out.push(`import ${imp};`);

    out.push("");

    out.push(`@WhatsAppWebModule(moduleName = "${event.moduleName}")`);
    out.push(buildAnnotation(event));
    out.push(`public interface ${cls} extends WamEventSpec {`);

    for (let i = 0; i < fields.length; i++) {
        const f = fields[i];
        out.push(`    @WamProperty(index = ${f.id}, type = WamType.${f.wamType})`);
        out.push(`    ${returnType(f)} ${f.name}();`);
        if (i < fields.length - 1) out.push("");
    }

    out.push("}");
    out.push("");

    return out.join("\n");
}

// Public API

/**
 * Writes all WAM event definitions as Java source files, pruning
 * any pre-existing {@code *Event.java} that is no longer in the
 * fresh definition set.
 *
 * <p>Pruning is unconditional: every run treats {@code events} as
 * the authoritative snapshot. If WhatsApp Web retires an event
 * between runs, the stale interface is deleted so the annotation
 * processor stops emitting a {@code *Impl} for an id that no longer
 * exists in the live bundle.
 *
 * @returns the number of files written
 */
export async function writeEvents(
    events: readonly WamEventDef[],
    outputDir: string,
    pkg: string,
): Promise<number> {
    const dir = join(outputDir, packageToPath(pkg), "event");
    await mkdir(dir, {recursive: true});

    const expected = new Set(events.map((e) => `${e.javaClassName}.java`));
    await pruneStaleJavaFiles(dir, expected, "event");

    let written = 0;
    for (const event of events) {
        const filePath = join(dir, `${event.javaClassName}.java`);
        await writeFile(filePath, generateEvent(event, pkg));
        written++;
    }

    return written;
}

/**
 * Deletes every {@code .java} file in {@code dir} that is not in
 * {@code expected}. Files with other extensions are left alone so
 * an editor's swap files or sibling artefacts (e.g. a hand-rolled
 * {@code package-info.java}) are not collateral damage.
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
        // Directory may not exist on the first run; mkdir above would
        // have created it, but the readdir is still a no-op for an
        // empty new directory.
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
