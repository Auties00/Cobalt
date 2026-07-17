

import { BinaryReader, BinaryWriter } from "@bufbuild/protobuf/wire";

export const protobufPackage = "fdfe";

export interface HttpCookie {
  name: string;
  value: string;
}

export interface SplitDeliveryData {
  name: string;
  downloadUrl: string;
}

export interface AppDeliveryData {
  downloadUrl: string;
  field4Entries: Uint8Array[];
  splits: SplitDeliveryData[];
}

export interface DeliveryResponse {
  appDeliveryData: AppDeliveryData | undefined;
}

export interface AppDetails {
  versionCode: number;
  versionName: string;
}

export interface DocDetails {
  appDetails: AppDetails | undefined;
}

export interface DocV2 {
  docDetails: DocDetails | undefined;
}

export interface DetailsResponse {
  docV2: DocV2 | undefined;
}

export interface Payload {
  detailsResponse: DetailsResponse | undefined;
  deliveryResponse: DeliveryResponse | undefined;
}

export interface ResponseWrapper {
  payload: Payload | undefined;
}

function createBaseHttpCookie(): HttpCookie {
  return { name: "", value: "" };
}

export const HttpCookie: MessageFns<HttpCookie> = {
  encode(message: HttpCookie, writer: BinaryWriter = new BinaryWriter()): BinaryWriter {
    if (message.name !== "") {
      writer.uint32(10).string(message.name);
    }
    if (message.value !== "") {
      writer.uint32(18).string(message.value);
    }
    return writer;
  },

  decode(input: BinaryReader | Uint8Array, length?: number): HttpCookie {
    const reader = input instanceof BinaryReader ? input : new BinaryReader(input);
    const end = length === undefined ? reader.len : reader.pos + length;
    const message = createBaseHttpCookie();
    while (reader.pos < end) {
      const tag = reader.uint32();
      switch (tag >>> 3) {
        case 1: {
          if (tag !== 10) {
            break;
          }

          message.name = reader.string();
          continue;
        }
        case 2: {
          if (tag !== 18) {
            break;
          }

          message.value = reader.string();
          continue;
        }
      }
      if ((tag & 7) === 4 || tag === 0) {
        break;
      }
      reader.skip(tag & 7);
    }
    return message;
  },
};

function createBaseSplitDeliveryData(): SplitDeliveryData {
  return { name: "", downloadUrl: "" };
}

export const SplitDeliveryData: MessageFns<SplitDeliveryData> = {
  encode(message: SplitDeliveryData, writer: BinaryWriter = new BinaryWriter()): BinaryWriter {
    if (message.name !== "") {
      writer.uint32(10).string(message.name);
    }
    if (message.downloadUrl !== "") {
      writer.uint32(42).string(message.downloadUrl);
    }
    return writer;
  },

  decode(input: BinaryReader | Uint8Array, length?: number): SplitDeliveryData {
    const reader = input instanceof BinaryReader ? input : new BinaryReader(input);
    const end = length === undefined ? reader.len : reader.pos + length;
    const message = createBaseSplitDeliveryData();
    while (reader.pos < end) {
      const tag = reader.uint32();
      switch (tag >>> 3) {
        case 1: {
          if (tag !== 10) {
            break;
          }

          message.name = reader.string();
          continue;
        }
        case 5: {
          if (tag !== 42) {
            break;
          }

          message.downloadUrl = reader.string();
          continue;
        }
      }
      if ((tag & 7) === 4 || tag === 0) {
        break;
      }
      reader.skip(tag & 7);
    }
    return message;
  },
};

function createBaseAppDeliveryData(): AppDeliveryData {
  return { downloadUrl: "", field4Entries: [], splits: [] };
}

export const AppDeliveryData: MessageFns<AppDeliveryData> = {
  encode(message: AppDeliveryData, writer: BinaryWriter = new BinaryWriter()): BinaryWriter {
    if (message.downloadUrl !== "") {
      writer.uint32(26).string(message.downloadUrl);
    }
    for (const v of message.field4Entries) {
      writer.uint32(34).bytes(v!);
    }
    for (const v of message.splits) {
      SplitDeliveryData.encode(v!, writer.uint32(122).fork()).join();
    }
    return writer;
  },

  decode(input: BinaryReader | Uint8Array, length?: number): AppDeliveryData {
    const reader = input instanceof BinaryReader ? input : new BinaryReader(input);
    const end = length === undefined ? reader.len : reader.pos + length;
    const message = createBaseAppDeliveryData();
    while (reader.pos < end) {
      const tag = reader.uint32();
      switch (tag >>> 3) {
        case 3: {
          if (tag !== 26) {
            break;
          }

          message.downloadUrl = reader.string();
          continue;
        }
        case 4: {
          if (tag !== 34) {
            break;
          }

          message.field4Entries.push(reader.bytes());
          continue;
        }
        case 15: {
          if (tag !== 122) {
            break;
          }

          message.splits.push(SplitDeliveryData.decode(reader, reader.uint32()));
          continue;
        }
      }
      if ((tag & 7) === 4 || tag === 0) {
        break;
      }
      reader.skip(tag & 7);
    }
    return message;
  },
};

