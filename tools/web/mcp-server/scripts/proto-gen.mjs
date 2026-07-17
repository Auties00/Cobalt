#!/usr/bin/env node
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import { existsSync } from "node:fs";

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, "..");
const pluginJs = resolve(root, "node_modules", "ts-proto", "protoc-gen-ts_proto");
if (!existsSync(pluginJs)) {
  console.error(`ts-proto plugin not found at ${pluginJs}. Run npm install.`);
  process.exit(1);
}

const pluginBat = resolve(root, "scripts", "proto-plugin.cmd");
const plugin = process.platform === "win32" ? pluginBat : pluginJs;

const args = [
  `--plugin=protoc-gen-ts_proto=${plugin}`,
  "-I",
  "proto",
  "--ts_proto_out=src/generated",
  "--ts_proto_opt=esModuleInterop=true,outputJsonMethods=false,outputPartialMethods=false,outputClientImpl=false,forceLong=number,importSuffix=.js",
  "proto/fdfe.proto",
];

const result = spawnSync("protoc", args, { cwd: root, stdio: "inherit", shell: false });
process.exit(result.status ?? 1);
