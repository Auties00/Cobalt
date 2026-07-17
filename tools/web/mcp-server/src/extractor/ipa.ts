import { createReadStream } from "node:fs";
import { readFile, mkdir, readdir, writeFile } from "node:fs/promises";
import { join, basename } from "node:path";
import { pipeline } from "node:stream/promises";
import { createWriteStream } from "node:fs";

const FAT_MAGIC = 0xcafebabe;
const MH_MAGIC_64 = 0xfeedface + 1;
const CPU_TYPE_ARM64 = 0x0100000c;
const LC_ENCRYPTION_INFO_64 = 0x2c;
const LC_ENCRYPTION_INFO = 0x21;
const MACH_HEADER_64_SIZE = 32;
const FAT_HEADER_SIZE = 8;
const FAT_ARCH_SIZE = 20;

export interface IpaExtractionResult {
  binaryPath: string;
  bundleId: string | null;
  bundleVersion: string | null;
  executableName: string;
  encrypted: boolean;
  frameworks: string[];
}

export interface MachOInfo {
  isFat: boolean;
  hasArm64: boolean;
  encrypted: boolean;
  arm64Offset: number;
  arm64Size: number;
}

export function parseMachOInfo(buffer: Buffer): MachOInfo {
  const magic = buffer.readUInt32BE(0);

  if (magic === FAT_MAGIC) {
    return parseFatBinary(buffer);
  }

  const leMagic = buffer.readUInt32LE(0);
  if (leMagic === MH_MAGIC_64) {
    const encrypted = checkEncryption(buffer, 0);
    return {
      isFat: false,
      hasArm64: true,
      encrypted,
      arm64Offset: 0,
      arm64Size: buffer.length,
    };
  }

  return { isFat: false, hasArm64: false, encrypted: false, arm64Offset: 0, arm64Size: 0 };
}

function parseFatBinary(buffer: Buffer): MachOInfo {
  const nArch = buffer.readUInt32BE(4);
  for (let i = 0; i < nArch; i++) {
    const archOffset = FAT_HEADER_SIZE + i * FAT_ARCH_SIZE;
    const cpuType = buffer.readUInt32BE(archOffset);
    const offset = buffer.readUInt32BE(archOffset + 8);
    const size = buffer.readUInt32BE(archOffset + 12);

    if (cpuType === CPU_TYPE_ARM64) {
      const encrypted = checkEncryption(buffer, offset);
      return { isFat: true, hasArm64: true, encrypted, arm64Offset: offset, arm64Size: size };
    }
  }
  return { isFat: true, hasArm64: false, encrypted: false, arm64Offset: 0, arm64Size: 0 };
}

function checkEncryption(buffer: Buffer, machoOffset: number): boolean {
  const ncmds = buffer.readUInt32LE(machoOffset + 16);
  let cmdOffset = machoOffset + MACH_HEADER_64_SIZE;

  for (let i = 0; i < ncmds; i++) {
    if (cmdOffset + 8 > buffer.length) break;
    const cmd = buffer.readUInt32LE(cmdOffset);
    const cmdSize = buffer.readUInt32LE(cmdOffset + 4);

    if (cmd === LC_ENCRYPTION_INFO_64 && cmdOffset + 20 <= buffer.length) {
      const cryptId = buffer.readUInt32LE(cmdOffset + 16);
      return cryptId !== 0;
    }
    if (cmd === LC_ENCRYPTION_INFO && cmdOffset + 16 <= buffer.length) {
      const cryptId = buffer.readUInt32LE(cmdOffset + 12);
      return cryptId !== 0;
    }

    cmdOffset += cmdSize;
  }
  return false;
}

export function extractArm64Slice(buffer: Buffer, info: MachOInfo): Buffer {
  if (!info.hasArm64) {
    throw new Error("No arm64 slice found in binary.");
  }
  if (!info.isFat) return buffer;
  return buffer.subarray(info.arm64Offset, info.arm64Offset + info.arm64Size);
}

