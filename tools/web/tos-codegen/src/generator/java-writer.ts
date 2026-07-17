import { mkdir, writeFile } from "node:fs/promises";
import { join } from "node:path";
import type { TosNoticeDef } from "../parser/types.js";

/** Converts a dotted Java package name to a directory path. */
function packageToPath(pkg: string): string {
    return pkg.replace(/\./g, "/");
}

/** Maps a snake_case AB-prop name to its generated {@code ABProp} constant name. */
function abPropConstant(prop: string | null): string {
    return prop === null ? "null" : `ABProp.${prop.toUpperCase()}`;
}

/** Renders a nullable id string as a Java literal. */
function idLiteral(id: string | null): string {
    return id === null ? "null" : `"${id.replace(/\\/g, "\\\\").replace(/"/g, '\\"')}"`;
}

/**
 * Generates the complete TosNotice.java source with all extracted constants.
 */
function generateTosNoticeJava(notices: readonly TosNoticeDef[], pkg: string): string {
    const lines: string[] = [];

    lines.push(`package ${pkg};`);
    lines.push("");
    lines.push("import com.github.auties00.cobalt.model.props.ABProp;");
    lines.push("");
    lines.push("import java.util.Objects;");
    lines.push("");
    lines.push("/**");
    lines.push(" * Represents a WhatsApp Terms-of-Service notice definition, identified by the");
    lines.push(" * stable notice id the relay echoes in the {@code w:tos} acceptance protocol.");
    lines.push(" *");
    lines.push(" * <p>A TOS notice is a versioned agreement (interoperability terms, bot terms,");
    lines.push(" * marketing-message disclosure, newsletter terms, and so on) whose per-user");
    lines.push(" * acceptance state the client tracks and, for some surfaces, gates behaviour on.");
    lines.push(" * The notice id is resolved at runtime by the {@code TosService} from this");
    lines.push(" * definition:");
    lines.push(" * <ul>");
    lines.push(" *   <li>the active AB-prop is {@link #smbProp} when the linked device is a");
    lines.push(" *       WhatsApp Business (SMB) client and it is set, otherwise {@link #webProp};</li>");
    lines.push(" *   <li>the resolved id is the active AB-prop's value when it is non-blank,");
    lines.push(" *       otherwise the static {@link #defaultId};</li>");
    lines.push(" *   <li>when {@link #multiValued} is {@code true} the value is a comma-separated");
    lines.push(" *       list of ids (a notice group) rather than a single id.</li>");
    lines.push(" * </ul>");
    lines.push(" *");
    lines.push(" * <p>This record is immutable and thread-safe.");
    lines.push(" *");
    lines.push(" * <p>The constants in this record are generated automatically by");
    lines.push(" * {@code tools/web/tos-codegen} from the WhatsApp Web TOS modules; each");
    lines.push(" * constant's documentation records its source definition. Do not edit the");
    lines.push(" * constants manually.");
    lines.push(" *");
    lines.push(" * @param defaultId    the static notice id baked into the source, or {@code null}");
    lines.push(" *                     when the id is supplied entirely by an AB-prop");
    lines.push(" * @param webProp      the AB-prop supplying or overriding the id for the web");
    lines.push(" *                     (non-SMB) client, or {@code null} when the id is fixed");
    lines.push(" * @param smbProp      the AB-prop supplying the id for the SMB (Business) client,");
    lines.push(" *                     or {@code null} when the notice has no SMB variant");
    lines.push(" * @param multiValued  whether the resolved AB-prop value is a comma-separated list");
    lines.push(" *                     of ids rather than a single id");
    lines.push(" */");
    lines.push("public record TosNotice(String defaultId, ABProp webProp, ABProp smbProp, boolean multiValued) {");

    const sorted = [...notices].sort((a, b) => a.name.localeCompare(b.name));
    for (const notice of sorted) {
        lines.push("");
        lines.push("    /**");
        lines.push(`     * The ${notice.name} Terms-of-Service notice.`);
        lines.push("     *");
        lines.push(`     * <p>Source: {@code ${notice.source}}.`);
        lines.push("     */");
        lines.push(
            `    public static final TosNotice ${notice.name} = new TosNotice(${idLiteral(notice.defaultId)}, ` +
            `${abPropConstant(notice.webProp)}, ${abPropConstant(notice.smbProp)}, ${notice.multiValued});`,
        );
    }

    lines.push("");
    lines.push("    /**");
    lines.push("     * Constructs a new {@code TosNotice} definition.");
    lines.push("     *");
    lines.push("     * @throws NullPointerException if both {@code defaultId} and {@code webProp}");
    lines.push("     *         are {@code null}, leaving the notice with no id source");
    lines.push("     */");
    lines.push("    public TosNotice {");
    lines.push("        if (defaultId == null) {");
    lines.push('            Objects.requireNonNull(webProp, "a notice with no defaultId must have a webProp");');
    lines.push("        }");
    lines.push("    }");
    lines.push("}");
    lines.push("");

    return lines.join("\n");
}

/**
 * Writes the TosNotice.java file with all extracted constants.
 *
 * @returns the number of constants written
 */
export async function writeTosNoticeJava(
    notices: readonly TosNoticeDef[],
    outputDir: string,
    pkg: string,
): Promise<number> {
    const dir = join(outputDir, packageToPath(pkg));
    await mkdir(dir, { recursive: true });

    const filePath = join(dir, "TosNotice.java");
    await writeFile(filePath, generateTosNoticeJava(notices, pkg));

    return notices.length;
}
