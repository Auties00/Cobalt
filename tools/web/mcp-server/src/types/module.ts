export interface ParsedModule {
  name: string;
  dependencies: string[];
  exports: string[];
  body: string;
}

export interface ParsedNativeModule {
  name: string;
  url: string;
  binary: Buffer;
}
