# Proto Extractor

Extracts every protobuf message and enum definition from WhatsApp Web and
emits a single `.proto` file.

The implementation mirrors `tools/web/ab-props-codegen`:
- TypeScript with ES modules
- Playwright launches a headed Chromium, loads `web.whatsapp.com`, and
  captures every `.js` resource served during page load
- Acorn parses each chunk; modules that declare an `internalSpec` are kept
- The parser resolves cross-module references, enum bodies, `oneof` groups,
  and nested types, then the generator writes proto2 source

## Usage

```
npm install
npm start
```

By default the output is written to `./whatsapp.proto` with package
`com.github.auties00.whatsapp.model.unsupported`. Override either via flags:

```
npm start -- --output ../../modules/model/src/main/proto/whatsapp.proto --package com.example
```
