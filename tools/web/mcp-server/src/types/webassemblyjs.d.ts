declare module "wabt" {
  interface ToTextOptions {
    foldExprs?: boolean;
    inlineExport?: boolean;
  }

  interface ToBinaryResult {
    buffer: Uint8Array;
    log: string;
  }

  interface ToBinaryOptions {
    log?: boolean;
    canonicalize_lebs?: boolean;
    relocatable?: boolean;
    write_debug_names?: boolean;
  }

  interface ReadWasmOptions {
    readDebugNames?: boolean;
    exceptions?: boolean;
    mutable_globals?: boolean;
    sat_float_to_int?: boolean;
    sign_extension?: boolean;
    simd?: boolean;
    threads?: boolean;
    multi_value?: boolean;
    tail_call?: boolean;
    bulk_memory?: boolean;
    reference_types?: boolean;
  }

  interface WabtModule {
    validate(): void;
    resolveNames(): void;
    generateNames(): void;
    applyNames(): void;
    toText(options?: ToTextOptions): string;
    toBinary(options?: ToBinaryOptions): ToBinaryResult;
    destroy(): void;
  }

  interface WabtInterface {
    readWasm(buffer: Uint8Array, options?: ReadWasmOptions): WabtModule;
    parseWat(
      filename: string,
      buffer: string | Uint8Array,
      features?: Record<string, boolean>
    ): WabtModule;
  }

  function wabtInit(): Promise<WabtInterface>;
  export = wabtInit;
}
