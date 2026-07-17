#!/usr/bin/env node
import { copyFileSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, "..");

const assets = [
  ["src/utils/play-store-profile.json", "dist/utils/play-store-profile.json"],
];

for (const [from, to] of assets) {
  const src = resolve(root, from);
  const dst = resolve(root, to);
  mkdirSync(dirname(dst), { recursive: true });
  copyFileSync(src, dst);
  console.log(`copied ${from} -> ${to}`);
}
