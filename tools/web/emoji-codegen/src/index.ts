import { parseArgs } from "node:util";
import { readFile, writeFile } from "node:fs/promises";
import { extractRawCatalog } from "./whatsapp/emoji-extractor.js";
import { fetchUnicodeNames } from "./whatsapp/unicode-names.js";
import { resolveCatalog } from "./parser/resolve.js";
import { writeJavaSources } from "./generator/java-writer.js";
import { writeResources } from "./generator/resource-writer.js";
import type { EmojiCatalog } from "./parser/types.js";

const { values: args } = parseArgs({
  options: {
    "output-dir": { type: "string", default: "../../../modules/lib/src/main/java" },
    "resource-dir": { type: "string", default: "../../../modules/lib/src/main/resources" },
    package: { type: "string", default: "com.github.auties00.cobalt.emoji" },
    // "latest" tracks the newest released Unicode emoji set (currently 17.0); pin a number for reproducibility.
    "unicode-version": { type: "string", default: "latest" },
    // Writes the resolved catalog to this path instead of (or before) generating, for inspection.
    "dump-json": { type: "string" },
    // Regenerates from a previously dumped catalog, skipping the browser and the Unicode fetch.
    "from-json": { type: "string" },
  },
});

const outputDir = args["output-dir"]!;
const resourceDir = args["resource-dir"]!;
const pkg = args["package"]!;

async function buildCatalog(): Promise<EmojiCatalog> {
  if (args["from-json"]) {
    console.log(`[INFO] Loading catalog from ${args["from-json"]} (skipping extraction)`);
    return JSON.parse(await readFile(args["from-json"], "utf-8")) as EmojiCatalog;
  }
  const raw = await extractRawCatalog();
  const names = await fetchUnicodeNames(args["unicode-version"]!);
  return resolveCatalog(raw, names);
}

async function main(): Promise<void> {
  console.log("=".repeat(50));
  console.log("WhatsApp Emoji Catalog Extractor");
  console.log("=".repeat(50));
  console.log(`Output dir   : ${outputDir}`);
  console.log(`Resource dir : ${resourceDir}`);
  console.log(`Package      : ${pkg}`);
  console.log("");

  const catalog = await buildCatalog();

  const skinable = catalog.emojis.filter((e) => e.skinToneable).length;
  const variants = catalog.emojis.reduce((n, e) => n + e.variants.length, 0);
  console.log(`\nEmoji type   : ${catalog.emojiType}`);
  console.log(`Categories   : ${catalog.categories.length} (${catalog.categories.join(", ")})`);
  console.log(`Base emojis  : ${catalog.emojis.length}`);
  console.log(`Skin-toneable: ${skinable} bases, ${variants} variants`);
  console.log(`Legacy forms : ${catalog.legacy.length}`);
  console.log(`Popular list : ${catalog.popular.length}`);
  console.log(`Locales      : ${catalog.locales.length} (${catalog.locales.map((l) => l.tag).join(", ")})`);

  if (catalog.unnamedCount > 0) {
    console.log(
      `\n[RECONCILE] ${catalog.unnamedCount} base emoji(s) had no Unicode name for emoji ` +
        `${args["unicode-version"]}; they fell back to a code-point constant (EMOJI_<hex>). ` +
        `Bump --unicode-version if the WhatsApp set is newer than the fetched data.`,
    );
  }

  if (args["dump-json"]) {
    await writeFile(args["dump-json"], JSON.stringify(catalog, null, 2), "utf-8");
    console.log(`\nWrote catalog JSON to ${args["dump-json"]}`);
  }

  console.log("\nGenerating Java sources...");
  const count = await writeJavaSources(catalog, outputDir, pkg);
  console.log(
    `Wrote WhatsAppEmoji.java (${count} constants), EmojiCategory.java, EmojiSkinTone.java, EmojiRegistry.java`,
  );

  console.log("\nWriting binary resources...");
  const files = await writeResources(catalog, resourceDir, pkg);
  console.log(`Wrote ${files.length} resource file(s): ${files.join(", ")}`);

  console.log("\n" + "=".repeat(50));
  console.log(`Done! ${count} emoji constants, ${catalog.locales.length} locale dictionaries.`);
  console.log("=".repeat(50));
}

main().catch((err: unknown) => {
  const message = err instanceof Error ? (err.stack ?? err.message) : String(err);
  console.error(`[FATAL] ${message}`);
  process.exit(1);
});
