import type { ModuleAnalysis } from "./analysis.js";

export type SnapshotPlatform = "web" | "desktop_windows" | "desktop_macos" | "ios";

export interface SnapshotModuleRecord {
  name: string;
  dependencies: string[];
  exports: string[];
  sourcePath: string;
  sourceHash: string;
  metadataHash: string;
  sourceBytes: number;
}

export interface SnapshotNativeModuleRecord {
  name: string;
  url: string;
  filePath: string;
  analysisPath: string;
  contentHash: string;
  sizeBytes: number;
}

export interface SnapshotManifest {
  snapshotId: string;
  platform: SnapshotPlatform;
  revision: string;
  createdAt: string;
  moduleCount: number;
  globalHash: string;
  modules: SnapshotModuleRecord[];
  nativeModules?: SnapshotNativeModuleRecord[];
}

export interface SnapshotIndex {
  schemaVersion: number;
  snapshotId: string;
  revision: string;
  builtAt: string;
  analyses: ModuleAnalysis[];
}

export interface SnapshotDiff {
  fromSnapshotId: string;
  toSnapshotId: string;
  addedModules: string[];
  removedModules: string[];
  changedModules: Array<{
    module: string;
    sourceChanged: boolean;
    dependenciesChanged: boolean;
    exportsAdded: string[];
    exportsRemoved: string[];
    symbolsAdded: string[];
    symbolsRemoved: string[];
    symbolsChanged: string[];
    excerpts?: Record<string, { before: string | null; after: string | null }>;
  }>;
}
