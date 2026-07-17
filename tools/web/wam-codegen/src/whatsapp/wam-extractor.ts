import { withForceLoadedBundle } from "./runtime.js";
import type {
  WamChannel,
  WamEnumDef,
  WamEventDef,
  WamFieldDef,
  WamFieldType,
} from "../parser/types.js";

interface RawEnum {
  readonly moduleName: string;
  readonly constants: ReadonlyArray<{ name: string; value: number }>;
}

type RawFieldType =
  | { kind: "type"; name: string }
  | { kind: "enum"; module: string | null };

interface RawEvent {
  readonly moduleName: string;
  readonly eventName: string;
  readonly eventId: number;
  readonly fields: ReadonlyArray<{
    name: string;
    id: number;
    type: RawFieldType;
  }>;
  readonly weights: ReadonlyArray<number>;
  readonly channel: string | null;
  readonly privateStatsId: number;
}

interface RawCapture {
  readonly enums: ReadonlyArray<RawEnum>;
  readonly events: ReadonlyArray<RawEvent>;
  readonly unresolvedEnumFields: ReadonlyArray<string>;
}

function interceptInPage(): RawCapture {
  const names: string[] = Array.from(
    new Set((window as any).__waDefinedModules || []),
  ) as string[];
  const req = (window as any).require as (name: string) => any;

  const enumObjectToModule = new Map<object, string>();
  const enums: RawEnum[] = [];

  for (const name of names) {
    if (!/^WAWebWamEnum/.test(name)) continue;
    let mod: any;
    try {
      mod = req(name);
    } catch {
      continue;
    }
    if (!mod || typeof mod !== "object") continue;

    let best: { name: string; value: number }[] | null = null;
    for (const key of Object.keys(mod)) {
      const value = mod[key];
      if (!value || typeof value !== "object") continue;
      enumObjectToModule.set(value, name);
      const constants: { name: string; value: number }[] = [];
      for (const ck of Object.keys(value)) {
        if (typeof value[ck] === "number") {
          constants.push({ name: ck, value: value[ck] });
        }
      }
      if (constants.length > 0 && (!best || constants.length > best.length)) {
        best = constants;
      }
    }
    if (best) enums.push({ moduleName: name, constants: best });
  }

  const events: RawEvent[] = [];
  const unresolvedEnumFields: string[] = [];

  let cu: any;
  try {
    cu = req("WAWebWamCodegenUtils");
  } catch {
    cu = null;
  }

  const typeSentinelToName = new Map<unknown, string>();
  const classToEventName = new Map<unknown, string>();
  if (cu) {
    if (cu.TYPES) {
      for (const key of Object.keys(cu.TYPES)) {
        typeSentinelToName.set(cu.TYPES[key], key);
      }
    }
    if (cu.events && typeof cu.events === "object") {
      for (const en of Object.keys(cu.events)) classToEventName.set(cu.events[en], en);
    }
  }

  const moduleByEventName = new Map<string, string>();
  for (const name of names) {
    if (!/WamEvent$/.test(name)) continue;
    let mod: any;
    try {
      mod = req(name);
    } catch {
      continue;
    }
    if (!mod || typeof mod !== "object") continue;
    for (const key of Object.keys(mod)) {
      const en = classToEventName.get(mod[key]);
      if (en) moduleByEventName.set(en, name);
    }
  }

  const specByEventName = new Map<string, any[]>();
  for (const entry of ((window as any).__waWamEvents || []) as Array<{ name: string; arr: any[] }>) {
    if (entry && entry.name && Array.isArray(entry.arr)) {
      specByEventName.set(entry.name, entry.arr);
    }
  }

  for (const [eventName, arr] of specByEventName) {
    const [eventId, fieldsObj, weights, channel, privateStatsId] = arr;
    const fields: RawEvent["fields"] = [];
    for (const fieldName of Object.keys(fieldsObj || {})) {
      const entry = fieldsObj[fieldName];
      const fieldId = entry[0];
      const rawType = entry[1];
      let type: RawFieldType;
      if (typeSentinelToName.has(rawType)) {
        type = { kind: "type", name: typeSentinelToName.get(rawType)! };
      } else if (rawType && typeof rawType === "object") {
        const module = enumObjectToModule.get(rawType) ?? null;
        if (!module) unresolvedEnumFields.push(`${eventName}.${fieldName}`);
        type = { kind: "enum", module };
      } else {
        type = { kind: "enum", module: null };
        unresolvedEnumFields.push(`${eventName}.${fieldName}`);
      }
      (fields as any).push({ name: fieldName, id: fieldId, type });
    }
    events.push({
      moduleName: moduleByEventName.get(eventName) ?? `WAWeb${eventName}WamEvent`,
      eventName,
      eventId,
      fields,
      weights: Array.isArray(weights) ? weights : [1, 1, 1],
      channel: typeof channel === "string" ? channel : null,
      privateStatsId: typeof privateStatsId === "number" ? privateStatsId : -1,
    });
  }

  return { enums, events, unresolvedEnumFields };
}

function toJavaEnumName(moduleName: string): string {
  const name = moduleName.replace(/^WAWebWamEnum/, "");
  return name.endsWith("Event") ? name + "Type" : name;
}

function builtinFieldType(typeName: string): WamFieldType {
  switch (typeName) {
    case "BOOLEAN":
      return "BOOLEAN";
    case "INTEGER":
      return "INTEGER";
    case "STRING":
      return "STRING";
    case "TIMER":
      return "TIMER";
    case "NUMBER":
      return "FLOAT";
    default:
      throw new Error(`Unknown WAM built-in field type: ${typeName}`);
  }
}

function toChannel(raw: string | null): WamChannel {
  return raw === "realtime" || raw === "private" ? raw : "regular";
}

export interface WamExtraction {
  readonly enums: WamEnumDef[];
  readonly events: WamEventDef[];
}

export async function extractWamDefinitions(): Promise<WamExtraction> {
  const raw = await withForceLoadedBundle(interceptInPage);

  if (raw.unresolvedEnumFields.length > 0) {
    throw new Error(
      `Unresolved enum-typed fields (missing enum module identity): ${raw.unresolvedEnumFields.join(", ")}`,
    );
  }

  const enums: WamEnumDef[] = raw.enums.map((e) => ({
    moduleName: e.moduleName,
    javaName: toJavaEnumName(e.moduleName),
    constants: e.constants.map((c) => ({ name: c.name, value: c.value })),
  }));

  const events: WamEventDef[] = raw.events.map((ev) => {
    const fields: WamFieldDef[] = ev.fields.map((f) => {
      if (f.type.kind === "type") {
        return {
          name: f.name,
          id: f.id,
          wamType: builtinFieldType(f.type.name),
          enumJavaName: null,
        };
      }
      return {
        name: f.name,
        id: f.id,
        wamType: "ENUM",
        enumJavaName: toJavaEnumName(f.type.module!),
      };
    });
    return {
      moduleName: ev.moduleName,
      eventName: ev.eventName,
      javaClassName: `${ev.eventName}Event`,
      eventId: Math.round(ev.eventId),
      fields,
      weights: {
        alpha: ev.weights[0] ?? 1,
        beta: ev.weights[1] ?? 1,
        release: ev.weights[2] ?? 1,
      },
      channel: toChannel(ev.channel),
      privateStatsId: ev.privateStatsId,
    };
  });

  return { enums, events };
}