export async function extractIpa(
  ipaPath: string,
  outputDir: string
): Promise<IpaExtractionResult> {
  const { default: unzipper } = await importUnzipper();

  await mkdir(outputDir, { recursive: true });

  const zip = createReadStream(ipaPath).pipe(unzipper.Parse({ forceStream: true }));

  let executableName: string | null = null;
  let bundleId: string | null = null;
  let bundleVersion: string | null = null;
  let appDirPrefix: string | null = null;
  const frameworks: string[] = [];
  const entries: Array<{ path: string; buffer: Buffer }> = [];

  for await (const entry of zip) {
    const entryPath: string = entry.path;
    const type: string = entry.type;

    if (!appDirPrefix && entryPath.startsWith("Payload/") && entryPath.includes(".app/")) {
      const parts = entryPath.split("/");
      appDirPrefix = `${parts[0]}/${parts[1]}/`;
    }

    if (type === "File") {

      if (appDirPrefix && entryPath === `${appDirPrefix}Info.plist`) {
        const buf: Buffer = await entry.buffer();
        const plistInfo = parseBinaryPlist(buf);
        executableName = plistInfo.executableName;
        bundleId = plistInfo.bundleId;
        bundleVersion = plistInfo.bundleVersion;
        continue;
      }

      if (appDirPrefix && entryPath.includes("/Frameworks/") && !entryPath.endsWith("/")) {
        const relPath = entryPath.slice(appDirPrefix.length);
        const fwMatch = relPath.match(/^Frameworks\/([^/]+)\.framework\/\1$/);
        if (fwMatch) {
          frameworks.push(fwMatch[1]);
          const buf: Buffer = await entry.buffer();
          entries.push({ path: relPath, buffer: buf });
          continue;
        }
      }

      if (appDirPrefix) {
        const relPath = entryPath.slice(appDirPrefix.length);

        if (!relPath.includes("/") || relPath.startsWith("Frameworks/")) {
          const buf: Buffer = await entry.buffer();
          entries.push({ path: relPath, buffer: buf });
        } else {
          entry.autodrain();
        }
      } else {
        entry.autodrain();
      }
    } else {
      entry.autodrain();
    }
  }

  if (!executableName) {

    for (const e of entries) {
      if (!e.path.includes("/") && e.buffer.length > 4) {
        const magic = e.buffer.readUInt32BE(0);
        const leMagic = e.buffer.readUInt32LE(0);
        if (magic === FAT_MAGIC || leMagic === MH_MAGIC_64) {
          executableName = e.path;
          break;
        }
      }
    }
  }

  if (!executableName) {
    throw new Error("Could not determine executable name from IPA.");
  }

  const mainEntry = entries.find((e) => e.path === executableName);
  if (!mainEntry) {
    throw new Error(`Executable "${executableName}" not found in IPA.`);
  }

  const binaryPath = join(outputDir, executableName);
  await writeFile(binaryPath, mainEntry.buffer);

  for (const e of entries) {
    if (e.path.startsWith("Frameworks/")) {
      const fwPath = join(outputDir, e.path);
      await mkdir(join(outputDir, "Frameworks"), { recursive: true });
      await writeFile(fwPath, e.buffer);
    }
  }

  const info = parseMachOInfo(mainEntry.buffer);

  return {
    binaryPath,
    bundleId,
    bundleVersion,
    executableName,
    encrypted: info.encrypted,
    frameworks,
  };
}

