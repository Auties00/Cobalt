import { access, readFile, writeFile, mkdir, rename } from "node:fs/promises";
import { dirname, join } from "node:path";
import { randomBytes } from "node:crypto";
import { SNAPSHOTS_DIR } from "../storage/snapshot-utils.js";

export interface ModuleAnnotation {
  label: string;
  notes: string;
  updatedAt: string;
}

export interface AnnotationStore {
  snapshotId: string;
  annotations: Record<string, ModuleAnnotation>;
}

function annotationsPath(snapshotId: string): string {
  return join(SNAPSHOTS_DIR, snapshotId, "annotations.json");
}

async function ensureDir(path: string): Promise<void> {
  await mkdir(dirname(path), { recursive: true });
}

export async function loadAnnotations(snapshotId: string): Promise<AnnotationStore> {
  const path = annotationsPath(snapshotId);
  try {
    await access(path);
    const raw = await readFile(path, "utf8");
    return JSON.parse(raw) as AnnotationStore;
  } catch {
    return { snapshotId, annotations: {} };
  }
}

export async function saveAnnotations(store: AnnotationStore): Promise<void> {
  const path = annotationsPath(store.snapshotId);
  await ensureDir(path);
  const tmpPath = `${path}.${randomBytes(4).toString("hex")}.tmp`;
  await writeFile(tmpPath, JSON.stringify(store, null, 2), "utf8");
  await rename(tmpPath, path);
}

export async function getAnnotation(
  snapshotId: string,
  moduleName: string
): Promise<ModuleAnnotation | null> {
  const store = await loadAnnotations(snapshotId);
  return store.annotations[moduleName] ?? null;
}

export async function setAnnotation(
  snapshotId: string,
  moduleName: string,
  label: string,
  notes: string
): Promise<ModuleAnnotation> {
  const store = await loadAnnotations(snapshotId);
  const annotation: ModuleAnnotation = {
    label,
    notes,
    updatedAt: new Date().toISOString(),
  };
  store.annotations[moduleName] = annotation;
  await saveAnnotations(store);
  return annotation;
}

export async function removeAnnotation(
  snapshotId: string,
  moduleName: string
): Promise<boolean> {
  const store = await loadAnnotations(snapshotId);
  if (!(moduleName in store.annotations)) return false;
  delete store.annotations[moduleName];
  await saveAnnotations(store);
  return true;
}

export async function listAnnotations(
  snapshotId: string,
  filter?: string
): Promise<Array<{ module: string; annotation: ModuleAnnotation }>> {
  const store = await loadAnnotations(snapshotId);
  const entries = Object.entries(store.annotations);
  const normalizedFilter = filter?.toLowerCase();
  return entries
    .filter(([name, ann]) => {
      if (!normalizedFilter) return true;
      return (
        name.toLowerCase().includes(normalizedFilter) ||
        ann.label.toLowerCase().includes(normalizedFilter) ||
        ann.notes.toLowerCase().includes(normalizedFilter)
      );
    })
    .map(([module, annotation]) => ({ module, annotation }))
    .sort((a, b) => a.module.localeCompare(b.module));
}
