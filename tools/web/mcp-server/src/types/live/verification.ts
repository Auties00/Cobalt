export interface SmsVerification {
  readonly id: string;
  readonly phoneNumber: string;

  waitForCode(): Promise<string>;
  release(): Promise<void>;
}

export interface CreateVerificationOptions {
  variant: "personal" | "business";
  country?: string;
}

export interface SmsVerificationProvider {
  readonly name: string;

  create(options: CreateVerificationOptions): Promise<SmsVerification>;
}
