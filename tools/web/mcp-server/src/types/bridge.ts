import type { SnapshotPlatform } from "./snapshot.js";

export interface CapturedResponse {
  url: string;
  contentType: string;
  getBody(): Promise<Buffer>;
}

export type ResponseListener = (response: CapturedResponse) => void;

export interface LoadedResources {
  jsUrls: string[];
  wasmUrls: string[];
}

export interface PlatformBridge {
  readonly platform: SnapshotPlatform;

  evaluate<R = unknown>(
    pageFunction: string | ((...args: any[]) => R | Promise<R>),
    ...args: any[]
  ): Promise<R>;

  addInitScript(source: string): Promise<void>;

  url(): string;

  navigate(url: string, options?: { timeout?: number }): Promise<void>;

  reload(options?: { timeout?: number }): Promise<void>;

  getLoadedResourceUrls(): Promise<LoadedResources>;

  onResponse(listener: ResponseListener): void;

  removeResponseListeners(): void;

  fetchText(url: string): Promise<string>;

  fetchBinary(url: string): Promise<Buffer>;

  disconnect(): Promise<void>;
}

export interface ConnectionOptions {
  cdpPort?: number;
  headless?: boolean;
  slowMo?: number;
  locale?: string;
}