function createBaseDeliveryResponse(): DeliveryResponse {
  return { appDeliveryData: undefined };
}

export const DeliveryResponse: MessageFns<DeliveryResponse> = {
  encode(message: DeliveryResponse, writer: BinaryWriter = new BinaryWriter()): BinaryWriter {
    if (message.appDeliveryData !== undefined) {
      AppDeliveryData.encode(message.appDeliveryData, writer.uint32(18).fork()).join();
    }
    return writer;
  },

  decode(input: BinaryReader | Uint8Array, length?: number): DeliveryResponse {
    const reader = input instanceof BinaryReader ? input : new BinaryReader(input);
    const end = length === undefined ? reader.len : reader.pos + length;
    const message = createBaseDeliveryResponse();
    while (reader.pos < end) {
      const tag = reader.uint32();
      switch (tag >>> 3) {
        case 2: {
          if (tag !== 18) {
            break;
          }

          message.appDeliveryData = AppDeliveryData.decode(reader, reader.uint32());
          continue;
        }
      }
      if ((tag & 7) === 4 || tag === 0) {
        break;
      }
      reader.skip(tag & 7);
    }
    return message;
  },
};

function createBaseAppDetails(): AppDetails {
  return { versionCode: 0, versionName: "" };
}

export const AppDetails: MessageFns<AppDetails> = {
  encode(message: AppDetails, writer: BinaryWriter = new BinaryWriter()): BinaryWriter {
    if (message.versionCode !== 0) {
      writer.uint32(24).int32(message.versionCode);
    }
    if (message.versionName !== "") {
      writer.uint32(34).string(message.versionName);
    }
    return writer;
  },

  decode(input: BinaryReader | Uint8Array, length?: number): AppDetails {
    const reader = input instanceof BinaryReader ? input : new BinaryReader(input);
    const end = length === undefined ? reader.len : reader.pos + length;
    const message = createBaseAppDetails();
    while (reader.pos < end) {
      const tag = reader.uint32();
      switch (tag >>> 3) {
        case 3: {
          if (tag !== 24) {
            break;
          }

          message.versionCode = reader.int32();
          continue;
        }
        case 4: {
          if (tag !== 34) {
            break;
          }

          message.versionName = reader.string();
          continue;
        }
      }
      if ((tag & 7) === 4 || tag === 0) {
        break;
      }
      reader.skip(tag & 7);
    }
    return message;
  },
};

function createBaseDocDetails(): DocDetails {
  return { appDetails: undefined };
}

export const DocDetails: MessageFns<DocDetails> = {
  encode(message: DocDetails, writer: BinaryWriter = new BinaryWriter()): BinaryWriter {
    if (message.appDetails !== undefined) {
      AppDetails.encode(message.appDetails, writer.uint32(10).fork()).join();
    }
    return writer;
  },

  decode(input: BinaryReader | Uint8Array, length?: number): DocDetails {
    const reader = input instanceof BinaryReader ? input : new BinaryReader(input);
    const end = length === undefined ? reader.len : reader.pos + length;
    const message = createBaseDocDetails();
    while (reader.pos < end) {
      const tag = reader.uint32();
      switch (tag >>> 3) {
        case 1: {
          if (tag !== 10) {
            break;
          }

          message.appDetails = AppDetails.decode(reader, reader.uint32());
          continue;
        }
      }
      if ((tag & 7) === 4 || tag === 0) {
        break;
      }
      reader.skip(tag & 7);
    }
    return message;
  },
};

function createBaseDocV2(): DocV2 {
  return { docDetails: undefined };
}

