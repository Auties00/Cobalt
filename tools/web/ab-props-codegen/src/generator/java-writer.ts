import { mkdir, writeFile } from "node:fs/promises";
import { join } from "node:path";
import type { ABPropDef } from "../parser/types.js";

/** Converts a dotted Java package name to a directory path. */
function packageToPath(pkg: string): string {
    return pkg.replace(/\./g, "/");
}

/** Converts a snake_case name to UPPER_SNAKE_CASE. */
function toUpperSnakeCase(name: string): string {
    return name.toUpperCase();
}

/**
 * Generates the complete ABProp.java source with all extracted constants.
 */
function generateABPropJava(props: readonly ABPropDef[], pkg: string): string {
    const lines: string[] = [];

    lines.push(`package ${pkg};`);
    lines.push("");
    lines.push("import java.util.Objects;");
    lines.push("import java.util.OptionalDouble;");
    lines.push("import java.util.OptionalInt;");
    lines.push("import java.util.OptionalLong;");
    lines.push("");
    lines.push("/**");
    lines.push(" * Represents an A/B testing property (AB prop) definition with its configuration code and default values.");
    lines.push(" *");
    lines.push(" * <p>AB props are feature flags and configuration values that WhatsApp uses to control client behavior,");
    lines.push(" * enable/disable features, and conduct A/B testing experiments. Each prop definition consists of:");
    lines.push(" * <ul>");
    lines.push(" * <li>A numeric {@code code} that uniquely identifies the property");
    lines.push(" * <li>A {@code defaultValue} string used when the server has not sent a value for this prop");
    lines.push(" * <li>A {@code debugDefaultValue} string used in place of {@code defaultValue} when the user");
    lines.push(" *     has joined the WhatsApp Web Beta programme");
    lines.push(" * </ul>");
    lines.push(" *");
    lines.push(" * <p>Both default values are always strings, matching the format in which values are received from");
    lines.push(" * the server. Static conversion methods are provided to parse the string into typed values");
    lines.push(" * (boolean, int, long, double).");
    lines.push(" *");
    lines.push(" * <p>This record is immutable and thread-safe.");
    lines.push(" *");
    lines.push(" * <p>The constants in this record are generated automatically by {@code tools/web/ab-props-codegen}");
    lines.push(" * from the {@code WAWebABPropsConfigs} module; each constant's documentation records its source");
    lines.push(" * definition. Do not edit the constants manually.");
    lines.push(" *");
    lines.push(" * @param code              the unique numeric identifier for this configuration property");
    lines.push(" * @param defaultValue      the production default value to use when the server has not provided");
    lines.push(" *                          a value for this property, must not be {@code null}");
    lines.push(" * @param debugDefaultValue the debug/beta default value used when the user has joined the");
    lines.push(" *                          WhatsApp Web Beta programme, must not be {@code null}");
    lines.push(" */");
    lines.push("public record ABProp(int code, String defaultValue, String debugDefaultValue) {");

    // Generate constants sorted by code for stable output
    const sorted = [...props].sort((a, b) => a.code - b.code);

    for (const prop of sorted) {
        const constName = toUpperSnakeCase(prop.name);
        const escapedDefault = prop.defaultValue
            .replace(/\\/g, "\\\\")
            .replace(/"/g, '\\"');
        const escapedDebugDefault = prop.debugDefaultValue
            .replace(/\\/g, "\\\\")
            .replace(/"/g, '\\"');
        lines.push("");
        lines.push("    /**");
        lines.push(`     * The {@code ${prop.name}} AB prop.`);
        lines.push("     *");
        lines.push(`     * <p>Source definition in {@code WAWebABPropsConfigs}: ${prop.sourceDefinition}`);
        lines.push("     */");
        lines.push(`    public static final ABProp ${constName} = new ABProp(${prop.code}, "${escapedDefault}", "${escapedDebugDefault}");`);
    }

    lines.push("");
    lines.push("    /**");
    lines.push("     * Constructs a new {@code ABProp} definition.");
    lines.push("     *");
    lines.push("     * @throws NullPointerException if {@code defaultValue} or {@code debugDefaultValue}");
    lines.push("     *         is {@code null}");
    lines.push("     */");
    lines.push("    public ABProp {");
    lines.push('        Objects.requireNonNull(defaultValue, "defaultValue cannot be null");');
    lines.push('        Objects.requireNonNull(debugDefaultValue, "debugDefaultValue cannot be null");');
    lines.push("    }");
    lines.push("");
    lines.push("    /**");
    lines.push("     * Converts a string value to a boolean.");
    lines.push("     *");
    lines.push("     * <p>The following values are considered {@code true}: {@code \"1\"}, {@code \"True\"},");
    lines.push("     * and {@code \"true\"}. All other values are considered {@code false}.");
    lines.push("     *");
    lines.push("     * @param value the string value to convert");
    lines.push("     * @return the boolean representation of the given value");
    lines.push("     */");
    lines.push("    public static boolean toBoolean(String value) {");
    lines.push('        return "1".equals(value)');
    lines.push('                || "True".equals(value)');
    lines.push('                || "true".equals(value);');
    lines.push("    }");
    lines.push("");
    lines.push("    /**");
    lines.push("     * Attempts to convert a string value to an integer.");
    lines.push("     *");
    lines.push("     * @param value the string value to parse");
    lines.push("     * @return an {@link OptionalInt} containing the integer value if parsing succeeds,");
    lines.push("     *         or empty if it fails");
    lines.push("     */");
    lines.push("    public static OptionalInt toInt(String value) {");
    lines.push("        try {");
    lines.push("            return OptionalInt.of(Integer.parseInt(value));");
    lines.push("        } catch (NumberFormatException exception) {");
    lines.push("            return OptionalInt.empty();");
    lines.push("        }");
    lines.push("    }");
    lines.push("");
    lines.push("    /**");
    lines.push("     * Attempts to convert a string value to a long.");
    lines.push("     *");
    lines.push("     * @param value the string value to parse");
    lines.push("     * @return an {@link OptionalLong} containing the long value if parsing succeeds,");
    lines.push("     *         or empty if it fails");
    lines.push("     */");
    lines.push("    public static OptionalLong toLong(String value) {");
    lines.push("        try {");
    lines.push("            return OptionalLong.of(Long.parseLong(value));");
    lines.push("        } catch (NumberFormatException exception) {");
    lines.push("            return OptionalLong.empty();");
    lines.push("        }");
    lines.push("    }");
    lines.push("");
    lines.push("    /**");
    lines.push("     * Attempts to convert a string value to a double (floating-point).");
    lines.push("     *");
    lines.push("     * @param value the string value to parse");
    lines.push("     * @return an {@link OptionalDouble} containing the double value if parsing succeeds,");
    lines.push("     *         or empty if it fails");
    lines.push("     */");
    lines.push("    public static OptionalDouble toDouble(String value) {");
    lines.push("        try {");
    lines.push("            return OptionalDouble.of(Double.parseDouble(value));");
    lines.push("        } catch (NumberFormatException exception) {");
    lines.push("            return OptionalDouble.empty();");
    lines.push("        }");
    lines.push("    }");
    lines.push("");
    lines.push("    @Override");
    lines.push("    public String toString() {");
    lines.push('        return "ABProp[code=%d, defaultValue=\'%s\', debugDefaultValue=\'%s\']"');
    lines.push("                .formatted(code, defaultValue, debugDefaultValue);");
    lines.push("    }");
    lines.push("}");
    lines.push("");

    return lines.join("\n");
}

/**
 * Writes the ABProp.java file with all extracted constants.
 *
 * @returns the number of constants written
 */
export async function writeABPropJava(
    props: readonly ABPropDef[],
    outputDir: string,
    pkg: string,
): Promise<number> {
    const dir = join(outputDir, packageToPath(pkg));
    await mkdir(dir, { recursive: true });

    const filePath = join(dir, "ABProp.java");
    await writeFile(filePath, generateABPropJava(props, pkg));

    return props.length;
}