export async function extractAppBundle(
  appPath: string
): Promise<IpaExtractionResult> {
  const contentsDir = join(appPath, "Contents");
  const infoPlistPath = join(contentsDir, "Info.plist");
  const macosDir = join(contentsDir, "MacOS");
  const frameworksDir = join(contentsDir, "Frameworks");

  const infoPlistBuffer = await readFile(infoPlistPath);
  const plistInfo = parseBinaryPlist(infoPlistBuffer);

  let executableName = plistInfo.executableName;
  if (!executableName) {
    const candidates = await readdir(macosDir, { withFileTypes: true });
    for (const entry of candidates) {
      if (!entry.isFile()) continue;
      const candidatePath = join(macosDir, entry.name);
      const buf = await readFile(candidatePath);
      if (buf.length < 4) continue;
      const magic = buf.readUInt32BE(0);
      const leMagic = buf.readUInt32LE(0);
      if (magic === FAT_MAGIC || leMagic === MH_MAGIC_64) {
        executableName = entry.name;
        break;
      }
    }
  }
  if (!executableName) {
    throw new Error(`Could not determine executable in ${appPath}/Contents/MacOS.`);
  }

  const binaryPath = join(macosDir, executableName);
  const binaryBuffer = await readFile(binaryPath);
  const info = parseMachOInfo(binaryBuffer);

  const frameworks: string[] = [];
  try {
    const fwEntries = await readdir(frameworksDir, { withFileTypes: true });
    for (const entry of fwEntries) {
      if (!entry.isDirectory()) continue;
      const match = entry.name.match(/^(.+)\.framework$/);
      if (match) frameworks.push(match[1]);
    }
  } catch {

  }

  return {
    binaryPath,
    bundleId: plistInfo.bundleId,
    bundleVersion: plistInfo.bundleVersion,
    executableName,
    encrypted: info.encrypted,
    frameworks,
  };
}

export async function extractBinaryFromPath(
  binaryPath: string,
  outputDir: string
): Promise<{ arm64Path: string; encrypted: boolean }> {
  const buffer = await readFile(binaryPath);
  const info = parseMachOInfo(buffer);

  if (!info.hasArm64) {
    throw new Error("Binary does not contain an arm64 slice.");
  }

  const arm64 = extractArm64Slice(buffer, info);
  const arm64Path = join(outputDir, basename(binaryPath) + ".arm64");
  await mkdir(outputDir, { recursive: true });
  await writeFile(arm64Path, arm64);

  return { arm64Path, encrypted: info.encrypted };
}

function parseBinaryPlist(buffer: Buffer): {
  executableName: string | null;
  bundleId: string | null;
  bundleVersion: string | null;
} {

  const text = buffer.toString("utf8");

  let executableName: string | null = null;
  let bundleId: string | null = null;
  let bundleVersion: string | null = null;

  const execMatch = text.match(
    /<key>CFBundleExecutable<\/key>\s*<string>([^<]+)<\/string>/
  );
  if (execMatch) executableName = execMatch[1];

  const idMatch = text.match(
    /<key>CFBundleIdentifier<\/key>\s*<string>([^<]+)<\/string>/
  );
  if (idMatch) bundleId = idMatch[1];

  const verMatch = text.match(
    /<key>CFBundleShortVersionString<\/key>\s*<string>([^<]+)<\/string>/
  );
  if (verMatch) bundleVersion = verMatch[1];

  if (executableName) {
    return { executableName, bundleId, bundleVersion };
  }

  const cfBundleExec = "CFBundleExecutable";
  const keyIndex = buffer.indexOf(cfBundleExec, 0, "utf8");
  if (keyIndex !== -1) {

    const searchStart = keyIndex + cfBundleExec.length;
    const searchEnd = Math.min(searchStart + 200, buffer.length);
    const chunk = buffer.subarray(searchStart, searchEnd).toString("utf8");

    const nameMatch = chunk.match(/([A-Za-z][A-Za-z0-9._-]{1,50})/);
    if (nameMatch) {
      executableName = nameMatch[1];
    }
  }

  const cfBundleId = "CFBundleIdentifier";
  const idIdx = buffer.indexOf(cfBundleId, 0, "utf8");
  if (idIdx !== -1) {
    const chunk = buffer.subarray(idIdx + cfBundleId.length, idIdx + cfBundleId.length + 200).toString("utf8");
    const match = chunk.match(/([a-z][a-z0-9.-]+\.[a-z][a-z0-9.-]+)/i);
    if (match) bundleId = match[1];
  }

  return { executableName, bundleId, bundleVersion };
}

async function importUnzipper(): Promise<any> {
  const moduleName = "unzipper";
  try {
    return await import(moduleName);
  } catch {
    throw new Error(
      'The "unzipper" package is required for IPA extraction. ' +
        "Install it with: npm install unzipper"
    );
  }
}
