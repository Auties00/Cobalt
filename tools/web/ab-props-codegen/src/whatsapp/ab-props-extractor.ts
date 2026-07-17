import { withForceLoadedBundle } from "./runtime.js";
import type { ABPropDef, ABPropType } from "../parser/types.js";

type RawValue = boolean | number | string;

interface RawProp {
  readonly name: string;
  readonly code: number;
  readonly type: string;
  readonly defaultValue: RawValue;
  readonly debugDefaultValue: RawValue;
}

function interceptInPage(): RawProp[] {
  const req = (window as any).require as (name: string) => any;
  const mod = req("WAWebABPropsConfigs");
  const configs = mod && mod.ABPropConfigs;
  if (!configs || typeof configs !== "object") {
    throw new Error("WAWebABPropsConfigs.ABPropConfigs is unavailable in the live runtime");
  }
  const out: RawProp[] = [];
  for (const name of Object.keys(configs)) {
    const tuple = configs[name];
    if (!Array.isArray(tuple)) continue;
    out.push({
      name,
      code: tuple[0],
      type: tuple[1],
      defaultValue: tuple[2],
      debugDefaultValue: tuple[3],
    });
  }
  return out;
}

function isABPropType(value: string): value is ABPropType {
  return value === "bool" || value === "int" || value === "float" || value === "string";
}

function toDefaultString(value: RawValue): string {
  if (typeof value === "boolean") return value ? "true" : "false";
  return String(value);
}

function toSourceToken(value: RawValue): string {
  if (typeof value === "boolean") return value ? "!0" : "!1";
  if (typeof value === "string") {
    return `"${value.replace(/\\/g, "\\\\").replace(/"/g, '\\"')}"`;
  }
  return String(value);
}

export async function extractABProps(): Promise<ABPropDef[]> {
  const raw = await withForceLoadedBundle(interceptInPage);

  return raw.map((p) => {
    if (!isABPropType(p.type)) {
      throw new Error(`Unknown AB prop type "${p.type}" for "${p.name}"`);
    }
    const sourceDefinition = `${p.name}:[${p.code},"${p.type}",${toSourceToken(p.defaultValue)},${toSourceToken(p.debugDefaultValue)}]`;
    return {
      name: p.name,
      code: Math.floor(p.code),
      type: p.type,
      defaultValue: toDefaultString(p.defaultValue),
      debugDefaultValue: toDefaultString(p.debugDefaultValue),
      sourceDefinition,
    };
  });
}
