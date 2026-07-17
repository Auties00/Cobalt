import { mkdir, writeFile } from "node:fs/promises";
import { join } from "node:path";
import type { EmojiCatalog } from "../parser/types.js";

/**
 * A growable big-endian binary buffer. Strings are written as a 4-byte length
 * prefix followed by their UTF-8 bytes (NOT Java modified UTF-8), so the Java
 * reader is a plain {@code DataInputStream.readInt()} + {@code readFully} +
 * {@code new String(bytes, UTF_8)} and supplementary-plane emoji round-trip
 * cleanly.
 */
class BinaryWriter {
  private readonly chunks: Buffer[] = [];

  int(value: number): this {
    const b = Buffer.allocUnsafe(4);
    b.writeInt32BE(value, 0);
    this.chunks.push(b);
    return this;
  }

  str(value: string): this {
    const bytes = Buffer.from(value, "utf-8");
    this.int(bytes.length);
    this.chunks.push(bytes);
    return this;
  }

  toBuffer(): Buffer {
    return Buffer.concat(this.chunks);
  }
}

function packageToPath(pkg: string): string {
  return pkg.replace(/\./g, "/");
}

/**
 * Writes the binary lookup resources the {@code WhatsAppEmoji} facade loads at
 * runtime: the legacy-form map, the skin-tone variant map, one keyword
 * dictionary per locale, and a plaintext index of the available locale tags.
 *
 * @returns the list of written file names (relative to the package directory)
 */
export async function writeResources(
  catalog: EmojiCatalog,
  resourceBaseDir: string,
  pkg: string,
): Promise<string[]> {
  const dir = join(resourceBaseDir, packageToPath(pkg));
  await mkdir(dir, { recursive: true });
  const written: string[] = [];

  const legacy = new BinaryWriter().int(catalog.legacy.length);
  for (const m of catalog.legacy) legacy.str(m.form).str(m.canonical);
  await writeFile(join(dir, "emoji_legacy.bin"), legacy.toBuffer());
  written.push("emoji_legacy.bin");

  const skinable = catalog.emojis.filter((e) => e.variants.length > 0);
  const skin = new BinaryWriter().int(skinable.length);
  for (const e of skinable) {
    skin.str(e.value).int(e.variants.length);
    for (const v of e.variants) skin.str(v.toneKey).str(v.value);
  }
  await writeFile(join(dir, "emoji_skintones.bin"), skin.toBuffer());
  written.push("emoji_skintones.bin");

  const popular = new BinaryWriter().int(catalog.popular.length);
  for (const value of catalog.popular) popular.str(value);
  await writeFile(join(dir, "emoji_popular.bin"), popular.toBuffer());
  written.push("emoji_popular.bin");

  const tags: string[] = [];
  for (const locale of [...catalog.locales].sort((a, b) => a.tag.localeCompare(b.tag))) {
    const kw = new BinaryWriter().int(locale.entries.length);
    for (const entry of locale.entries) {
      kw.str(entry.keyword).int(entry.values.length);
      for (const value of entry.values) kw.str(value);
    }
    const fileName = `emoji_keywords_${locale.tag}.bin`;
    await writeFile(join(dir, fileName), kw.toBuffer());
    written.push(fileName);
    tags.push(locale.tag);
  }

  await writeFile(join(dir, "emoji_keywords.index"), tags.join("\n") + "\n", "utf-8");
  written.push("emoji_keywords.index");

  return written;
}
