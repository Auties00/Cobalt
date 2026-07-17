export type ApkVariant = "personal" | "business";

export type EmulatorRegistrationState =
  | "unregistered"
  | "registering"
  | "registered"
  | "invalidated";

export type EmulatorRunState = "stopped" | "starting" | "running" | "stopping";

export interface EmulatorRecord {
  name: string;
  avdName: string;
  deviceProfile: string;
  systemImage: string;
  apkVariant: ApkVariant;
  runState: EmulatorRunState;
  adbSerial: string | null;
  accountPhone: string | null;
  accountType: "personal" | "business" | "unknown";
  registrationState: EmulatorRegistrationState;
  createdAt: string;
  updatedAt: string;
  lastBootedAt: string | null;
}

export interface CreateEmulatorOptions {
  name: string;
  deviceProfile?: string;
  systemImage?: string;
  apkVariant?: ApkVariant;
}

export interface StartEmulatorOptions {
  name: string;
  headless?: boolean;
  bootTimeoutMs?: number;
}

export interface RegisterWhatsAppOptions {
  name: string;
  phone?: string;
  countryCode?: string;
  force?: boolean;
}

export interface PairingCodeOptions {
  name: string;
  code?: string;
  packageName?: string;
  stepTimeoutMs?: number;
}

export interface SystemImage {
  id: string;
  installed: boolean;
  apiLevel: number | null;
  tag: string | null;
  abi: string | null;
}

export interface RegisterWhatsAppResult {
  success: boolean;
  accountPhone: string | null;
  verificationCode: string | null;
  details: string[];
}