export const DocV2: MessageFns<DocV2> = {
  encode(message: DocV2, writer: BinaryWriter = new BinaryWriter()): BinaryWriter {
    if (message.docDetails !== undefined) {
      DocDetails.encode(message.docDetails, writer.uint32(106).fork()).join();
    }
    return writer;
  },

  decode(input: BinaryReader | Uint8Array, length?: number): DocV2 {
    const reader = input instanceof BinaryReader ? input : new BinaryReader(input);
    const end = length === undefined ? reader.len : reader.pos + length;
    const message = createBaseDocV2();
    while (reader.pos < end) {
      const tag = reader.uint32();
      switch (tag >>> 3) {
        case 13: {
          if (tag !== 106) {
            break;
          }

          message.docDetails = DocDetails.decode(reader, reader.uint32());
          continue;
        }
      }
      if ((tag & 7) === 4 || tag === 0) {
        break;
      }
      reader.skip(tag & 7);
    }
    return message;
  },
};

function createBaseDetailsResponse(): DetailsResponse {
  return { docV2: undefined };
}

export const DetailsResponse: MessageFns<DetailsResponse> = {
  encode(message: DetailsResponse, writer: BinaryWriter = new BinaryWriter()): BinaryWriter {
    if (message.docV2 !== undefined) {
      DocV2.encode(message.docV2, writer.uint32(34).fork()).join();
    }
    return writer;
  },

  decode(input: BinaryReader | Uint8Array, length?: number): DetailsResponse {
    const reader = input instanceof BinaryReader ? input : new BinaryReader(input);
    const end = length === undefined ? reader.len : reader.pos + length;
    const message = createBaseDetailsResponse();
    while (reader.pos < end) {
      const tag = reader.uint32();
      switch (tag >>> 3) {
        case 4: {
          if (tag !== 34) {
            break;
          }

          message.docV2 = DocV2.decode(reader, reader.uint32());
          continue;
        }
      }
      if ((tag & 7) === 4 || tag === 0) {
        break;
      }
      reader.skip(tag & 7);
    }
    return message;
  },
};

function createBasePayload(): Payload {
  return { detailsResponse: undefined, deliveryResponse: undefined };
}

export const Payload: MessageFns<Payload> = {
  encode(message: Payload, writer: BinaryWriter = new BinaryWriter()): BinaryWriter {
    if (message.detailsResponse !== undefined) {
      DetailsResponse.encode(message.detailsResponse, writer.uint32(18).fork()).join();
    }
    if (message.deliveryResponse !== undefined) {
      DeliveryResponse.encode(message.deliveryResponse, writer.uint32(170).fork()).join();
    }
    return writer;
  },

  decode(input: BinaryReader | Uint8Array, length?: number): Payload {
    const reader = input instanceof BinaryReader ? input : new BinaryReader(input);
    const end = length === undefined ? reader.len : reader.pos + length;
    const message = createBasePayload();
    while (reader.pos < end) {
      const tag = reader.uint32();
      switch (tag >>> 3) {
        case 2: {
          if (tag !== 18) {
            break;
          }

          message.detailsResponse = DetailsResponse.decode(reader, reader.uint32());
          continue;
        }
        case 21: {
          if (tag !== 170) {
            break;
          }

          message.deliveryResponse = DeliveryResponse.decode(reader, reader.uint32());
          continue;
        }
      }
      if ((tag & 7) === 4 || tag === 0) {
        break;
      }
      reader.skip(tag & 7);
    }
    return message;
  },
};

function createBaseResponseWrapper(): ResponseWrapper {
  return { payload: undefined };
}

export const ResponseWrapper: MessageFns<ResponseWrapper> = {
  encode(message: ResponseWrapper, writer: BinaryWriter = new BinaryWriter()): BinaryWriter {
    if (message.payload !== undefined) {
      Payload.encode(message.payload, writer.uint32(10).fork()).join();
    }
    return writer;
  },

  decode(input: BinaryReader | Uint8Array, length?: number): ResponseWrapper {
    const reader = input instanceof BinaryReader ? input : new BinaryReader(input);
    const end = length === undefined ? reader.len : reader.pos + length;
    const message = createBaseResponseWrapper();
    while (reader.pos < end) {
      const tag = reader.uint32();
      switch (tag >>> 3) {
        case 1: {
          if (tag !== 10) {
            break;
          }

          message.payload = Payload.decode(reader, reader.uint32());
          continue;
        }
      }
      if ((tag & 7) === 4 || tag === 0) {
        break;
      }
      reader.skip(tag & 7);
    }
    return message;
  },
};

export interface MessageFns<T> {
  encode(message: T, writer?: BinaryWriter): BinaryWriter;
  decode(input: BinaryReader | Uint8Array, length?: number): T;
}
